package net.tecnihardcore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RulesTest {
 @Test void deathNeverUnderflows(){assertEquals(0,Rules.afterDeath(0));assertEquals(0,Rules.afterDeath(1));assertEquals(2,Rules.afterDeath(3));}
 @Test void migrationClampsOldScores(){assertEquals(0,Rules.clampLives(-8));assertEquals(5,Rules.clampLives(9));assertEquals(2,Rules.clampLives(2));}
 @Test void fiveLivesPreservePreviousLossesAndElimination(){assertEquals(0,Rules.migrateLives(0,3));assertEquals(3,Rules.migrateLives(1,3));assertEquals(4,Rules.migrateLives(2,3));assertEquals(5,Rules.migrateLives(3,3));assertEquals(4,Rules.migrateLives(4,5));}
 @Test void eliminatedSoulCanReviveRepeatedly(){assertTrue(Rules.canRevive(0,false));assertFalse(Rules.canRevive(1,false));assertTrue(Rules.canRevive(0,true));}
 @Test void sharedCooldownExpiresAtDeadline(){assertFalse(Rules.canUse(299999,300000));assertTrue(Rules.canUse(300000,300000));}
}
