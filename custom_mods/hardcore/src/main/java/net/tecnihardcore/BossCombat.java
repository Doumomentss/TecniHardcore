package net.tecnihardcore;

/** Timing is captured at attack start, so phase changes cannot skip releases. */
public final class BossCombat {
    private BossCombat(){}
    public static int phase(int value){return Math.max(1,Math.min(3,value));}
    public static float health(int phase){return switch(phase(phase)){case 1->400;case 2->500;default->1000;};}
    public static int pause(int phase){return switch(phase(phase)){case 1->16;case 2->12;default->8;};}
    public static int meleeCooldown(int phase){return switch(phase(phase)){case 1->28;case 2->23;default->18;};}
    public static float meleeDamage(int phase){return switch(phase(phase)){case 1->16;case 2->20;default->24;};}
    public static float damage(int type){return switch(type){case 1->18;case 2->16;case 3->14;case 4->20;case 5->18;case 6->8;case 7->24;case 8->24;case 9->18;case 10->12;case 11->26;case 12->22;default->0;};}
    public static int transitionTicks(int phase){return phase==3?120:80;}
    public static int[] pool(int phase){return switch(phase(phase)){case 1->new int[]{1,2,3};case 2->new int[]{4,5,6,7};default->new int[]{8,9,10,11,12};};}
    public static int choose(int phase,int previous,boolean distant){int[] pool=distant?(phase==1?new int[]{3,2}:phase==2?new int[]{5,6}:new int[]{9,12,10}):pool(phase);for(int i=0;i<pool.length;i++)if(pool[i]==previous)return pool[(i+1)%pool.length];return pool[0];}
    public static Attack plan(int phase,int type){int windup=phase==1?22:phase==2?18:14;if(type==5||type==6||type==9||type==10||type==11||type==12)windup=Math.max(20,windup);return new Attack(phase(phase),type,windup);}
    public static long remaining(long now,long used,boolean arena){if(used<=0)return 0;return Math.max(0,used+(arena?60_000:300_000)-now);}
    public static String name(int type){return switch(type){case 1->"ONDA · salta";case 2->"FRACTURA · sal de la línea";case 3->"CRISTALES · esquiva";case 4->"FRACTURA TRIPLE";case 5->"LLUVIA DE CRISTALES";case 6->"PRISIÓN RÚNICA · sal del círculo";case 7->"EMBESTIDA · apártate";case 8->"DOBLE ONDA · salta";case 9->"LANZAS PERSEGUIDORAS";case 10->"CADENAS DEL NÚCLEO";case 11->"CONVERGENCIA · busca un hueco";case 12->"TORMENTA FINAL";default->"CUSTODIO";};}
    public record Attack(int phase,int type,int windup){
        public boolean volley(int age){int since=age-windup;return type==3&&since>=0&&since%7==0&&since/7<2;}
        public int finish(){return windup+switch(type){case 5->28;case 8->32;case 9->8;case 12->44;default->24;};}
    }
}
