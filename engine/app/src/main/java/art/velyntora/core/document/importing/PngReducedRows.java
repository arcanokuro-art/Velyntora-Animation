package art.velyntora.core;
import java.io.IOException;

/** One-pass bilinear reduction with premultiplied alpha and only two source rows. */
final class PngReducedRows {
  private final ImageImportSize size;
  private int[] previous,current;
  private final int[] output;
  private final int rows;
  private int next=0,buffered=0;
  PngReducedRows(ImageImportSize size){
    this.size=size;previous=new int[size.sourceWidth];current=new int[size.sourceWidth];
    rows=Math.max(1,Math.min(64,524288/size.width));output=new int[size.width*rows];
  }
  void row(int y,int[] pixels,int offset)throws IOException{
    int[] swap=previous;previous=current;current=swap;
    System.arraycopy(pixels,offset,current,0,size.sourceWidth);
    if(y==0)System.arraycopy(current,0,previous,0,current.length);
    while(next<size.height){
      double sy=Math.max(0,Math.min(size.sourceHeight-1,(next+.5)*size.sourceHeight/size.height-.5));
      int lower=(int)Math.floor(sy),upper=Math.min(size.sourceHeight-1,lower+1);
      if(upper>y)break;
      int[] a=lower==y?current:previous;double fy=sy-lower;
      for(int x=0;x<size.width;x++){
        double sx=Math.max(0,Math.min(size.sourceWidth-1,(x+.5)*size.sourceWidth/size.width-.5));
        int left=(int)Math.floor(sx),right=Math.min(size.sourceWidth-1,left+1);double fx=sx-left;
        output[buffered*size.width+x]=mix(a[left],a[right],current[left],current[right],fx,fy);
      }
      next++;buffered++;if(buffered==rows)flush();
    }
  }
  private static int mix(int a,int b,int c,int d,double x,double y){
    double wa=(1-x)*(1-y),wb=x*(1-y),wc=(1-x)*y,wd=x*y;
    double aa=(a>>>24)*wa,ab=(b>>>24)*wb,ac=(c>>>24)*wc,ad=(d>>>24)*wd;
    double alpha=aa+ab+ac+ad;if(alpha<.5)return 0;
    int result=(int)Math.round(alpha)<<24;
    for(int shift=16;shift>=0;shift-=8)result|=(int)Math.round((((a>>shift)&255)*aa+((b>>shift)&255)*ab+((c>>shift)&255)*ac+((d>>shift)&255)*ad)/alpha)<<shift;
    return result;
  }
  private void flush()throws IOException{
    if(buffered==0)return;
    ImageImportStripe.write(size.width,size.height,size.orientation,next-buffered,buffered,output);
    buffered=0;
  }
  void finish()throws IOException{
    if(next!=size.height)throw new IOException("Reducción PNG incompleta");flush();
  }
}
