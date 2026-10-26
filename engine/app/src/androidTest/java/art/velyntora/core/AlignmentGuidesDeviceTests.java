package art.velyntora.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class AlignmentGuidesDeviceTests {
  @Test public void moveSnapsAxesIndependentlyAndReleases(){
    AlignmentGuides g=new AlignmentGuides();PlacedImageTransform t=new PlacedImageTransform(20,20,800,600);
    t.cx=405;t.cy=340;g.move(t,800,600,8);
    assertEquals(400,t.cx,0);assertEquals(340,t.cy,0);assertTrue(g.vertical);assertFalse(g.horizontal);
    t.cx=420;t.cy=295;g.move(t,800,600,8);
    assertEquals(420,t.cx,0);assertEquals(300,t.cy,0);assertFalse(g.vertical);assertTrue(g.horizontal);
    g.clear();assertFalse(g.horizontal);
  }
  @Test public void rotationSnapsNegativeAndMultipleTurnsWithoutChangingGeometry(){
    AlignmentGuides g=new AlignmentGuides();PlacedImageTransform t=new PlacedImageTransform(20,30,800,600);
    float w=t.width,h=t.height;
    for(float a:new float[]{-91,89,181,359,721}){t.angle=a;g.rotate(t);assertTrue(g.rotation);assertEquals(0,t.angle%90,0);}
    t.angle=42;g.rotate(t);assertFalse(g.rotation);assertEquals(42,t.angle,0);
    assertEquals(w,t.width,0);assertEquals(h,t.height,0);
  }
}
