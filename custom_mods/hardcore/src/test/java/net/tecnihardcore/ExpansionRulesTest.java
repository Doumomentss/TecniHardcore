package net.tecnihardcore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExpansionRulesTest {
    @Test void distributionIsCategoricalNotFiveIndependentRolls(){int[] histogram=new int[6];for(int i=0;i<100;i++)histogram[ExpansionRules.mountRoll(i)]++;assertArrayEquals(new int[]{52,20,15,8,4,1},histogram);}
    @Test void eachBoundary(){assertEquals(1,ExpansionRules.mountRoll(19));assertEquals(2,ExpansionRules.mountRoll(20));assertEquals(2,ExpansionRules.mountRoll(34));assertEquals(3,ExpansionRules.mountRoll(35));assertEquals(4,ExpansionRules.mountRoll(43));assertEquals(5,ExpansionRules.mountRoll(47));assertEquals(0,ExpansionRules.mountRoll(48));assertThrows(IllegalArgumentException.class,()->ExpansionRules.mountRoll(100));}
    @Test void seededStatisticalDistribution(){var r=new java.util.Random(25);int[] counts=new int[6];int n=500000;for(int i=0;i<n;i++)counts[ExpansionRules.mountRoll(r.nextInt(100))]++;int[] expected={52,20,15,8,4,1};for(int i=0;i<6;i++)assertEquals(expected[i]/100.0,counts[i]/(double)n,.003);}
    @Test void contributionThreshold(){assertFalse(ExpansionRules.eligible(94.999,1900));assertTrue(ExpansionRules.eligible(95,1900));assertFalse(ExpansionRules.eligible(0,0));}
    @Test void zonesDoNotOverlap(){assertTrue(ExpansionRules.overlaps(191.9,96,96));assertFalse(ExpansionRules.overlaps(192,96,96));assertTrue(ExpansionRules.plaza(0,0,96));assertFalse(ExpansionRules.plaza(128,0,96));}
    @Test void limits(){assertTrue(ExpansionRules.hazardBounds(32,30));assertTrue(ExpansionRules.hazardBounds(512,600));assertFalse(ExpansionRules.hazardBounds(513,600));assertFalse(ExpansionRules.hazardBounds(96,601));}
    @Test void tornadoIsOneHundredTwentyBlocksWide(){assertEquals(60,ExpansionRules.tornadoRadius(1));assertEquals(5,ExpansionRules.tornadoRadius(0));assertTrue(ExpansionRules.tornadoRadius(.5)>ExpansionRules.tornadoRadius(.25));}
    @Test void strongerMountsAreStillModerate(){for(int i=0;i<5;i++){assertTrue(ExpansionRules.SPEED[i]<ExpansionRules.BOOST[i]);assertTrue(ExpansionRules.BOOST[i]<=24);if(i>0)assertTrue(ExpansionRules.HEALTH[i]>ExpansionRules.HEALTH[i-1]);}}
    @Test void wavesHaveBoundedPopulation(){assertEquals(10,ExpansionRules.waveSize(1));assertEquals(14,ExpansionRules.waveSize(2));assertEquals(24,ExpansionRules.waveSize(100));}
}
