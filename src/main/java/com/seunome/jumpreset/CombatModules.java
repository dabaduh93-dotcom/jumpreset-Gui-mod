
package com.seunome.jumpreset;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class CombatModules {

    public static boolean wtapEnabled = false;
    public static boolean blockHitEnabled = false;

    // WTap
    public static double activeDistance = 1.0D;
    public static int wtapDelay = 50;
    public static int postDelay = 200;

    // BlockHit
    public enum BlockHitMode {
        AUTO,
        PREDICT
    }

    public static BlockHitMode blockHitMode = BlockHitMode.AUTO;

    public static double minCps = 20.1D;
    public static double maxCps = 20.1D;

    public static boolean jitter = false;
    public static boolean limitItems = false;

    private static Configuration config;

    public static void loadConfig() {
        File file = new File(
                Minecraft.getMinecraft().mcDataDir,
                "config/jumpreset-combat.cfg"
        );

        config = new Configuration(file);

        wtapEnabled = config.getBoolean(
                "wtapEnabled", "WTap", false, "Ativa o WTap"
        );

        activeDistance = config.getFloat(
                "activeDistance", "WTap", 1.0F, 0.0F, 6.0F,
                "Distancia adicional para ativar"
        );

        wtapDelay = config.getInt(
                "wtapDelay", "WTap", 50, 0, 1000,
                "Atraso em milissegundos"
        );

        postDelay = config.getInt(
                "postDelay", "WTap", 200, 0, 1000,
                "Duracao em milissegundos"
        );

        blockHitEnabled = config.getBoolean(
                "blockHitEnabled", "BlockHit", false, "Ativa o BlockHit"
        );

        String mode = config.getString(
                "mode", "BlockHit", "AUTO",
                "Modo do BlockHit", new String[]{"AUTO", "PREDICT"}
        );

        try {
            blockHitMode = BlockHitMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            blockHitMode = BlockHitMode.AUTO;
        }

        minCps = config.getFloat(
                "minCps", "BlockHit", 20.1F, 1.0F, 30.0F,
                "Valor minimo de CPS (reservado)"
        );

        maxCps = config.getFloat(
                "maxCps", "BlockHit", 20.1F, 1.0F, 30.0F,
                "Valor maximo de CPS (reservado)"
        );

        jitter = config.getBoolean(
                "jitter", "BlockHit", false, "Jitter"
        );

        limitItems = config.getBoolean(
                "limitItems", "BlockHit", false, "Limitar itens"
        );

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static void saveConfig() {
        if (config == null) {
            loadConfig();
        }

        config.get("WTap", "wtapEnabled", false).set(wtapEnabled);
        config.get("WTap", "activeDistance", 1.0F).set((float) activeDistance);
        config.get("WTap", "wtapDelay", 50).set(wtapDelay);
        config.get("WTap", "postDelay", 200).set(postDelay);

        config.get("BlockHit", "blockHitEnabled", false).set(blockHitEnabled);
        config.get("BlockHit", "mode", "AUTO").set(blockHitMode.name());
        config.get("BlockHit", "minCps", 20.1F).set((float) minCps);
        config.get("BlockHit", "maxCps", 20.1F).set((float) maxCps);
        config.get("BlockHit", "jitter", false).set(jitter);
        config.get("BlockHit", "limitItems", false).set(limitItems);

        config.save();
    }
}
