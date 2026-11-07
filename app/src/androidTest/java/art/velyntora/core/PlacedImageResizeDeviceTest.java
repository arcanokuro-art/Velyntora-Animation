package art.velyntora.core;

import org.junit.Test;
import static org.junit.Assert.*;

public final class PlacedImageResizeDeviceTest {
  @Test public void cornerFillsCanvasWithoutMovingOppositeCornerAndCanRelease(){
    PlacedImageTransform start=new PlacedImageTransform(100,50,200,100);
    start.cx=50;start.cy=25;
    PlacedImageTransform result=start.copy();
    AlignmentGuides guides=new AlignmentGuides();
    PlacedImageResize.apply(start,result,4,97,48.5f,true,200,100,5,guides);
    assertEquals(200,result.width,0.001f);
    assertEquals(100,result.height,0.001f);
    assertEquals(0,result.cx-result.width/2,0.001f);
    assertEquals(0,result.cy-result.height/2,0.001f);
    assertEquals(200,guides.guideX,0f);
    assertEquals(100,guides.guideY,0f);

    PlacedImageResize.apply(start,result,4,120,60,true,200,100,5,guides);
    assertEquals(220,result.width,0.001f);
    assertEquals(110,result.height,0.001f);
    assertTrue(Float.isNaN(guides.guideX));
    assertTrue(Float.isNaN(guides.guideY));
  }

  @Test public void sideHandleChangesOnlyItsOwnDimensionWhenUnlinked(){
    PlacedImageTransform start=new PlacedImageTransform(100,50,200,100);
    start.cx=50;start.cy=25;
    PlacedImageTransform result=start.copy();
    AlignmentGuides guides=new AlignmentGuides();
    PlacedImageResize.apply(start,result,3,97,60,false,200,100,5,guides);
    assertEquals(200,result.width,0.001f);
    assertEquals(start.height,result.height,0f);
    assertEquals(start.cy,result.cy,0f);
    assertEquals(200,guides.guideX,0f);
    assertTrue(Float.isNaN(guides.guideY));
  }

  @Test public void rotatedSideKeepsOppositeHandleFixed(){
    PlacedImageTransform start=new PlacedImageTransform(100,50,200,200);
    start.cx=100;start.cy=50;start.angle=90;
    PlacedImageTransform result=start.copy();
    AlignmentGuides guides=new AlignmentGuides();
    PlacedImageResize.apply(start,result,3,0,97,false,200,200,5,guides);
    assertEquals(200,result.width,0.001f);
    assertEquals(start.height,result.height,0f);
    assertEquals(0,result.cy-result.width/2,0.001f);
    assertEquals(200,guides.guideY,0f);
    assertTrue(Float.isNaN(guides.guideX));
  }
}
