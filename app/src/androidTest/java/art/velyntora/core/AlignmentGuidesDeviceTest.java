package art.velyntora.core;

import org.junit.Test;
import static org.junit.Assert.*;

public final class AlignmentGuidesDeviceTest {
  @Test public void snapsEachAxisAndReleasesBeyondCanvasEdge() {
    AlignmentGuides guides=new AlignmentGuides();
    PlacedImageTransform image=new PlacedImageTransform(40,20,200,100);
    image.cx=23;image.cy=87;
    guides.move(image,200,100,5);
    assertEquals(20,image.cx,0f);
    assertEquals(90,image.cy,0f);
    assertEquals(0,guides.guideX,0f);
    assertEquals(100,guides.guideY,0f);

    image.cx=-31;image.cy=52;
    guides.move(image,200,100,5);
    assertEquals(-31,image.cx,0f);
    assertTrue(Float.isNaN(guides.guideX));
    assertEquals(50,image.cy,0f);
    assertEquals(50,guides.guideY,0f);
  }

  @Test public void usesRotatedOutlineWhenSnappingToRightEdge() {
    AlignmentGuides guides=new AlignmentGuides();
    PlacedImageTransform text=new PlacedImageTransform(40,20,200,100);
    text.angle=90;text.cx=187;text.cy=38;
    guides.move(text,200,100,4);
    assertEquals(190,text.cx,0f);
    assertEquals(200,guides.guideX,0f);
    assertEquals(38,text.cy,0f);
    assertTrue(Float.isNaN(guides.guideY));
    guides.clear();
    assertTrue(Float.isNaN(guides.guideX));
  }
}
