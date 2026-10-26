package art.velyntora.core;
import android.app.AlertDialog;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.*;
import java.util.concurrent.*;
/** Per-canvas state. No document changes until successful revision-checked commit. */
final class RemoveAiModule {
 private final DrawingView view;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final RemoveAiBackend backend=new RemoveAiBackend();
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private Bitmap mask,strokeBackup;
 private Canvas maskCanvas;
 private final Paint brush=new Paint(),overlay=new Paint();
 private float previousX,previousY;
 private long maskRevision=-1;
 private int maskLayer=-1,generation;
 private volatile boolean busy;
 private boolean disposed;
 private AlertDialog activeProgress;
 RemoveAiModule(DrawingView view){this.view=view;brush.setColor(0xFFFF4081);brush.setStrokeCap(Paint.Cap.ROUND);brush.setStrokeJoin(Paint.Join.ROUND);overlay.setAlpha(110);}
 private boolean valid(){return mask!=null&&mask.getWidth()==view.readCanvasWidth()&&mask.getHeight()==view.readCanvasHeight()&&maskRevision==DrawingView.nativeRevision()&&maskLayer==DrawingView.nativeActiveLayer();}
 private void ensure(){if(!valid()){clear();mask=Bitmap.createBitmap(view.readCanvasWidth(),view.readCanvasHeight(),Bitmap.Config.ARGB_8888);maskCanvas=new Canvas(mask);maskRevision=DrawingView.nativeRevision();maskLayer=DrawingView.nativeActiveLayer();}}
 void down(float x,float y){if(busy)return;ensure();recycleBackup();strokeBackup=mask.copy(Bitmap.Config.ARGB_8888,true);previousX=x;previousY=y;line(x,y);}
 void move(float x,float y,MotionEvent event){if(busy||!valid())return;for(int i=0;i<event.getHistorySize();i++)line((float)view.readViewport().documentX(event.getHistoricalX(i),event.getHistoricalY(i)),(float)view.readViewport().documentY(event.getHistoricalX(i),event.getHistoricalY(i)));line(x,y);}
 private void line(float x,float y){brush.setStrokeWidth(view.brushRadius()*2);maskCanvas.drawLine(previousX,previousY,x,y,brush);maskCanvas.drawCircle(x,y,view.brushRadius(),brush);previousX=x;previousY=y;view.invalidate();}
 void finishStroke(){recycleBackup();}
 void cancelStroke(){if(busy)return;if(strokeBackup!=null){if(mask!=null)mask.recycle();mask=strokeBackup;strokeBackup=null;maskCanvas=new Canvas(mask);view.invalidate();}}
 void preview(Canvas canvas){if(valid())canvas.drawBitmap(mask,0,0,overlay);}
 private void recycleBackup(){if(strokeBackup!=null){strokeBackup.recycle();strokeBackup=null;}}
 void clear(){generation++;if(mask!=null){mask.recycle();mask=null;maskCanvas=null;}recycleBackup();view.invalidate();}
 void controls(LinearLayout options){
  Button apply=new Button(view.getContext()),cancel=new Button(view.getContext());apply.setText("Eliminar");cancel.setText("Limpiar máscara");apply.setAllCaps(false);cancel.setAllCaps(false);
  // Workspace layout can detach/recreate the module while keeping these buttons.
  // Resolve the current per-view instance at click time, never capture this module.
  final DrawingView canvasView=view;
  apply.setOnClickListener(v->canvasView.removeAi().apply());
  cancel.setOnClickListener(v->canvasView.removeAi().clear());
  options.addView(apply);options.addView(cancel);
  Runnable visibility=()->{boolean active=view.currentTool()==DrawingView.REMOVE_AI;apply.setVisibility(active?0:8);cancel.setVisibility(active?0:8);};
  view.removeAiControlsChanged=visibility;visibility.run();
 }
 void apply(){
  if(busy||disposed)return;
  if(!valid()){clear();toast("Pinta sobre el objeto que quieres eliminar");return;}
  int width=mask.getWidth(),height=mask.getHeight(),count=width*height;
  int[] values=new int[count];mask.getPixels(values,0,width,0,0,width,height);
  byte[] removal=new byte[count];boolean any=false,known=false;
  for(int i=0;i<count;i++){boolean selected=(values[i]>>>24)!=0&&(!view.hasSelection()||view.selectionContains(i%width,i/width));removal[i]=(byte)(selected?1:0);any|=selected;known|=!selected;}
  if(!any||!known){toast("Marca una zona y conserva parte del fondo alrededor");return;}
  int[] source=DrawingView.nativeRemoveSource();if(source==null||source.length!=count){toast("No se puede leer la capa activa");return;}
  long revision=maskRevision;int layer=maskLayer,token=++generation;busy=true;
  AlertDialog progress=new AlertDialog.Builder(view.getContext()).setTitle("Remove AI").setMessage("Reconstruyendo la zona con MI-GAN 512…").setNegativeButton("Cancelar",(d,w)->generation++).create();progress.setCancelable(false);activeProgress=progress;progress.show();
  worker.execute(()->{
   int[] result=null;String error=null;
   try{result=backend.run(view.getContext().getApplicationContext(),source,removal,width,height);}catch(Exception|OutOfMemoryError failure){error=failure.getMessage();}
   int[] completed=result;String message=error;
   main.post(()->{
    busy=false;progress.dismiss();activeProgress=null;if(disposed||token!=generation)return;
    if(completed==null){toast("No se pudo eliminar: "+(message==null?"error de inferencia":message));return;}
    if(!DrawingView.nativeRemoveCommit(completed,removal,width,height,layer,revision)){clear();toast("La capa cambió durante el proceso. Marca la zona nuevamente.");return;}
    clear();view.refresh();toast("Zona reconstruida. Puedes deshacer el cambio.");
   });
  });
 }
 private void toast(String text){Toast.makeText(view.getContext(),text,Toast.LENGTH_LONG).show();}
 void dispose(){disposed=true;if(activeProgress!=null){activeProgress.dismiss();activeProgress=null;}clear();worker.execute(backend::close);worker.shutdown();}
 boolean busy(){return busy;}
}
