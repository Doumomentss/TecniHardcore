package net.tecnihardcore;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IntroRulesTest {
    @Test void durationAndCommitWindowAreBounded() {
        assertEquals(30_000,IntroRules.DURATION_MS);
        assertEquals(0,IntroRules.progress(-1));
        assertEquals(.5,IntroRules.progress(15_000));
        assertEquals(1,IntroRules.progress(35_000));
        assertFalse(IntroRules.mayCommit(28_499));
        assertTrue(IntroRules.mayCommit(28_500));
        assertEquals(0,IntroRules.ease(-1));
        assertEquals(1,IntroRules.ease(2));
    }
}
