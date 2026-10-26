package art.velyntora.core;
import art.velyntora.core.document.CanvasLimits;

/** Immutable proportional import plan, including the displayed EXIF dimensions. */
final class ImageImportSize {
  final int sourceWidth,sourceHeight,width,height,orientation;
  ImageImportSize(int sourceWidth,int sourceHeight,int orientation) {
    if(sourceWidth<=0 || sourceHeight<=0)throw new IllegalArgumentException("Dimensiones inválidas");
    this.sourceWidth=sourceWidth;this.sourceHeight=sourceHeight;
    this.orientation=orientation>=1 && orientation<=8?orientation:1;
    double scale=Math.min(1.0,CanvasLimits.MAX_SIDE/(double)Math.max(sourceWidth,sourceHeight));
    width=Math.max(1,(int)Math.round(sourceWidth*scale));
    height=Math.max(1,(int)Math.round(sourceHeight*scale));
  }
  boolean reduced(){return width!=sourceWidth || height!=sourceHeight;}
  int sourceDisplayWidth(){return orientation>=5?sourceHeight:sourceWidth;}
  int sourceDisplayHeight(){return orientation>=5?sourceWidth:sourceHeight;}
  int displayWidth(){return orientation>=5?height:width;}
  int displayHeight(){return orientation>=5?width:height;}
  String confirmation(){return "Esta imagen mide "+sourceDisplayWidth()+" × "+sourceDisplayHeight()
      +" px y supera el límite permitido de 6000 px por lado.\n\n¿Quieres reducirla a "
      +displayWidth()+" × "+displayHeight()+" px para importarla? Se conservarán sus proporciones.";}
}
