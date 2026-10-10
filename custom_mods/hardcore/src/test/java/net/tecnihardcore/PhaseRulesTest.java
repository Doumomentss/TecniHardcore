package net.tecnihardcore;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhaseRulesTest {
    @Test void distanceRingsDoubleAndCap() {
        assertEquals(0,PhaseRules.ring(2999,0));
        assertEquals(1,PhaseRules.ring(3000,0));
        assertEquals(.02,PhaseRules.distanceBonus(1),1e-9);
        assertEquals(.04,PhaseRules.distanceBonus(2),1e-9);
        assertEquals(.08,PhaseRules.distanceBonus(3),1e-9);
        assertEquals(2.56,PhaseRules.distanceBonus(100),1e-9);
        assertTrue(PhaseRules.extraSpawnChance(100)<=.5);
    }
    @Test void onlineDaysAndClock() {
        assertEquals(1,PhaseRules.day(0));
        assertEquals(1,PhaseRules.day(86399));
        assertEquals(2,PhaseRules.day(86400));
        assertEquals(1.10,PhaseRules.multiplier(0,2),1e-9);
        assertEquals("24:00:00",PhaseRules.clock(86400));
    }
}
