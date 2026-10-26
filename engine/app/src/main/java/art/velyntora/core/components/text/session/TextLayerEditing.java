package art.velyntora.core;

import android.graphics.RectF;

/** Coordinates text regeneration without mutating the confirmed layer. */
final class TextLayerEditing {
  static void apply(PlacedImageSession session,TextSpec next,int x,int y,boolean editing){
    MainActivity host=session.host();DrawingView drawing=session.drawing;
    if(host.readProjectProgress()!=null || (editing && (!session.active() || session.text==null)) || (!editing && session.active()))return;
    float stretch=editing?(session.state.width/session.state.crop.width())/(session.state.height/session.state.crop.height()):1;
    TextSpec normalized=new TextSpec(next.content,next.size,next.color,next.family,next.style,Math.max(8,Math.min(6000,Math.round(next.wrapWidth/stretch))));
    PlacedImageSource[] rendered={null};
    LayerWorkQueue.run(host,()->{try{rendered[0]=TextSourceRenderer.render(host,normalized);return true;}catch(Exception|OutOfMemoryError error){return false;}},false,"Preparando texto…","No se pudo preparar el texto. Reduce su tamaño o la cantidad de líneas.",()->{
      if(host.isDestroyed()){rendered[0].close();return;}
      if(!editing){
        session.begin(rendered[0]);if(session.source!=rendered[0])return;session.text=normalized;
        session.state.width=session.source.size.displayWidth();session.state.height=session.source.size.displayHeight();session.state.cx=x+session.state.width/2;session.state.cy=y+session.state.height/2;session.state.opacity=drawing.toolOpacityPercent()/100f;
      }else{
        session.remember();PlacedImageTransform state=session.state;float[] origin={0,0};state.matrix().mapPoints(origin);float sx=stretch,sy=1;
        session.retainSource(rendered[0]);session.text=normalized;state.crop=new RectF(0,0,session.source.size.displayWidth(),session.source.size.displayHeight());state.width=state.crop.width()*sx;state.height=state.crop.height()*sy;
        float fit=Math.min(1,Math.min(24000/state.width,24000/state.height));state.width*=fit;state.height*=fit;
        double angle=Math.toRadians(state.angle);state.cx=origin[0]+(float)(state.width/2*Math.cos(angle)-state.height/2*Math.sin(angle));state.cy=origin[1]+(float)(state.width/2*Math.sin(angle)+state.height/2*Math.cos(angle));
      }
      session.notifyChanged();host.refreshLayerPanel();
    },()->{if(host.isDestroyed() && rendered[0]!=null)rendered[0].close();});
  }
}
