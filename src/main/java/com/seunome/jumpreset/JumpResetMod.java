
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
            Mode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public static boolean enabled = true;
    public static Mode mode = Mode.BLATANT;
    public static boolean velocityEnabled = true;

    public static int blatantKeep = 60;
    public static boolean blatantAntiY = false;
    public static int antikbHold = 2;

    public static boolean velocityRandomize = true;
    public static int velocityMissChance = 15;

    public static int chance = 100;
    public static int accuracy = 100;
    public static boolean instantJump = true;
    public static boolean onlyWhenTargeting = false;
    public static boolean nearbyPlayerCheck = true;
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

        blatantKeep = config.getInt("blatantKeep", cat, 60, 0, 100,
                "Blatant: percentual de KB horizontal que permanece");

        blatantAntiY = config.get(cat, "blatantAntiY", false,
                "Cancela impulso vertical do hit").getBoolean();

        antikbHold = config.getInt("antikbHold", cat, 2, 0, 10,
                "Ticks de antikb apos o hit");

        velocityRandomize = config.get(cat, "velocityRandomize", true,
                "Sorteia fator de reducao por hit").getBoolean();

        velocityMissChance = config.getInt("velocityMissChance", cat, 15, 0, 50,
                "Percentual de hits sem reducao").getInt();

        chance = config.getInt("chance", cat, 100, 0, 100,
                "Chance de tentar o perfect jump");

        accuracy = config.getInt("accuracy", cat, 100, 0, 100,
                "Chance do timing sair perfeito");

        instantJump = config.get(cat, "instantJump", true,
                "Pula no mesmo tick do hit").getBoolean();

        onlyWhenTargeting = config.get(cat, "onlyWhenTargeting", false,
                "So ativa quando o oponente esta no crosshair").getBoolean();

        nearbyPlayerCheck = config.get(cat, "nearbyPlayerCheck", true,
                "Verifica jogador por perto").getBoolean();

        waterCheck = config.get(cat, "waterCheck", true,
                "Nao ativa na agua ou lava").getBoolean();

        delay = config.getInt("delay", cat, 0, 0, 5,
                "Ticks de espera").getInt();

        knockbackThreshold = config.get(cat, "knockbackThreshold", 0.12D,
                "Velocidade horizontal minima para considerar hit real").getDouble();

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static void saveConfig() {
        if (config == null) {
            return;
        }

        String cat = Configuration.CATEGORY_GENERAL;

        config.get(cat, "enabled", true).set(enabled);
        config.get(cat, "mode", Mode.BLATANT.name()).set(mode.name());
        config.get(cat, "velocityEnabled", true).set(velocityEnabled);
        config.get(cat, "blatantKeep", 60).set(blatantKeep);
        config.get(cat, "blatantAntiY", false).set(blatantAntiY);
        config.get(cat, "antikbHold", 2).set(antikbHold);
        config.get(cat, "velocityRandomize", true).set(velocityRandomize);
        config.get(cat, "velocityMissChance", 15).set(velocityMissChance);
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
        toggleKey = new KeyBinding(
                "key.jumpreset.toggle",
                Keyboard.KEY_R,
                "key.categories.jumpreset"
        );

        guiKey = new KeyBinding(
                "key.jumpreset.gui",
                Keyboard.KEY_Z,
                "key.categories.jumpreset"
        );

        ClientRegistry.registerKeyBinding(toggleKey);
        ClientRegistry.registerKeyBinding(guiKey);

        JumpResetHandler handler = new JumpResetHandler();

        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);

        // Carrega as configuracoes dos novos modulos
        CombatModules.loadConfig();

        // Registra o handler de WTap e BlockHit
        CombatModulesHandler combatHandler = new CombatModulesHandler();

        MinecraftForge.EVENT_BUS.register(combatHandler);
        FMLCommonHandler.instance().bus().register(combatHandler);
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

            mc.thePlayer.addChatMessage(
                    new ChatComponentText(
                            EnumChatFormatting.AQUA + "[" + NAME + "] "
                                    + EnumChatFormatting.RESET + state
                    )
            );
        }
    }
}
