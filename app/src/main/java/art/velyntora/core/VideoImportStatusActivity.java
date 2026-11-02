package art.velyntora.core;
import android.app.*;import android.content.*;import android.os.*;import android.widget.*;
import app.velyntora.animation.video.background.*;import java.util.*;
/** Progress survives Activity destruction because the service and journal own the work. */
public final class VideoImportStatusActivity extends Activity {
 private VideoImportJob job;private TextView status;private ProgressBar bar;private Button resume,open;
 private final Handler ui=new Handler(Looper.getMainLooper());
 private final Runnable poll=new Runnable(){public void run(){refresh();ui.postDelayed(this,1000);}};
 public void onCreate(Bundle state){
  super.onCreate(state);
  try{job=VideoImportJob.load(this,getIntent().getStringExtra("job"));}
  catch(Exception e){finish();return;}
  LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(32,32,32,32);
  TextView title=new TextView(this);title.setText("Importación de video");title.setTextSize(22);form.addView(title);
  status=new TextView(this);form.addView(status);
  bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(job.request.frameCount);form.addView(bar);
  TextView note=new TextView(this);note.setText("Puedes salir de esta pantalla o apagar la pantalla. Si apagas el dispositivo, el avance queda guardado y se reanuda al volver a abrir Velyntora.");form.addView(note);
  button(form,"Continuar en segundo plano",this::finish);
  resume=button(form,"Reanudar",()->VideoImportManager.resume(this,job,true));
  button(form,"Pausar",()->signal("pause"));
  button(form,"Cancelar importación",()->new AlertDialog.Builder(this).setMessage("¿Cancelar y eliminar los fotogramas parciales?")
    .setPositiveButton("Cancelar importación",(d,w)->signal("cancel")).setNegativeButton("Seguir",null).show());
  open=button(form,"Abrir animación",()->{
   startActivity(new Intent(this,AnimationActivity.class).setAction(AnimationActivity.ACTION).putExtra("session",job.projectDirectory.getAbsolutePath()));
   finish();
  });
  ScrollView scroll=new ScrollView(this);scroll.addView(form);setContentView(scroll);refresh();
 }
 private Button button(LinearLayout parent,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setOnClickListener(v->action.run());parent.addView(b);return b;}
 private void signal(String action){try{VideoImportManager.signal(this,job,action);refresh();}catch(Exception e){status.setText(e.getMessage());}}
 private void refresh(){
  try{
   Properties p=job.status();String state=p.getProperty("state");int done=Integer.parseInt(p.getProperty("completed","0"));
   bar.setProgress(done);String label=state.equals("complete")?"Completada":state.equals("cancelled")?"Cancelada":state.equals("failed")?"Interrumpida: "+p.getProperty("error"):state.equals("paused")||state.equals("interrupted")?"En pausa":"Procesando";
   status.setText(label+"\n"+done+" / "+job.request.frameCount+" fotogramas");
   open.setEnabled(state.equals("complete"));resume.setEnabled(!state.equals("complete")&&!state.equals("cancelled"));
  }catch(Exception e){status.setText("No se pudo leer el progreso");}
 }
 protected void onResume(){super.onResume();if(job!=null)ui.post(poll);}
 protected void onPause(){ui.removeCallbacks(poll);super.onPause();}
 protected void onDestroy(){ui.removeCallbacksAndMessages(null);super.onDestroy();}
}
