package net.tecnihardcore;

/** Camera timing shared by server, client and tests. All times are milliseconds. */
public final class IntroRules {
    public static final long DURATION_MS = 30_000;
    public static final long ACK_EARLIEST_MS = 28_500;
    public static final long TIMEOUT_MS = 60_000;
    private IntroRules() {}

    public static double ease(double value) {
        double t = Math.max(0, Math.min(1, value));
        return t * t * (3 - 2 * t);
    }

    public static double progress(long elapsed) {
        return Math.max(0, Math.min(1, (double) elapsed / DURATION_MS));
    }

    public static boolean mayCommit(long elapsed) { return elapsed >= ACK_EARLIEST_MS; }
}
