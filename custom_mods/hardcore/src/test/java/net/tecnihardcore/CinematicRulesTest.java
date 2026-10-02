package net.tecnihardcore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CinematicRulesTest {
 @Test void bubbleGrowsAndDisappearsAtTheEnd(){
  assertEquals(0,CinematicRules.domeRadius(0));assertEquals(32,CinematicRules.domeRadius(80));
  assertEquals(0,CinematicRules.darkness(300,33));assertTrue(CinematicRules.darkness(300,5)>.9);
  assertEquals(0,CinematicRules.darkness(680,5));assertEquals(0,CinematicRules.darkness(0,0));
 }
 @Test void cameraBeginsAtTenSecondsAndEndsAfterTheDescent(){
  assertFalse(CinematicRules.cameraTime(199.99));assertTrue(CinematicRules.cameraTime(200));
  assertTrue(CinematicRules.cameraTime(679));assertFalse(CinematicRules.cameraTime(680));
 }
 @Test void descentHasContinuousClampedEndpoints(){
  assertEquals(0,CinematicRules.descent(590));assertEquals(0,CinematicRules.descent(600));
  assertEquals(.5,CinematicRules.descent(640));assertEquals(1,CinematicRules.descent(680));
  assertEquals(1,CinematicRules.descent(1000));
  for(int i=600;i<680;i++)assertTrue(CinematicRules.descent(i+1)>=CinematicRules.descent(i));
 }
}
