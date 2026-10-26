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
    public void openImage(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(intent,2001);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==2001&&result==RESULT_OK&&data!=null&&data.getData()!=null)app.velyntora.editor.EditorNavigator.open(this,data.getData());}
    public void showRecentProjects(){info("Proyectos recientes","Todavía no hay proyectos guardados en esta versión piloto.");}
    public void showPreferences(){info("Preferencias","Las preferencias estarán disponibles en una próxima etapa del prototipo.");}
    public void showHelp(){info("Velyntora Drawing · Versión piloto","Versión piloto con el motor de Velyntora Core integrado. Crea ilustraciones y edita imágenes.\n\nPowered By Zaskar Startup");}
}
