package net.tecnihardcore;

/** Explicit operator controls; zero is always cosmetic, even for giant storms. */
public final class WeatherRules {
    public static final int DEFAULT_RADIUS=192, MAX_RADIUS=512;
    public static int envelope(int radius,int width){return Math.max(radius,(int)Math.ceil(width*.5+24));}
    public static boolean settings(int type,int radius,int seconds,int width,int destruction){return type>=0&&type<5&&radius>=32&&radius<=MAX_RADIUS&&seconds>=30&&seconds<=600&&width>=40&&width<=600&&destruction>=0&&destruction<=4&&((type==0||type==1)||destruction==0)&&envelope(radius,type==0?width:0)<=MAX_RADIUS;}
    public static int blockBudget(int type,int level){if(level<0||level>4)throw new IllegalArgumentException("destruction");return (type==0?new int[]{0,12,40,90,160}:new int[]{0,6,24,60,120})[level];}
    public static int depth(int type,int level){return level==0?0:type==0?Math.min(3,level):new int[]{0,1,2,4,7}[level];}
    private WeatherRules(){}
}
