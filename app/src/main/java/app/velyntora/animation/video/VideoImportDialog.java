package app.velyntora.animation.video;

import android.app.*;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Video preview, trim and extraction settings. Metadata is read outside the UI thread. */
public final class VideoImportDialog {
  public static void show(Activity host, Uri uri, Executor executor, Consumer<VideoImportRequest> accept) {
    AtomicBoolean canceled=new AtomicBoolean();
    ProgressDialog loading=new ProgressDialog(host);
    loading.setMessage("Leyendo video…");loading.setCancelable(true);
    loading.setOnCancelListener(d->canceled.set(true));loading.show();
    executor.execute(()->{
      MediaMetadataRetriever reader=new MediaMetadataRetriever();
      long duration=0;int width=0,height=0,fps=12;String failure=null;
      try {
        reader.setDataSource(host,uri);
        duration=Long.parseLong(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))*1000L;
        width=Integer.parseInt(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        height=Integer.parseInt(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        String rotation=reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if("90".equals(rotation)||"270".equals(rotation)){int swap=width;width=height;height=swap;}
        String rate=reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
        if(rate!=null)fps=Math.max(1,Math.min(60,Math.round(Float.parseFloat(rate))));
        if(duration<=0||width<=0||height<=0)throw new IllegalArgumentException();
      } catch(Exception e){failure="No se pudo leer el video. Revisa que el formato sea compatible con tu dispositivo.";}
      finally {try{reader.release();}catch(Exception ignored){}}
      final long length=duration;final int w=width,h=height,rate=fps;final String error=failure;
      host.runOnUiThread(()->{
        if(host.isDestroyed()||host.isFinishing())return;
        loading.dismiss();if(canceled.get())return;
        if(error!=null){new AlertDialog.Builder(host).setMessage(error).setPositiveButton("Aceptar",null).show();return;}
        configure(host,uri,length,w,h,rate,accept);
      });
    });
  }

  private static EditText number(Activity host, LinearLayout form, String label, String value, boolean decimal) {
    TextView caption=new TextView(host);caption.setText(label);form.addView(caption);
    EditText field=new EditText(host);field.setSingleLine(true);
    field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
        (decimal?android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL:0));
    field.setText(value);form.addView(field);return field;
  }
  private static void configure(Activity host,Uri uri,long duration,int width,int height,int sourceFps,
      Consumer<VideoImportRequest> accept) {
    LinearLayout form=new LinearLayout(host);form.setOrientation(LinearLayout.VERTICAL);
    int padding=Math.round(16*host.getResources().getDisplayMetrics().density);
    form.setPadding(padding,padding,padding,padding);
    VideoView preview=new VideoView(host);preview.setBackgroundColor(Color.BLACK);
    form.addView(preview,new LinearLayout.LayoutParams(-1,padding*10));
    preview.setVideoURI(uri);preview.setOnPreparedListener(player->{player.setLooping(false);preview.seekTo(0);});
    TextView source=new TextView(host);
    source.setText(width+" × "+height+" px · "+String.format(Locale.getDefault(),"%.3f s",duration/1000000d));
    form.addView(source);
    EditText start=number(host,form,"Inicio (segundos)","0",true);
    EditText end=number(host,form,"Final (segundos)",String.format(Locale.US,"%.3f",duration/1000000d),true);
    EditText fps=number(host,form,"FPS de extracción (1–60)",""+sourceFps,false);
    TextView resolution=new TextView(host);resolution.setText("Resolución de salida");form.addView(resolution);
    Spinner size=new Spinner(host);
    String[] sizes={"Máximo 480 px","Máximo 720 px","Máximo 1080 px","Máximo 1920 px","Original (máximo 6000 px)"};
    size.setAdapter(new ArrayAdapter<>(host,android.R.layout.simple_spinner_dropdown_item,sizes));
    size.setSelection(3);form.addView(size);
    TextView estimate=new TextView(host);form.addView(estimate);
    TextView note=new TextView(host);
    note.setText("Se creará una animación nueva. Tu proyecto actual se conservará. El audio no se importa.");
    form.addView(note);
    Button play=new Button(host);play.setText("Previsualizar fragmento");form.addView(play);
    ScrollView scroll=new ScrollView(host);scroll.addView(form);
    AlertDialog dialog=new AlertDialog.Builder(host).setTitle("Video a fotogramas").setView(scroll)
        .setNegativeButton("Cancelar",null).setPositiveButton("Importar",null).create();
    Handler ui=new Handler(Looper.getMainLooper());
    final VideoImportRequest[] selected=new VideoImportRequest[1];
    Runnable update=()->{
      selected[0]=null;
      try{
        long in=Math.round(Double.parseDouble(start.getText().toString().replace(',','.'))*1000000d);
        long out=Math.round(Double.parseDouble(end.getText().toString().replace(',','.'))*1000000d);
        if(out>duration)throw new IllegalArgumentException("El final supera la duración del video");
        int[] limits={480,720,1080,1920,6000};
        float scale=Math.min(1,limits[size.getSelectedItemPosition()]/(float)Math.max(width,height));
        int targetW=Math.max(1,Math.round(width*scale)),targetH=Math.max(1,Math.round(height*scale));
        VideoImportRequest request=new VideoImportRequest(uri,"Video importado",in,out,
            Integer.parseInt(fps.getText().toString()),targetW,targetH);
        selected[0]=request;
        estimate.setText(request.frameCount+" fotogramas · "+targetW+" × "+targetH+" px");
      }catch(Exception e){estimate.setText(e.getMessage()==null?"Revisa los valores":e.getMessage());}
      if(dialog.getButton(AlertDialog.BUTTON_POSITIVE)!=null)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(selected[0]!=null);
    };
    TextWatcher changes=new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int a,int c,int f){}
      public void onTextChanged(CharSequence s,int a,int b,int c){update.run();}
      public void afterTextChanged(Editable e){}
    };
    start.addTextChangedListener(changes);end.addTextChangedListener(changes);fps.addTextChangedListener(changes);
    size.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
      public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){update.run();}
      public void onNothingSelected(android.widget.AdapterView<?> p){}
    });
    Runnable stop=new Runnable(){public void run(){
      VideoImportRequest request=selected[0];
      if(request==null||!preview.isPlaying())return;
      if(preview.getCurrentPosition()*1000L>=request.endUs)preview.pause();
      else ui.postDelayed(this,40);
    }};
    play.setOnClickListener(v->{
      update.run();if(selected[0]==null)return;
      ui.removeCallbacks(stop);preview.seekTo((int)(selected[0].startUs/1000));preview.start();ui.post(stop);
    });
    dialog.setOnDismissListener(d->{ui.removeCallbacksAndMessages(null);preview.stopPlayback();});
    preview.setOnErrorListener((player,what,extra)->{
      estimate.setText("No se puede previsualizar este video en el dispositivo");return true;
    });
    dialog.setOnShowListener(d->{
      update.run();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
        update.run();if(selected[0]!=null){VideoImportRequest request=selected[0];dialog.dismiss();accept.accept(request);}
      });
    });dialog.show();
  }
  private VideoImportDialog(){}
}
