package app.velyntora;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import app.velyntora.ui.start.*;
public final class MainActivity extends Activity implements StartActions {
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(0xff151922);getWindow().setNavigationBarColor(0xff151922);setContentView(new StartScreen(this,this).create());app.velyntora.ui.common.ImmersiveMode.apply(getWindow());}
    @Override public void onWindowFocusChanged(boolean hasFocus){super.onWindowFocusChanged(hasFocus);if(hasFocus)app.velyntora.ui.common.ImmersiveMode.apply(getWindow());}
    @Override protected void onResume(){super.onResume();app.velyntora.ui.common.ImmersiveMode.apply(getWindow());}
    private void info(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Aceptar",null).show();}
    public void createIllustration(){app.velyntora.editor.EditorNavigator.create(this);}
    public void openImage(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(intent,2001);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==2001&&result==RESULT_OK&&data!=null&&data.getData()!=null)app.velyntora.editor.EditorNavigator.open(this,data.getData());}
    public void showRecentProjects(){
        java.io.File folder=new java.io.File(getFilesDir(),"animation");
        java.io.File[] sessions=folder.listFiles(f->new java.io.File(f,"project.meta").isFile());
        if(sessions==null||sessions.length==0){info("Proyectos recientes","Todavía no hay proyectos guardados.");return;}
        java.util.Arrays.sort(sessions,(a,b)->Long.compare(new java.io.File(b,"project.meta").lastModified(),new java.io.File(a,"project.meta").lastModified()));
        String[] labels=new String[sessions.length];for(int i=0;i<labels.length;i++)labels[i]="Animación · "+new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",java.util.Locale.getDefault()).format(new java.util.Date(new java.io.File(sessions[i],"project.meta").lastModified()));
        new AlertDialog.Builder(this).setTitle("Proyectos recientes").setItems(labels,(d,i)->startActivity(new Intent(this,art.velyntora.core.AnimationActivity.class).setAction("app.velyntora.animation.RESUME").putExtra("session",sessions[i].getAbsolutePath()))).setNegativeButton("Cerrar",null).show();
    }
    public void showPreferences(){
        android.widget.SeekBar slider=new android.widget.SeekBar(this);slider.setMax(59);slider.setProgress(getSharedPreferences("animation",0).getInt("fps",12)-1);
        android.widget.TextView label=new android.widget.TextView(this);label.setText((slider.getProgress()+1)+" FPS");
        android.widget.LinearLayout box=new android.widget.LinearLayout(this);box.setOrientation(1);box.addView(label);box.addView(slider);
        slider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int v,boolean u){label.setText((v+1)+" FPS");}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});
        new AlertDialog.Builder(this).setTitle("FPS para nuevas animaciones").setView(box).setPositiveButton("Guardar",(d,w)->getSharedPreferences("animation",0).edit().putInt("fps",slider.getProgress()+1).apply()).setNegativeButton("Cancelar",null).show();
    }
    public void showHelp(){info("Velyntora Animation · Versión piloto","Versión piloto con motor Velyntora Core, fotogramas, piel de cebolla y reproducción.\n\nPowered By Zaskar Startup");}
}
