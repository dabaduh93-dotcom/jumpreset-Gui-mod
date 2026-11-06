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
        BLATANT("Blatant", 0.00D, 0.00D),
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
    public static Mode mode = Mode.BLATANT;
    public static boolean velocityEnabled = true;

    // Blatant: 0 = antikb total (KB zerado), 100 = sem reducao
    public static int blatantKeep = 0;
    public static boolean blatantAntiY = true;     // mata o "pulinho" do hit
    public static int antikbHold = 4;              // ticks segurando o KB em 0

    public static boolean velocityRandomize = true;
    public static int velocityMissChance = 0;      // % de hits que passam sem reducao

    public static int chance = 100;
    public static int accuracy = 100;
    public static boolean instantJump = true;      // pula no MESMO tick do hit
    public static boolean onlyWhenTargeting = false;
    public static boolean nearbyPlayerCheck = true; // so conta hit de jogador perto
    public static boolean waterCheck = true;
    public static int delay = 0;
    public static double knockbackThreshold = 0.12D;

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

        String modeName = config.get(cat, "mode", Mode.BLATANT.name(),
                "BLATANT / SEMI_BLATANT / LEGIT").getString();
        try {
            mode = Mode.valueOf(modeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            mode = Mode.BLATANT;
        }

        velocityEnabled = config.get(cat, "velocityEnabled", true,
                "Aplica a reducao de knockback").getBoolean();
        blatantKeep = config.getInt("blatantKeep", cat, 0, 0, 100,
                "Blatant: % do KB horizontal que permanece (0 = antikb total)");
        blatantAntiY = config.get(cat, "blatantAntiY", true,
                "Blatant: cancela o impulso vertical do hit").getBoolean();
        antikbHold = config.getInt("antikbHold", cat, 4, 0, 10,
                "Ticks de antikb apos o hit (mata o KB residual)");

        velocityRandomize = config.get(cat, "velocityRandomize", true,
                "Sorteia o fator de reducao por hit").getBoolean();
        velocityMissChance = config.getInt("velocityMissChance", cat, 0, 0, 50,
                "% de hits sem reducao (disfarce)");

        chance = config.getInt("chance", cat, 100, 0, 100,
                "Chance de tentar o perfect jump");
        accuracy = config.getInt("accuracy", cat, 100, 0, 100,
                "Chance do timing sair perfeito");
        instantJump = config.get(cat, "instantJump", true,
                "Pula no mesmo tick do hit (sem atraso)").getBoolean();
        onlyWhenTargeting = config.get(cat, "onlyWhenTargeting", false,
                "So ativa quando o oponente esta no crosshair").getBoolean();
        nearbyPlayerCheck = config.get(cat, "nearbyPlayerCheck", true,
                "So conta como hit se houver jogador por perto").getBoolean();
        waterCheck = config.get(cat, "waterCheck", true,
                "Nao ativa na agua/lava").getBoolean();
        delay = config.getInt("delay", cat, 0, 0, 5,
                "Ticks de espera (ignorado com Instant Jump)");
        knockbackThreshold = config.get(cat, "knockbackThreshold", 0.12D,
                "Velocidade horizontal minima para considerar hit real").getDouble();

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static void saveConfig() {
        String cat = Configuration.CATEGORY_GENERAL;
        config.get(cat, "enabled", true).set(enabled);
        config.get(cat, "mode", Mode.BLATANT.name()).set(mode.name());
        config.get(cat, "velocityEnabled", true).set(velocityEnabled);
        config.get(cat, "blatantKeep", 0).set(blatantKeep);
        config.get(cat, "blatantAntiY", true).set(blatantAntiY);
        config.get(cat, "antikbHold", 4).set(antikbHold);
        config.get(cat, "velocityRandomize", true).set(velocityRandomize);
        config.get(cat, "velocityMissChance", 0).set(velocityMissChance);
        config.get(cat, "chance", 100).set(chance);
        config.get(cat, "accuracy", 100).set(accuracy);
        config.get(cat, "instantJump", true).set(instantJump);
        config.get(cat, "onlyWhenTargeting", false).set(onlyWhenTargeting);
        config.get(cat, "nearbyPlayerCheck", true).set(nearbyPlayerCheck);
        config.get(cat, "waterCheck", true).set(waterCheck);
        config.get(cat, "delay", 0).set(delay);
        config.get(cat, "knockbackThreshold", 0.12D).set(knockbackThreshold);
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
