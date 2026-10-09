package net.tecnihardcore;

public final class ModerationRules {
    public static final long PROTECTION_MS = 30 * 60_000L;
    public static long remaining(long before, long elapsed, boolean authenticatedOnline) {
        return authenticatedOnline ? Math.max(0, before - Math.max(0, elapsed)) : Math.max(0, before);
    }
    public static boolean blocksPvp(long attacker, long victim) { return attacker > 0 || victim > 0; }
    private ModerationRules() {}
}
