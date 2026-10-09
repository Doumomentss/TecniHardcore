package net.tecnihardcore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class SoulStoreTest {
 @TempDir Path temp;
 @Test void migrationKeepsLivesAndUsedRitualAsHistory() throws Exception {
  Path file=temp.resolve("souls.json");Files.writeString(file,"{\"schema\":1,\"players\":{\"existing\":{\"lives\":0,\"revived\":true,\"totemReadyAt\":123456}},\"ritualPayments\":{}}");
  var store=new SoulStore(file);var soul=store.data.players.get("existing");assertEquals(3,store.data.schema);assertEquals(0,soul.lives);assertEquals(1,soul.resurrections);assertEquals(123456,soul.totemReadyAt);assertTrue(Rules.canRevive(soul.lives,soul.revived));
  store.save();assertEquals(1,new SoulStore(file).data.players.get("existing").resurrections);
 }
 @Test void currentCounterSurvivesReloadWithoutIncrement() throws Exception {
  Path file=temp.resolve("souls.json");Files.writeString(file,"{\"schema\":2,\"players\":{\"existing\":{\"lives\":1,\"revived\":true,\"resurrections\":8}}}");
  assertEquals(8,new SoulStore(file).data.players.get("existing").resurrections);
 }
 @Test void invalidSchemaFailsClosed() throws Exception {
  Path file=temp.resolve("souls.json");Files.writeString(file,"{\"schema\":99,\"players\":{}}");assertThrows(IllegalStateException.class,()->new SoulStore(file));
 }
 @Test void oldActiveCooldownRetainsActivationAndReloadDoesNotRebase() throws Exception {
  Path file=temp.resolve("cooldown.json");Files.writeString(file,"{\"schema\":2,\"players\":{\"a\":{\"lives\":2,\"totemReadyAt\":1300000,\"resurrections\":4}}}");var store=new SoulStore(file);assertEquals(1000000,store.data.players.get("a").totemUsedAt);assertEquals(4,store.data.players.get("a").lives);store.save();var reloaded=new SoulStore(file);assertEquals(1000000,reloaded.data.players.get("a").totemUsedAt);assertEquals(4,reloaded.data.players.get("a").lives);assertEquals(5,reloaded.data.maxLives);
 }
 @Test void newPlayersReceiveFiveAndMigrationIsNotRepeated() throws Exception {
  Path file=temp.resolve("five.json");Files.writeString(file,"{\"schema\":3,\"players\":{\"a\":{\"lives\":3},\"b\":{\"lives\":0}}}");var store=new SoulStore(file);assertEquals(5,store.data.players.get("a").lives);assertEquals(0,store.data.players.get("b").lives);store.save();assertEquals(5,new SoulStore(file).data.players.get("a").lives);assertEquals(5,new SoulStore.Soul().lives);
 }
}
