
package com.seunome.jumpreset;

public class CombatModules {

    public static boolean wtapEnabled = false;
    public static boolean blockHitEnabled = false;

    // WTap
    public static double activeDistance = 1.0;
    public static int wtapDelay = 50;
    public static int postDelay = 200;

    // BlockHit
    public enum BlockHitMode {
        AUTO,
        PREDICT
    }

    public static BlockHitMode blockHitMode = BlockHitMode.AUTO;

    public static double minCps = 20.1;
    public static double maxCps = 20.1;

    public static boolean jitter = false;
    public static boolean limitItems = false;
}
