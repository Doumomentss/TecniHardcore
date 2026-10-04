package net.tecnihardcore;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SocialRulesTest {
    @Test void pauseOfflineAndExpireAtExactlyTwoHours(){int n=7200;for(int i=0;i<10000;i++)n=SocialRules.remaining(n,false);assertEquals(7200,n);for(int i=0;i<7199;i++)n=SocialRules.remaining(n,true);assertEquals(1,n);assertEquals(0,SocialRules.remaining(n,true));assertEquals(0,SocialRules.remaining(0,true));}
    @Test void protectionIsMutualAndIncludesFormerAndNewMembers(){var t=new SocialStore.Truce();t.expelled="A";t.team="group";t.members.add("B");assertTrue(SocialRules.protectedPair("A","B","","",List.of(t)));assertTrue(SocialRules.protectedPair("B","A","","",List.of(t)));assertTrue(SocialRules.protectedPair("A","C","","group",List.of(t)));assertFalse(SocialRules.protectedPair("A","outsider","","",List.of(t)));t.remaining=0;assertFalse(SocialRules.protectedPair("A","B","","",List.of(t)));}
    @Test void sameTeamProtectedWithoutTruce(){assertTrue(SocialRules.protectedPair("A","B","same","same",List.of()));assertFalse(SocialRules.protectedPair("A","B","","",List.of()));}
    @Test void transferCannotOverdrawOverflowOrMint(){assertEquals(17,SocialRules.transfer(25,-8));assertThrows(IllegalArgumentException.class,()->SocialRules.transfer(7,-8));assertThrows(IllegalArgumentException.class,()->SocialRules.transfer(SocialRules.MAX_BALANCE,1));assertThrows(ArithmeticException.class,()->SocialRules.transfer(Long.MAX_VALUE,1));}
    @Test void teamNamesCannotInjectFormatting(){assertTrue(SocialRules.teamName("Los_Cristales"));assertFalse(SocialRules.teamName("§kteam"));assertFalse(SocialRules.teamName("ab"));assertFalse(SocialRules.teamName("a".repeat(21)));}
}
