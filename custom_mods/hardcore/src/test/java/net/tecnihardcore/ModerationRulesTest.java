package net.tecnihardcore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class ModerationRulesTest {
 @TempDir Path dir;
 @Test void bothDirectionsBlockedUntilThirtyMinutes(){assertEquals(1800000,ModerationRules.PROTECTION_MS);assertTrue(ModerationRules.blocksPvp(1,0));assertTrue(ModerationRules.blocksPvp(0,1));assertFalse(ModerationRules.blocksPvp(0,0));assertEquals(0,ModerationRules.remaining(1,1,true));}
 @Test void offlineAndUnauthenticatedTimeDoesNotConsumeProtection(){assertEquals(1800000,ModerationRules.remaining(1800000,3600000,false));assertEquals(1700000,ModerationRules.remaining(1800000,100000,true));}
 @Test void protectionAndDeathIndexSurviveRestart(){var s=new ModerationStore(dir.resolve("moderation.json"));String id="00000000-0000-0000-0000-000000000001";s.data.protection.put(id,1200000L);s.save();assertEquals(1200000,new ModerationStore(dir.resolve("moderation.json")).data.protection.get(id));}
 @Test void replayPathsCannotEscapePrivateDirectory(){assertTrue(ModerationStore.validPath("00000000-0000-0000-0000-000000000001/2026-10-08_20-10-00.mcpr"));assertFalse(ModerationStore.validPath("../../world/playerdata/x.mcpr"));assertFalse(ModerationStore.validPath("C:/private/x.mcpr"));}
}
