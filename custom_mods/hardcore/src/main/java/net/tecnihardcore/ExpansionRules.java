package net.tecnihardcore;

/** Pure rules shared by validation and server-authoritative simulation. */
public final class ExpansionRules {
    public static final String[] MOUNTS={"Mantarraya de cristal","Ave de cuarzo","Grifo de jade","Wyvern de amatista","Dragón del núcleo"};
    public static final int[] CHANCES={20,15,8,4,1};
    public static final int[] HEALTH={30,40,55,70,90};
    public static final int[] SPEED={8,10,12,14,16};
    public static final int[] BOOST={12,15,18,21,24};
    public static int mountRoll(int roll){if(roll<0||roll>=100)throw new IllegalArgumentException("roll");int sum=0;for(int i=0;i<5;i++){sum+=CHANCES[i];if(roll<sum)return i+1;}return 0;}
    public static int tier(int n){return Math.max(1,Math.min(5,n));}
    public static boolean eligible(double damage,double total){return total>0&&damage>=total*.05;}
    public static boolean hazardBounds(int radius,int seconds){return radius>=32&&radius<=WeatherRules.MAX_RADIUS&&seconds>=30&&seconds<=600;}
    public static boolean overlaps(double distance,int a,int b){return distance<a+b;}
    public static boolean plaza(double x,double z,int radius){return Math.hypot(x,z)<radius+32;}
    public static int waveSize(int players){return Math.min(24,6+4*players);}
    public static double tornadoRadius(double fraction){return 5+55*Math.pow(Math.max(0,Math.min(1,fraction)),.68);}
    private ExpansionRules(){}
}
