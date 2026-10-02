package net.tecnihardcore;

/** Shared timing and geometry, independent of render state and Minecraft classes. */
public final class CinematicRules {
    public static final double DOME_RADIUS=32, CORE_SCALE=3.5, CREST_HEIGHT=11.7;
    public static final int CAMERA_START=200, COMMIT=600, END=680;
    private CinematicRules(){}
    public static double ease(double value){double t=Math.max(0,Math.min(1,value));return t*t*(3-2*t);}
    public static double domeRadius(double age){return DOME_RADIUS*ease(age/80);}
    public static double fade(double age){return Math.min(ease(age/80),1-ease((age-COMMIT)/80));}
    public static double descent(double age){return ease((age-COMMIT)/(END-COMMIT));}
    public static boolean cameraTime(double age){return age>=CAMERA_START&&age<END;}
    public static double darkness(double age,double distance){return .96*fade(age)*ease((domeRadius(age)-distance)/3);}
}
