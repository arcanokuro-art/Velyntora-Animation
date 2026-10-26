package art.velyntora.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;


/** Owns components/layers behavior; the host is the workspace composition facade. */
final class LayersPanel {
  private final MainActivity host;
  LayersPanel(MainActivity host) { this.host = host; }


  private Button delete,duplicate,merge,up,down;
  private int lastTapIndex=-1;
  private long lastTapTime;
  void refreshLayerPanel() {
    if(host.readLayerItems()==null || host.readProjectProgress()!=null)return;
    host.readLayerItems().removeAllViews();for(Bitmap old:host.readThumbnails())old.recycle();host.readThumbnails().clear();
    boolean placing=host.readDrawing().placedImage().active();
    if(placing && host.readDrawing().placedImage().editingIndex<0){LinearLayout pending=host.row();pending.setPadding(host.dp(6),host.dp(4),host.dp(4),host.dp(4));pending.setBackgroundColor(0xFF397753);
      ImageView thumb=new ImageView(host);thumb.setImageBitmap(host.readDrawing().placedImage().source.preview);thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);pending.addView(thumb,new LinearLayout.LayoutParams(host.dp(40),host.dp(40)));pending.addView(host.text(host.readDrawing().placedImage().text!=null?"Texto (pendiente)":"Imagen (pendiente)"));host.readLayerItems().addView(pending,new LinearLayout.LayoutParams(-1,host.dp(56)));}
    int active=host.readDrawing().activeLayer(),count=host.readDrawing().layerCount();
    for(int i=count-1;i>=0;i--){final int index=i;LinearLayout row=host.row();row.setPadding(host.dp(6),host.dp(4),host.dp(4),host.dp(4));row.setSelected(i==active);row.setContentDescription(host.readDrawing().layerName(i)+(i==active?", activa":""));row.setBackgroundColor(i==active?0xFF397753:0xFF292929);
      ImageView thumb=new ImageView(host);Bitmap preview=host.readDrawing().layerThumbnail(i);if(preview!=null){host.readThumbnails().add(preview);thumb.setImageBitmap(preview);}thumb.setBackground(new LayerCheckerDrawable());thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);row.addView(thumb,new LinearLayout.LayoutParams(host.dp(40),host.dp(40)));
      TextView label=host.text(host.readDrawing().layerName(i)+(DrawingView.nativeTextStyle(i)!=null?"  (texto)":DrawingView.nativeLayerIsImage(i)?"  ◇":""));label.setPadding(host.dp(8),0,host.dp(4),0);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
      android.widget.CheckBox visible=new android.widget.CheckBox(host);visible.setChecked(host.readDrawing().layerVisible(i));visible.setEnabled(!placing);visible.setContentDescription("Visible: "+host.readDrawing().layerName(i));visible.setOnClickListener(v->{if(host.readProjectProgress()!=null || host.readDrawing().placedImage().active())return;String name=host.readDrawing().layerName(index);float opacity=layerOpacity(index);int mode=host.readDrawing().layerBlendMode(index);boolean checked=visible.isChecked();LayerWorkQueue.run(host,()->DrawingView.nativeLayerProperties(index,name,checked,opacity,mode),true,null);});row.addView(visible,new LinearLayout.LayoutParams(host.dp(40),host.dp(48)));
      row.setEnabled(!placing);row.setOnClickListener(v->{if(host.readProjectProgress()==null && !host.readDrawing().placedImage().active()){long now=android.os.SystemClock.uptimeMillis();boolean twice=lastTapIndex==index&&now-lastTapTime<350;lastTapTime=now;lastTapIndex=index;LayerWorkQueue.select(host,index,twice?()->{lastTapIndex=-1;LayerPropertiesDialog.show(host,index);}:DrawingView.nativeLayerIsImage(index)?()->host.readDrawing().placedImage().edit(index):null);}});row.setOnLongClickListener(v->{if(host.readProjectProgress()==null && !host.readDrawing().placedImage().active()){LayerWorkQueue.select(host,index,()->LayerPropertiesDialog.show(host,index));}return true;});host.readLayerItems().addView(row,new LinearLayout.LayoutParams(-1,host.dp(56)));
    }
    if(delete!=null){delete.setEnabled(count>1);duplicate.setEnabled(count<32);merge.setEnabled(active>0);up.setEnabled(active<count-1);down.setEnabled(active>0);for(Button control:new Button[]{delete,duplicate,merge,up,down})control.setAlpha(control.isEnabled()?1f:.35f);}
  }
  private float layerOpacity(int index){return host.readDrawing().layerOpacity(index);}

  void addLayer() { LayerWorkQueue.run(host,DrawingView::nativeAddLayer,false,null); }
  void renameLayer(int index) { LayerWorkQueue.select(host,index,()->LayerPropertiesDialog.show(host,index)); }
  void chooseLayer() {
    int count=host.readDrawing().layerCount();String[] items=new String[count];
    for(int i=0;i<count;i++)items[i]=host.readDrawing().layerName(i);
    new AlertDialog.Builder(host).setTitle("Capas").setItems(items,(dialog,index)->LayerWorkQueue.select(host,index,null)).show();
  }
  void deleteLayer() { LayerWorkQueue.run(host,DrawingView::nativeDeleteLayer,true,null); }
  void toggleLayer() { LayerWorkQueue.run(host,DrawingView::nativeToggleLayer,true,null); }
  void moveLayer(int direction) { LayerWorkQueue.run(host,()->DrawingView.nativeMoveLayer(direction),true,null); }

  void create(LinearLayout root) {
    LinearLayout panel=new LinearLayout(host);panel.setOrientation(1);panel.setBackgroundColor(0xFF202020);TextView title=host.text("CAPAS");title.setPadding(host.dp(8),host.dp(8),host.dp(8),host.dp(8));panel.addView(title);
    LinearLayout items=new LinearLayout(host);items.setOrientation(1);ScrollView list=new ScrollView(host);list.addView(items,new ScrollView.LayoutParams(-1,-2));panel.addView(list,new LinearLayout.LayoutParams(-1,0,1));host.writeLayerItems(items);
    LayerActionBar commands=new LayerActionBar(host);
    panel.addView(commands.view,new LinearLayout.LayoutParams(-1,-2));
    commands.add("Añadir una capa nueva","new",host::addLayer);
    delete=commands.add("Eliminar capa","delete",host::deleteLayer);
    duplicate=commands.add("Duplicar la capa","duplicate",()->action(0));
    merge=commands.add("Combinar con la capa inferior","merge",()->action(1));
    up=commands.add("Subir capa","up",()->host.moveLayer(1));
    down=commands.add("Bajar capa","down",()->host.moveLayer(-1));

    LayerPanelContainer container=new LayerPanelContainer(host);container.addView(panel,new ScrollView.LayoutParams(-1,-1));host.writeLayerScroll(container);
  }

  private void action(int action){LayerWorkQueue.run(host,()->DrawingView.nativeLayerAction(action),true,null);}
}
