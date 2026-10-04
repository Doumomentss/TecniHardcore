package net.tecnihardcore;
import java.util.*;

/** Pure policies shared by persistence validation and command handling. */
public final class SocialRules {
    public static final long MAX_BALANCE=1_000_000_000L;
    public static final int TRUCE_SECONDS=7200,TEAM_LIMIT=12;
    public static boolean teamName(String s){return s!=null&&s.matches("[A-Za-z0-9_]{3,20}");}
    public static int remaining(int seconds,boolean connected){return connected?Math.max(0,seconds-1):seconds;}
    public static boolean protectedPair(String a,String b,String currentTeamA,String currentTeamB,Collection<SocialStore.Truce> truces){
        if(!currentTeamA.isEmpty()&&currentTeamA.equals(currentTeamB))return true;
        for(var t:truces)if(t.remaining>0&&((t.expelled.equals(a)&&(t.members.contains(b)||t.team.equals(currentTeamB)))||(t.expelled.equals(b)&&(t.members.contains(a)||t.team.equals(currentTeamA)))))return true;
        return false;
    }
    public static long transfer(long balance,long delta){long next=Math.addExact(balance,delta);if(next<0||next>MAX_BALANCE)throw new IllegalArgumentException("Saldo insuficiente o fuera de límites");return next;}
    private SocialRules(){}
}
