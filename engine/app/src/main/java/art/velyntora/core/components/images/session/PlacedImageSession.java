package art.velyntora.core;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.view.MotionEvent;
import java.util.ArrayDeque;

/** Modal, disposable image editor. Only confirm can mutate the document. */
final class PlacedImageSession {
  static final int PICK_IMAGE=86;
  final DrawingView drawing;
  PlacedImageSource source;
  volatile PlacedImageSource loading;
  PlacedImageTransform state;
  TextSpec text;
  private final java.util.IdentityHashMap<PlacedImageSource,Boolean> sources=new java.util.IdentityHashMap<>();
  private final class EditState {final PlacedImageTransform transform;final TextSpec spec;final PlacedImageSource original;EditState(PlacedImageTransform t){transform=t.copy();spec=text;original=source;}void restore(){state=transform.copy();text=spec;source=original;}}
  boolean keepRatio=true;
  int editingIndex=-1;
  PlacedImageElementEditor editor;
  Runnable changed;
  private final ArrayDeque<EditState> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
  private PlacedImageTransform dragStart;
  private int handle=-3;
  final AlignmentGuides guides=new AlignmentGuides();
  private float startX,startY,startAngle;
  private boolean committing;
  PlacedImageSession(DrawingView drawing){this.drawing=drawing;}
  boolean active(){return source!=null;}
  MainActivity host(){return (MainActivity)drawing.getContext();}
  void notifyChanged(){if(editor!=null)editor.request();drawing.invalidate();if(changed!=null)changed.run();}
  void pick(){
    if(active() || host().readProjectProgress()!=null)return;
    Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("image/*");
    intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/png","image/jpeg","image/webp"});host().startActivityForResult(intent,PICK_IMAGE);
  }
  void load(Uri uri){
    if(active() || host().readProjectProgress()!=null)return;
    LayerWorkQueue.run(host(),()->{
      try{loading=PlacedImageSource.load(host(),uri);if(host().isDestroyed()){loading.close();loading=null;return false;}return true;}
      catch(Exception|OutOfMemoryError error){return false;}
    },false,"Preparando imagen…","No se pudo insertar la imagen. Usa PNG, JPEG o WebP.",()->{
      PlacedImageSource ready=loading;loading=null;if(ready==null)return;
      if(ready.size.reduced())new AlertDialog.Builder(host()).setTitle("Imagen mayor al límite")
        .setMessage(ready.size.confirmation()).setNegativeButton("Cancelar",(d,w)->ready.close())
        .setPositiveButton("Reducir e insertar",(d,w)->begin(ready)).setOnCancelListener(d->ready.close()).show();
      else begin(ready);
    });
  }
  void begin(PlacedImageSource ready){
    if(host().isDestroyed()){ready.close();return;}
    if(drawing.layerCount()>=32){ready.close();host().message("Se alcanzó el límite de capas");return;}
    text=null;editingIndex=-1;source=ready;sources.put(source,true);state=new PlacedImageTransform(ready.size.displayWidth(),ready.size.displayHeight(),drawing.documentWidth(),drawing.documentHeight());
    undo.clear();redo.clear();drawing.requestFocus();notifyChanged();host().refreshLayerPanel();
  }
  void edit(int index){
    if(active() || host().readProjectProgress()!=null || !DrawingView.nativeLayerIsImage(index))return;
    PlacedImageElementEditor.Prepared[] ready={null};
    LayerWorkQueue.run(host(),()->{
      try{ready[0]=PlacedImageElementEditor.prepare(host(),index);return true;}
      catch(Exception|OutOfMemoryError error){DrawingView.nativeCloseImageEdit();return false;}
    },false,"Abriendo imagen editable…","No se pudo abrir la imagen editable",()->{
      if(host().isDestroyed()){ready[0].source.close();DrawingView.nativeCloseImageEdit();return;}
      source=ready[0].source;sources.put(source,true);text=ready[0].text;state=new PlacedImageTransform(source.size.displayWidth(),source.size.displayHeight(),drawing.documentWidth(),drawing.documentHeight());state.restore(ready[0].transform);state.opacity=ready[0].opacity;
      editingIndex=index;undo.clear();redo.clear();editor=new PlacedImageElementEditor(this,ready[0].preview);editor.vector=ready[0].vector;drawing.deselect();drawing.requestFocus();notifyChanged();host().refreshLayerPanel();
    },()->{if(host().isDestroyed() && ready[0]!=null){ready[0].source.close();DrawingView.nativeCloseImageEdit();}});
  }
  void retainSource(PlacedImageSource value){sources.put(value,true);source=value;}
  void applyText(TextSpec next,int x,int y,boolean editing){TextLayerEditing.apply(this,next,x,y,editing);}
  boolean tapExisting(float x,float y){
    int index=drawing.activeLayer();if(!DrawingView.nativeLayerIsImage(index))return false;
    if(drawing.currentTool()==DrawingView.TEXT && DrawingView.nativeTextStyle(index)==null)return false;
    float[] values=DrawingView.nativeImageTransform(index);if(values==null)return false;
    PlacedImageTransform t=new PlacedImageTransform(1,1,1,1);t.restore(values);
    if(!t.contains(x,y))return false;edit(index);return true;
  }
  boolean drawBase(Canvas canvas){return editor!=null && editor.draw(canvas);}
  void remember(){if(active()){undo.addLast(new EditState(state));redo.clear();}}
  void undo(){guides.clear();if(!active() || undo.isEmpty())return;redo.addLast(new EditState(state));undo.removeLast().restore();notifyChanged();}
  void redo(){guides.clear();if(!active() || redo.isEmpty())return;undo.addLast(new EditState(state));redo.removeLast().restore();notifyChanged();}
  void rotate(){if(!active())return;remember();state.angle=(state.angle+90)%360;notifyChanged();}
  void cropTo(RectF rect){if(!active() || rect.width()<1 || rect.height()<1)return;remember();state.cropTo(rect);notifyChanged();}
  void cancel(){if(committing || host().readProjectProgress()!=null)return;clear();}
  private void clear(){
    guides.clear();
    if(editor!=null){editor.close();editor=null;}editingIndex=-1;
    for(PlacedImageSource s:sources.keySet())s.close();sources.clear();source=null;text=null;state=null;dragStart=null;undo.clear();redo.clear();
    notifyChanged();host().syncToolState();if(!host().isDestroyed())host().refreshLayerPanel();
  }
  void dispose(){if(editor!=null){editor.close();editor=null;}if(!committing){for(PlacedImageSource s:sources.keySet())s.close();sources.clear();source=null;text=null;if(loading!=null)loading.close();loading=null;}}
  boolean key(int code,android.view.KeyEvent event){
    if(!active())return false;
    if(host().readProjectProgress()!=null)return true;
    if(code==android.view.KeyEvent.KEYCODE_ENTER){confirm();return true;}
    if(code==android.view.KeyEvent.KEYCODE_ESCAPE){cancel();return true;}
    if(event.isCtrlPressed()){
      if(code==android.view.KeyEvent.KEYCODE_Z){if(event.isShiftPressed())redo();else undo();}
      else if(code==android.view.KeyEvent.KEYCODE_Y)redo();
      return true;
    }
    return false;
  }
  boolean allow(String label){
    if(!active())return true;
    if(host().readProjectProgress()!=null)return false;
    if(label.equals("Deshacer")){undo();return false;}if(label.equals("Rehacer")){redo();return false;}
    return label.equals("Editar texto") || label.equals("Confirmar imagen") || label.equals("Cancelar imagen") || label.equals("Recortar imagen") || label.equals("Rotar imagen");
  }
  void cancelGesture(){guides.clear();if(dragStart!=null){state=dragStart;dragStart=null;notifyChanged();}}
  boolean touch(MotionEvent event,float x,float y){
    if(!active())return false;
    float radius=24*drawing.getResources().getDisplayMetrics().density/(float)drawing.readViewport().scale;
    int action=event.getActionMasked();
    if(action==MotionEvent.ACTION_DOWN){
      guides.clear();drawing.getParent().requestDisallowInterceptTouchEvent(true);dragStart=state.copy();startX=x;startY=y;handle=-3;
      float[] p=state.handles();float best=radius;
      float radians=(float)Math.toRadians(state.angle),rotationX=state.cx+(float)Math.sin(radians)*(state.height/2+radius*1.6f),rotationY=state.cy-(float)Math.cos(radians)*(state.height/2+radius*1.6f);
      if(Math.hypot(x-rotationX,y-rotationY)<=radius)handle=8;
      else for(int i=0;i<8;i++){float d=(float)Math.hypot(x-p[i*2],y-p[i*2+1]);if(d<best){best=d;handle=i;}}
      Matrix inverse=new Matrix();state.matrix().invert(inverse);float[] local={x,y};inverse.mapPoints(local);
      if(state.contains(x,y) && Math.abs(local[0]-state.crop.centerX())<state.crop.width()*.25f && Math.abs(local[1]-state.crop.centerY())<state.crop.height()*.25f)handle=-1;
      if(handle==-3 && state.contains(x,y))handle=-1;
      startAngle=(float)Math.toDegrees(Math.atan2(y-state.cy,x-state.cx));return true;
    }
    if(action==MotionEvent.ACTION_CANCEL){cancelGesture();return true;}
    if((action==MotionEvent.ACTION_MOVE || action==MotionEvent.ACTION_UP) && dragStart!=null){
      if(handle==-1){state.cx=dragStart.cx+x-startX;state.cy=dragStart.cy+y-startY;guides.move(state,drawing.documentWidth(),drawing.documentHeight(),8*drawing.getResources().getDisplayMetrics().density/(float)drawing.readViewport().scale);}
      else if(handle==8){state.angle=dragStart.angle+(float)Math.toDegrees(Math.atan2(y-dragStart.cy,x-dragStart.cx))-startAngle;guides.rotate(state);}
      else if(handle>=0){
        int[] xs={-1,0,1,1,1,0,-1,-1},ys={-1,-1,-1,0,1,1,1,0};int hx=xs[handle],hy=ys[handle];
        double angle=Math.toRadians(dragStart.angle),c=Math.cos(angle),s=Math.sin(angle);
        float dx=(float)((x-startX)*c+(y-startY)*s),dy=(float)(-(x-startX)*s+(y-startY)*c);
        float w=hx==0?dragStart.width:Math.max(1,dragStart.width+hx*dx),h=hy==0?dragStart.height:Math.max(1,dragStart.height+hy*dy);
        if(keepRatio){float factor=hx==0?h/dragStart.height:hy==0?w/dragStart.width:(Math.abs(w/dragStart.width-1)>=Math.abs(h/dragStart.height-1)?w/dragStart.width:h/dragStart.height);w=dragStart.width*factor;h=dragStart.height*factor;}
        w=Math.min(24000,w);h=Math.min(24000,h);float shiftX=hx*(w-dragStart.width)/2,shiftY=hy*(h-dragStart.height)/2;
        state.cx=dragStart.cx+(float)(shiftX*c-shiftY*s);state.cy=dragStart.cy+(float)(shiftX*s+shiftY*c);state.width=w;state.height=h;
      }
      if(editor!=null)editor.request();drawing.invalidate();
      if(action==MotionEvent.ACTION_UP){
        if(handle!=-3 && (Math.abs(x-startX)>0.01 || Math.abs(y-startY)>0.01)){undo.addLast(new EditState(dragStart));redo.clear();}
        guides.clear();dragStart=null;drawing.getParent().requestDisallowInterceptTouchEvent(false);notifyChanged();
      }
      return true;
    }
    return true;
  }
  void draw(Canvas canvas,float scale){
    if(!active())return;Paint image=new Paint(Paint.FILTER_BITMAP_FLAG);image.setAlpha(Math.round(state.opacity*255));
    if(editor==null){canvas.save();canvas.concat(state.matrix());canvas.clipRect(state.crop);
    if(text!=null){canvas.saveLayerAlpha(state.crop,Math.round(state.opacity*255));new TextGlyphLayout(text).draw(canvas);canvas.restore();}else canvas.drawBitmap(source.preview,null,new RectF(0,0,source.size.displayWidth(),source.size.displayHeight()),image);canvas.restore();}
    guides.draw(canvas,state,drawing.documentWidth(),drawing.documentHeight(),scale,drawing.getResources().getDisplayMetrics().density);
    float[] p=state.handles();Paint frame=new Paint(Paint.ANTI_ALIAS_FLAG);frame.setColor(0xFF80CBC4);frame.setStrokeWidth(1.5f/scale);frame.setStyle(Paint.Style.STROKE);
    Path border=new Path();border.moveTo(p[0],p[1]);for(int i:new int[]{2,4,6})border.lineTo(p[i*2],p[i*2+1]);border.close();canvas.drawPath(border,frame);
    float radius=5*drawing.getResources().getDisplayMetrics().density/scale;frame.setStyle(Paint.Style.FILL);
    for(int i=0;i<8;i++)canvas.drawCircle(p[i*2],p[i*2+1],radius,frame);
    float offset=24*drawing.getResources().getDisplayMetrics().density/scale*1.6f;double angle=Math.toRadians(state.angle);
    float rx=state.cx+(float)Math.sin(angle)*(state.height/2+offset),ry=state.cy-(float)Math.cos(angle)*(state.height/2+offset);
    canvas.drawLine(p[2],p[3],rx,ry,frame);canvas.drawCircle(rx,ry,radius*1.2f,frame);
  }
  void confirm(){
    if(!active() || committing || host().readProjectProgress()!=null)return;
    PlacedImageSource original=source;PlacedImageTransform transform=state.copy();TextSpec confirmedText=text;
    RectF area=new RectF(transform.crop);transform.matrix().mapRect(area);
    Rect bounds=new Rect(Math.max(0,(int)Math.floor(area.left)),Math.max(0,(int)Math.floor(area.top)),Math.min(drawing.documentWidth(),(int)Math.ceil(area.right)),Math.min(drawing.documentHeight(),(int)Math.ceil(area.bottom)));
    if(bounds.isEmpty()){host().message("La imagen está fuera del lienzo");return;}
    committing=true;boolean[] committed={false};
    LayerWorkQueue.run(host(),()->{
      BitmapRegionDecoder decoder=null;TextGlyphLayout glyphs=confirmedText!=null?new TextGlyphLayout(confirmedText):null;
      try{
        if(!(editingIndex>=0?DrawingView.nativeBeginPlacedImageEdit(editingIndex,transform.opacity):DrawingView.nativeBeginPlacedImage(original.name,transform.opacity)))return false;
        decoder=BitmapRegionDecoder.newInstance(original.file.getPath(),false);if(decoder==null)return false;
        for(int y=bounds.top;y<bounds.bottom;y+=256)for(int x=bounds.left;x<bounds.right;x+=256){
          Rect tile=new Rect(x,y,Math.min(bounds.right,x+256),Math.min(bounds.bottom,y+256));Bitmap pixels=glyphs!=null?TextGlyphLayout.tile(glyphs,transform,tile):original.tile(decoder,transform,tile);
          try{int[] data=new int[tile.width()*tile.height()];pixels.getPixels(data,0,tile.width(),0,0,tile.width(),tile.height());if(!DrawingView.nativePlacedImageTile(tile.left,tile.top,tile.width(),tile.height(),data))return false;}
          finally{pixels.recycle();}
        }
        if(!(confirmedText!=null?DrawingView.nativePlacedTextElement(original.file.getPath(),transform.values(),confirmedText.bytes(),confirmedText.values()):DrawingView.nativePlacedImageElement(original.file.getPath(),transform.values())))return false;
        committed[0]=DrawingView.nativeCommitPlacedImage();return committed[0];
      }catch(Exception|OutOfMemoryError error){return false;}
      finally{if(decoder!=null)decoder.recycle();DrawingView.nativeCancelPlacedImage();}
    },true,"Insertando imagen como capa…","No se pudo insertar la imagen",()->drawing.deselect(),()->{committing=false;if(committed[0])clear();});
  }
}
