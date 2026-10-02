package net.tecnihardcore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class SoulStoreTest {
 @TempDir Path temp;
 @Test void migrationKeepsLivesAndUsedRitualAsHistory() throws Exception {
  Path file=temp.resolve("souls.json");Files.writeString(file,"{\"schema\":1,\"players\":{\"existing\":{\"lives\":0,\"revived\":true,\"totemReadyAt\":123456}},\"ritualPayments\":{}}");
  var store=new SoulStore(file);var soul=store.data.players.get("existing");assertEquals(2,store.data.schema);assertEquals(0,soul.lives);assertEquals(1,soul.resurrections);assertEquals(123456,soul.totemReadyAt);assertTrue(Rules.canRevive(soul.lives,soul.revived));
  store.save();assertEquals(1,new SoulStore(file).data.players.get("existing").resurrections);
 }
 @Test void currentCounterSurvivesReloadWithoutIncrement() throws Exception {
  Path file=temp.resolve("souls.json");Files.writeString(file,"{\"schema\":2,\"players\":{\"existing\":{\"lives\":1,\"revived\":true,\"resurrections\":8}}}");
  assertEquals(8,new SoulStore(file).data.players.get("existing").resurrections);
 }
 @Test void invalidSchemaFailsClosed() throws Exception {
  Path file=temp.resolve("souls.json");Files.writeString(file,"{\"schema\":99,\"players\":{}}");assertThrows(IllegalStateException.class,()->new SoulStore(file));
 }
}
