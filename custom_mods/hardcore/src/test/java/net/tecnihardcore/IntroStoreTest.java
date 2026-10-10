package net.tecnihardcore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class IntroStoreTest {
    @TempDir Path world;

    @Test void onlyCompletedSequencesPersistAndEachWorldHasItsOwnRecord() throws Exception {
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        IntroStore first=new IntroStore(world);
        String season=first.season();
        assertFalse(first.completed(a));
        // Disconnect before acknowledgement does not write the player UUID.
        assertFalse(new IntroStore(world).completed(a));
        first.complete(a);
        IntroStore restarted=new IntroStore(world);
        assertEquals(season,restarted.season());
        assertTrue(restarted.completed(a));assertFalse(restarted.completed(b));
        restarted.reset(a);
        assertFalse(new IntroStore(world).completed(a));
        Path next=Files.createDirectory(world.resolve("next-season"));
        assertFalse(new IntroStore(next).completed(a));
    }

    @Test void corruptRecordFailsClosedInsteadOfSilentlyReplayingEveryone() throws Exception {
        Files.writeString(world.resolve("tecnihardcore-intro.json"),"{broken");
        assertThrows(IllegalStateException.class,()->new IntroStore(world));
    }
}
