package net.tecnihardcore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WeatherRulesTest {
    @Test void cosmeticDefaultAndExtendedRadius(){assertEquals(192,WeatherRules.DEFAULT_RADIUS);assertTrue(WeatherRules.settings(0,192,180,120,0));assertTrue(WeatherRules.settings(3,512,600,120,0));assertFalse(WeatherRules.settings(3,513,180,120,0));}
    @Test void widthCannotEscapeProtectedEnvelope(){assertEquals(324,WeatherRules.envelope(192,600));assertEquals(84,WeatherRules.envelope(32,120));assertTrue(WeatherRules.settings(0,192,180,600,0));assertFalse(WeatherRules.settings(0,192,180,601,0));}
    @Test void onlyExplicitTornadoAndQuakeCanDestroy(){for(int t=0;t<5;t++){assertTrue(WeatherRules.settings(t,192,180,120,0));assertEquals(t<=1,WeatherRules.settings(t,192,180,120,4));}assertFalse(WeatherRules.settings(1,192,180,120,-1));assertFalse(WeatherRules.settings(0,192,180,120,5));}
    @Test void budgetsAreDifferentBoundedAndIncrease(){assertEquals(0,WeatherRules.blockBudget(0,0));assertEquals(0,WeatherRules.blockBudget(1,0));for(int i=1;i<=4;i++){assertTrue(WeatherRules.blockBudget(0,i)>WeatherRules.blockBudget(0,i-1));assertTrue(WeatherRules.blockBudget(1,i)>WeatherRules.blockBudget(1,i-1));assertNotEquals(WeatherRules.blockBudget(0,i),WeatherRules.blockBudget(1,i));}assertTrue(WeatherRules.blockBudget(0,4)<=160);assertTrue(WeatherRules.blockBudget(1,4)<=120);}
    @Test void QuakeCutsDeeperAndZeroNeverCuts(){assertEquals(0,WeatherRules.depth(0,0));assertEquals(0,WeatherRules.depth(1,0));assertTrue(WeatherRules.depth(1,4)>WeatherRules.depth(0,4));}
}
