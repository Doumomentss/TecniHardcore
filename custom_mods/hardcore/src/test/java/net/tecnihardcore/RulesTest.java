package net.tecnihardcore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RulesTest {
 @Test void deathNeverUnderflows(){assertEquals(0,Rules.afterDeath(0));assertEquals(0,Rules.afterDeath(1));assertEquals(2,Rules.afterDeath(3));}
 @Test void migrationClampsOldScores(){assertEquals(0,Rules.clampLives(-8));assertEquals(3,Rules.clampLives(9));assertEquals(2,Rules.clampLives(2));}
 @Test void eliminatedSoulCanReviveRepeatedly(){assertTrue(Rules.canRevive(0,false));assertFalse(Rules.canRevive(1,false));assertTrue(Rules.canRevive(0,true));}
 @Test void sharedCooldownExpiresAtDeadline(){assertFalse(Rules.canUse(299999,300000));assertTrue(Rules.canUse(300000,300000));}
}
