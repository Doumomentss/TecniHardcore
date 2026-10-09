package net.tecnihardcore;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReplayIndexTest {
    private static ModerationStore.Clip clip(int id,String player,String name) {
        var c=new ModerationStore.Clip();c.id=Integer.toString(id);c.player=player;c.name=name;return c;
    }
    @Test void idsRemainStableAcrossOtherPlayersDeathsAndNameChanges() {
        var clips=List.of(clip(1,"a","Alex"),clip(2,"b","Bea"),clip(3,"a","Alex"),clip(4,"a","AlexNuevo"));
        assertEquals(List.of("1","3","4"),ReplayIndex.history(clips,"AlexNuevo").stream().map(c->c.id).toList());
        assertEquals("3",ReplayIndex.history(clips,"Alex").get(1).id);
        assertFalse(ReplayIndex.ambiguous(clips,"Alex"));
    }
    @Test void reusedNamesCannotSelectAnotherIdentitySilently() {
        var clips=List.of(clip(1,"a","Alex"),clip(2,"b","Alex"));
        assertTrue(ReplayIndex.ambiguous(clips,"Alex"));
        assertTrue(ReplayIndex.history(clips,"Alex").isEmpty());
    }
}
