package art.velyntora.core;
public final class WorkspaceLayoutTests {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args){
  for(int width:new int[]{240,320,360,411,600,719,720,840,1280})for(int height:new int[]{240,320,479,480,800,1280})for(float font:new float[]{.85f,1f,1.3f,2f,3f}){
   WorkspaceLayout layout=new WorkspaceLayout(width,height,font);check(layout.canvasWidth>=240&&layout.toolsWidth<=width&&layout.layersWidth<=width);
   if(width<720)check(!layout.inline&&layout.canvasWidth==width);
   if(layout.inline)check(layout.canvasWidth+layout.toolsWidth+layout.layersWidth==width);
   check(layout.compact==(height<480));
   if(font>1&&width>=720)check(layout.layersWidth>=192);
  }
  for(float invalid:new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY})try{new WorkspaceLayout(800,600,invalid);throw new AssertionError();}catch(IllegalArgumentException expected){}
  try{new WorkspaceLayout(0);throw new AssertionError();}catch(IllegalArgumentException expected){}
  System.out.println("Phone/tablet/split-window/font-scale and compact-height layout policy passed");
 }
}
