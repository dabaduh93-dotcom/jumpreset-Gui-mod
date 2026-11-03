package com.seunome.jumpreset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
@Mod(
        modid = JumpResetMod.MODID,
        name = JumpResetMod.NAME,
        version = JumpResetMod.VERSION,
        acceptedMinecraftVersions = "[1.8.9]",
        clientSideOnly = true
)
public class JumpResetMod {

    public static final String MODID = "jumpreset";
    public static final String NAME = "Jump Reset";
    public static final String VERSION = "@VERSION@";

    public enum Mode {
        // velocityFactor = fracao do KB horizontal que PERMANECE
        BLATANT("Blatant", 0.40D, 0.03D),        // mantem 40% = reduz 60%
        SEMI_BLATANT("Semi Blatant", 0.85D, 0.04D),
        LEGIT("Legit", 0.90D, 0.03D);

        public final String label;
        public final double velocityFactor;
        public final double randomizeRange;

        Mode(String label, double velocityFactor, double randomizeRange) {
            this.label = label;
            this.velocityFactor = velocityFactor;
            this.randomizeRange = randomizeRange;
        }

        public Mode next() {
            Mode[] v = values();
            return v[(ordinal() + 1) % v.length];
        }
    }

    // ---- estado ----
    public static boolean enabled = true;
    public static Mode mode = Mode.LEGIT;
    public static boolean velocityEnabled = true;

    public static int blatantKeep = 40;          // % do KB horizontal que permanece no Blatant
    public static boolean velocityRandomize = true;
    public static int velocityMissChance = 12;   // % de hits sem reducao (disfarce)

    public static int chance = 100;              // chance de tentar o perfect jump
    public static int accuracy = 100;            // chance do timing sair perfeito
    public static boolean onlyWhenTargeting = false;
    public static boolean waterCheck = true;
    public static int delay = 0;                 // ticks (ignorado no Blatant)

    public static KeyBinding toggleKey;
    public static KeyBinding guiKey;

    private static Configuration config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        loadConfig();
    }

    public static void loadConfig() {
        String cat = Configuration.CATEGORY_GENERAL;

        enabled = config.get(cat, "enabled", true,
                "Mod ligado ao entrar no mundo").getBoolean();

        String modeName = config.get(cat, "mode", Mode.LEGIT.name(),
                "BLATANT / SEMI_BLATANT / LEGIT").getString();
        try {
            mode = Mode.valueOf(modeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            mode = Mode.LEGIT;
        }

        velocityEnabled = config.get(cat, "velocityEnabled", true,
                "Aplica a reducao de knockback do modo").getBoolean();
        blatantKeep = config.getInt("blatantKeep", cat, 40, 0, 100,
                "Blatant: % do KB horizontal que permanece (40 = reduz 60%)");
        velocityRandomize = config.get(cat, "velocityRandomize", true,
                "Sorteia o fator de reducao a cada hit").getBoolean();
        velocityMissChance = config.getInt("velocityMissChance", cat, 12, 0, 50,
                "% de hits em que o velocity nao aplica (disfarce)");

        chance = config.getInt("chance", cat, 100, 0, 100,
                "Chance de tentar o perfect jump no hit");
        accuracy = config.getInt("accuracy", cat, 100, 0, 100,
                "Chance do timing do pulo sair perfeito");
        onlyWhenTargeting = config.get(cat, "onlyWhenTargeting", false,
                "So ativa quando o oponente esta perto do crosshair").getBoolean();
        waterCheck = config.get(cat, "waterCheck", true,
                "Nao ativa na agua/lava").getBoolean();
        delay = config.getInt("delay", cat, 0, 0, 5,
                "Ticks de espera antes de pular (Blatant ignora)");

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static void saveConfig() {
        String cat = Configuration.CATEGORY_GENERAL;
        config.get(cat, "enabled", true).set(enabled);
        config.get(cat, "mode", Mode.LEGIT.name()).set(mode.name());
        config.get(cat, "velocityEnabled", true).set(velocityEnabled);
        config.get(cat, "blatantKeep", 40).set(blatantKeep);
        config.get(cat, "velocityRandomize", true).set(velocityRandomize);
        config.get(cat, "velocityMissChance", 12).set(velocityMissChance);
        config.get(cat, "chance", 100).set(chance);
        config.get(cat, "accuracy", 100).set(accuracy);
        config.get(cat, "onlyWhenTargeting", false).set(onlyWhenTargeting);
        config.get(cat, "waterCheck", true).set(waterCheck);
        config.get(cat, "delay", 0).set(delay);
        config.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        toggleKey = new KeyBinding("key.jumpreset.toggle", Keyboard.KEY_R, "key.categories.jumpreset");
        guiKey = new KeyBinding("key.jumpreset.gui", Keyboard.KEY_Z, "key.categories.jumpreset");
        ClientRegistry.registerKeyBinding(toggleKey);
        ClientRegistry.registerKeyBinding(guiKey);

        JumpResetHandler handler = new JumpResetHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }

    public static void toggle() {
        setEnabled(!enabled);
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        saveConfig();

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            String state = value
                    ? EnumChatFormatting.GREEN + "ON"
                    : EnumChatFormatting.RED + "OFF";
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.AQUA + "[" + NAME + "] " + EnumChatFormatting.RESET + state));
        }
    }
}
