package net.tecnihardcore;

/** Explicit operator controls; zero is always cosmetic, even for giant storms. */
public final class WeatherRules {
    public static final int DEFAULT_RADIUS=192, MAX_RADIUS=512;
    public static double orbit(int radius){return Math.min(radius*.25,24);}
    public static double angle(double age){return Math.max(0,age-200)/400.0;}
    public static double crown(double previous,double floor){return Math.max(floor+200,previous+Math.max(-.5,Math.min(.5,floor+200-previous)));}
    /** Follow the connected floor near the previous tip, rather than a roof far above it. */
    public static int floor(int bottom,int top,int highest,double reference,java.util.function.IntPredicate support){
        int ceiling=Math.max(bottom,Math.min(top,highest));
        int y=Math.max(bottom,Math.min(top-1,(int)Math.floor(reference)));
        if(y>=ceiling)return ceiling;
        if(support.test(y)){while(y+1<ceiling&&support.test(y+1))y++;return y+1;}
        for(;y>=bottom;y--)if(support.test(y))return y+1;
        return bottom;
    }
    public static int envelope(int radius,int width){return Math.max(radius,(int)Math.ceil(width*.5+24));}
    public static boolean settings(int type,int radius,int seconds,int width,int destruction){return type>=0&&type<5&&radius>=32&&radius<=MAX_RADIUS&&seconds>=30&&seconds<=600&&width>=40&&width<=600&&destruction>=0&&destruction<=4&&((type==0||type==1||type==4)||destruction==0)&&envelope(radius,type==0?width:0)<=MAX_RADIUS;}
    public static int blockBudget(int type,int level){if(level<0||level>4)throw new IllegalArgumentException("destruction");return (type==0?new int[]{0,12,40,90,160}:type==4?new int[]{0,48,128,256,480}:new int[]{0,6,24,160,480})[level];}
    public static int depth(int type,int level){return level==0?0:type==0?Math.min(3,level):type==4?new int[]{0,1,2,4,7}[level]:new int[]{0,1,2,8,16}[level];}
    public static int craterRadius(int level){return new int[]{0,2,3,5,7}[level];}
    public static int fractureWidth(int level){return new int[]{0,1,2,3,5}[level];}
    private WeatherRules(){}
}
