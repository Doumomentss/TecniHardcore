package net.tecnihardcore;

/** Pure, bounded season difficulty progression. */
public final class PhaseRules {
    public static final long DAY_SECONDS = 86_400;
    private PhaseRules() {}
    public static int ring(double x,double z) { return Math.max(0,(int)Math.floor(Math.hypot(x,z)/3000.0)); }
    public static double distanceBonus(int ring) { return ring<=0?0:Math.min(2.56,Math.scalb(.02,Math.min(16,ring-1))); }
    public static int day(long afterGraceSeconds) { return 1+(int)Math.min(1000,Math.max(0,afterGraceSeconds)/DAY_SECONDS); }
    public static double dayBonus(int day) { return Math.min(1.0,Math.max(0,day-1)*.10); }
    public static double multiplier(int ring,int day) { return (1+distanceBonus(ring))*(1+dayBonus(day)); }
    public static double extraSpawnChance(int ring) { return Math.min(.50,distanceBonus(ring)); }
    public static String clock(long seconds) { return String.format(java.util.Locale.ROOT,"%02d:%02d:%02d",seconds/3600,(seconds/60)%60,seconds%60); }
}
