package net.tecnihardcore;

/** Pure rules, also exercised without starting Minecraft. */
public final class Rules {
    public static final long COOLDOWN_MS = 300_000;
    public static final int MAX_LIVES = 5;
    public static int clampLives(int lives) { return Math.max(0, Math.min(MAX_LIVES, lives)); }
    public static int migrateLives(int lives, int previousMax) {
        int current = Math.max(0, Math.min(previousMax, lives));
        return current == 0 ? 0 : clampLives(current + MAX_LIVES - previousMax);
    }
    public static int afterDeath(int lives) { return Math.max(0, lives - 1); }
    public static boolean canRevive(int lives, boolean used) { return lives == 0; }
    public static boolean canUse(long now, long readyAt) { return now >= readyAt; }
}
