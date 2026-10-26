package art.velyntora.core;

import org.json.*;
import java.io.IOException;

/** Native dab session, reset for every stroke. No fallback to a different named brush. */
final class VelyntoraBrushRuntime {
  private final DrawingView view;
  private byte[] tips,texture;
  private float[] parameters;
  private final float[][] curves=new float[3][];
  private boolean ready,running;
  private float x,y,pressure=1;
  private final Runnable spray=new Runnable(){public void run(){
    if(!running || !view.strokeEditing || view.currentTool()!=DrawingView.BRUSH)return;
    DrawingView.nativeTipBrushStroke(x,y,x,y,view.brushRadius,view.color,view.brushOpacity,pressure);
    view.noteStrokeBounds(x,y,x,y,view.brushRadius*1.415f+1);view.requestStrokeRefresh();
    view.postDelayed(this,40);
  }};
  VelyntoraBrushRuntime(DrawingView view){this.view=view;}
  boolean load(VelyntoraBrushCatalog.Preset preset){
    stop();ready=false;
    try{
      JSONObject json=new JSONObject(new String(VelyntoraBrushCatalog.read(view.getContext(),preset.folder+"/preset.json"),java.nio.charset.StandardCharsets.UTF_8));
      tips=VelyntoraBrushCatalog.read(view.getContext(),preset.folder+"/tip.bin");texture=VelyntoraBrushCatalog.read(view.getContext(),preset.folder+"/texture.bin");
      JSONArray data=json.getJSONArray("params");parameters=new float[data.length()];for(int i=0;i<data.length();i++)parameters[i]=(float)data.getDouble(i);
      JSONArray all=json.getJSONArray("curves");
      for(int k=0;k<3;k++){JSONArray points=all.getJSONArray(k);curves[k]=new float[points.length()*2];for(int i=0;i<points.length();i++)for(int j=0;j<2;j++)curves[k][i*2+j]=(float)points.getJSONArray(i).getDouble(j);}
      ready=true;return true;
    }catch(IOException|JSONException|OutOfMemoryError error){tips=null;texture=null;return false;}
  }
  boolean begin(){
    stop();if(!ready)return false;
    return DrawingView.nativeConfigureTipBrush(tips,texture,parameters,curves[0],curves[1],curves[2]);
  }
  void stroke(float x0,float y0,float x1,float y1,float p){
    if(!ready)return;x=x1;y=y1;pressure=p;
    DrawingView.nativeTipBrushStroke(x0,y0,x1,y1,view.brushRadius,view.color,view.brushOpacity,p);
    if(parameters[11]>0 && !running){running=true;view.postDelayed(spray,40);}
  }
  void stop(){running=false;view.removeCallbacks(spray);}
}
