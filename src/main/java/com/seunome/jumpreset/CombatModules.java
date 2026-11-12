
package com.seunome.jumpreset;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class CombatModules {

    // Ativação dos módulos
    public static boolean wtapEnabled = false;
    public static boolean blockHitEnabled = false;
    public static boolean hitSelectEnabled = false;

    // =========================
    // WTAP
    // =========================

    public static double activeDistance = 1.0D;
    public static int wtapDelay = 50;
    public static int postDelay = 200;

    // =========================
    // BLOCKHIT
    // =========================

    public enum HitSelectMode {
        MANUAL,
        BLATANT
    }

    public static HitSelectMode hitSelectMode = HitSelectMode.MANUAL;
    public static int hitSelectDelayMs = 150;

    public enum BlockHitMode {
        AUTO,
        PREDICT
    }

    public enum Randomization {
        OFF,
        LOW,
        MEDIUM,
        EXTRA;

        public Randomization next() {
            Randomization[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public static BlockHitMode blockHitMode = BlockHitMode.AUTO;

    public static Randomization randomization =
            Randomization.EXTRA;

    public static double minCps = 20.1D;
    public static double maxCps = 20.1D;
    public static int minDelayMs = 0;
    public static int maxDelayMs = 50;
    public static int blockDurationMs = 50;

    public static boolean jitter = false;
    public static boolean limitItems = false;

    private static Configuration config;

    // =========================
    // CARREGAR CONFIGURAÇÕES
    // =========================

    public static void loadConfig() {

        File file = new File(
                Minecraft.getMinecraft().mcDataDir,
                "config/jumpreset-combat.cfg"
        );

        config = new Configuration(file);

        // WTap

        wtapEnabled = config.getBoolean(
                "wtapEnabled",
                "WTap",
                false,
                "Ativa o WTap"
        );

        activeDistance = config.getFloat(
                "activeDistance",
                "WTap",
                1.0F,
                0.0F,
                6.0F,
                "Distancia de ativacao"
        );

        wtapDelay = config.getInt(
                "wtapDelay",
                "WTap",
                50,
                0,
                1000,
                "Atraso em milissegundos"
        );

        postDelay = config.getInt(
                "postDelay",
                "WTap",
                200,
                0,
                1000,
                "Duracao em milissegundos"
        );

        // BlockHit

        blockHitEnabled = config.getBoolean(
                "blockHitEnabled",
                "BlockHit",
                false,
                "Ativa o BlockHit"
        );

        String mode = config.getString(
                "mode",
                "BlockHit",
                "AUTO",
                "Modo do BlockHit",
                new String[]{"AUTO", "PREDICT"}
        );

        try {
            blockHitMode = BlockHitMode.valueOf(
                    mode.toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            blockHitMode = BlockHitMode.AUTO;
        }

        String random = config.getString(
                "randomization",
                "BlockHit",
                "EXTRA",
                "Nivel de randomizacao",
                new String[]{"OFF", "LOW", "MEDIUM", "EXTRA"}
        );

        try {
            randomization = Randomization.valueOf(
                    random.toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            randomization = Randomization.EXTRA;
        }

        minCps = config.getFloat(
                "minCps",
                "BlockHit",
                20.1F,
                1.0F,
                30.0F,
                "CPS minimo"
        );

        maxCps = config.getFloat(
                "maxCps",
                "BlockHit",
                20.1F,
                1.0F,
                30.0F,
                "CPS maximo"
        );

        if (minCps > maxCps) {
            maxCps = minCps;
        }

        minDelayMs = config.getInt(
                "minDelayMs", "BlockHit", 0, 0, 500,
                "Atraso minimo antes de bloquear (ms)"
        );

        maxDelayMs = config.getInt(
                "maxDelayMs", "BlockHit", 50, 0, 500,
                "Atraso maximo antes de bloquear (ms)"
        );

        if (minDelayMs > maxDelayMs) {
            maxDelayMs = minDelayMs;
        }

        blockDurationMs = config.getInt(
                "blockDurationMs", "BlockHit", 50, 50, 500,
                "Duracao do bloqueio (ms)"
        );

        jitter = config.getBoolean(
                "jitter",
                "BlockHit",
                false,
                "Ativa o jitter"
        );

        limitItems = config.getBoolean(
                "limitItems",
                "BlockHit",
                false,
                "Limita os itens utilizados"
        );

        hitSelectEnabled = config.getBoolean(
                "hitSelectEnabled", "HitSelect", false,
                "Ativa o Hit Select"
        );

        String hitMode = config.getString(
                "mode", "HitSelect", "MANUAL",
                "Modo do Hit Select", new String[]{"MANUAL", "BLATANT"}
        );
        try {
            hitSelectMode = HitSelectMode.valueOf(hitMode.toUpperCase());
        } catch (IllegalArgumentException e) {
            hitSelectMode = HitSelectMode.MANUAL;
        }

        hitSelectDelayMs = config.getInt(
                "delayMs", "HitSelect", 150, 50, 500,
                "Delay antes de atacar (ms)"
        );

        if (config.hasChanged()) {
            config.save();
        }
    }

    // =========================
    // SALVAR CONFIGURAÇÕES
    // =========================

    public static void saveConfig() {

        if (config == null) {
            return;
        }

        // WTap

        config.get(
                "WTap",
                "wtapEnabled",
                false
        ).set(wtapEnabled);

        config.get(
                "WTap",
                "activeDistance",
                1.0F
        ).set((float) activeDistance);

        config.get(
                "WTap",
                "wtapDelay",
                50
        ).set(wtapDelay);

        config.get(
                "WTap",
                "postDelay",
                200
        ).set(postDelay);

        // BlockHit

        config.get(
                "BlockHit",
                "blockHitEnabled",
                false
        ).set(blockHitEnabled);

        config.get(
                "BlockHit",
                "mode",
                "AUTO"
        ).set(blockHitMode.name());

        config.get(
                "BlockHit",
                "randomization",
                "EXTRA"
        ).set(randomization.name());

        config.get(
                "BlockHit",
                "minCps",
                20.1F
        ).set((float) minCps);

        config.get(
                "BlockHit",
                "maxCps",
                20.1F
        ).set((float) maxCps);

        config.get(
                "BlockHit",
                "minDelayMs",
                0
        ).set(minDelayMs);

        config.get(
                "BlockHit",
                "maxDelayMs",
                50
        ).set(maxDelayMs);

        config.get(
                "BlockHit",
                "blockDurationMs",
                50
        ).set(blockDurationMs);

        config.get(
                "BlockHit",
                "jitter",
                false
        ).set(jitter);

        config.get(
                "BlockHit",
                "limitItems",
                false
        ).set(limitItems);

        config.get("HitSelect", "hitSelectEnabled", false).set(hitSelectEnabled);
        config.get("HitSelect", "mode", "MANUAL").set(hitSelectMode.name());
        config.get("HitSelect", "delayMs", 150).set(hitSelectDelayMs);

        config.save();
    }
}
