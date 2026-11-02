package app.velyntora.animation.export.menu;
import android.app.*;import android.widget.*;import java.util.function.Consumer;
import app.velyntora.animation.export.*;
/** Sole entry point for animation save and export choices. */
public final class AnimationSaveMenu {
 public static void show(Activity host,int width,int height,Consumer<ExportRequest> choose){
  ExportKind[] kinds=ExportKind.values();String[] labels=new String[kinds.length];
  for(int i=0;i<kinds.length;i++)labels[i]=kinds[i].label;
  new AlertDialog.Builder(host).setTitle("Guardar y exportar").setItems(labels,(d,index)->{
   ExportKind kind=kinds[index];
   if(!kind.animation()){choose.accept(new ExportRequest(kind,width,height,6000000));return;}
   LinearLayout form=new LinearLayout(host);form.setOrientation(LinearLayout.VERTICAL);
   int pad=Math.round(16*host.getResources().getDisplayMetrics().density);form.setPadding(pad,pad,pad,pad);
   TextView caption=new TextView(host);caption.setText("Resolución (lado mayor)");form.addView(caption);
   Spinner size=new Spinner(host);size.setAdapter(new ArrayAdapter<>(host,android.R.layout.simple_spinner_dropdown_item,
     new String[]{"480 px","720 px","1080 px","1920 px","Original (máximo 6000 px)"}));size.setSelection(kind.video()?3:1);form.addView(size);
   TextView detail=new TextView(host);detail.setText(kind.video()?"Video sin audio; fondo blanco. Se respetan los FPS y la duración de cada fotograma.":"Animación en bucle. Se respeta la duración de los fotogramas.");form.addView(detail);
   new AlertDialog.Builder(host).setTitle(kind.label).setView(form).setNegativeButton("Cancelar",null)
    .setPositiveButton("Continuar",(dialog,button)->{
      int[] bounds={480,720,1080,1920,6000};float scale=Math.min(1,bounds[size.getSelectedItemPosition()]/(float)Math.max(width,height));
      int w=Math.max(1,Math.round(width*scale)),h=Math.max(1,Math.round(height*scale));
      if(kind.video()){w=Math.max(2,(w+1)/2*2);h=Math.max(2,(h+1)/2*2);}
      choose.accept(new ExportRequest(kind,w,h,6000000));
    }).show();
  }).setNegativeButton("Cerrar",null).show();
 }
 private AnimationSaveMenu(){}
}
