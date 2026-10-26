package art.velyntora.core;
import java.io.IOException;
/** Bounded original-coordinate EXIF stripe adapter shared by PNG and Android decoders. */
final class ImageImportStripe {
  static void write(int width,int height,int orientation,int top,int count,int[] source)throws IOException{
    boolean rotated=orientation>=5;
          int x=0,y=top,w=width,h=count;int[] pixels=source.length==width*count?source:java.util.Arrays.copyOf(source,width*count);
          if(orientation!=1){
            if(rotated){x=(orientation==6||orientation==7)?height-top-count:top;y=0;w=count;h=width;}
            else if(orientation==3||orientation==4)y=height-top-count;
            pixels=new int[width*count];
            for(int row=0;row<h;++row)for(int col=0;col<w;++col){
              int index=ExifOrientation.sourceIndex(x+col,y+row,width,height,orientation)-top*width;
              pixels[row*w+col]=source[index];
            }
          }
          if(!DrawingView.nativeImageImportRegion(x,y,w,h,pixels))throw new IOException("No se pudo guardar la región de la imagen");
  }
  private ImageImportStripe(){}
}
