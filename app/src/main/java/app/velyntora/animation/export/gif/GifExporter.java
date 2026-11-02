package app.velyntora.animation.export.gif;
import android.graphics.Bitmap;import app.velyntora.animation.export.*;import java.io.*;
/** GIF89a writer: RGB332 palette, binary transparency, bounded fixed-width LZW blocks. */
public final class GifExporter {
 private static void le(OutputStream out,int value)throws IOException{out.write(value);out.write(value>>8);}
 public static void write(AnimationFrames frames,ExportRequest request,OutputStream out)throws IOException{
  int w=request.width,h=request.height;out.write("GIF89a".getBytes("US-ASCII"));le(out,w);le(out,h);out.write(0xf7);out.write(0);out.write(0);
  for(int i=0;i<256;i++){out.write(i==1?0:((i>>5)&7)*255/7);out.write(i==1?0:((i>>2)&7)*255/7);out.write(i==1?0:(i&3)*255/3);}
  out.write(new byte[]{0x21,(byte)0xff,11});out.write("NETSCAPE2.0".getBytes("US-ASCII"));out.write(new byte[]{3,1,0,0,0});
  long ticks=0,previous=0;
  for(int index=0;index<frames.count();index++){
   ticks+=frames.exposure(index);long end=Math.round(ticks*100d/frames.fps());int delay=(int)Math.max(1,end-previous);previous=end;
   out.write(new byte[]{0x21,(byte)0xf9,4,9});le(out,delay);out.write(0);out.write(0);
   out.write(0x2c);le(out,0);le(out,0);le(out,w);le(out,h);out.write(0);out.write(8);
   Bitmap image=frames.render(index,w,h,false);
   try{
    Blocks blocks=new Blocks(out);blocks.code(256);
    int[] row=new int[w];int group=0;
    for(int y=0;y<h;y++){frames.check();image.getPixels(row,0,w,0,y,w,1);
     for(int color:row){if(group==200){blocks.code(256);group=0;}
      int p=((color>>16&255)>>5)<<5|((color>>8&255)>>5)<<2|(color&255)>>6;
      if((color>>>24)<128)p=0;else if(p==0)p=1;
      blocks.code(p);group++;
     }
    }
    blocks.code(257);blocks.finish();
   }finally{image.recycle();}
  }
  out.write(0x3b);
 }
 private static final class Blocks{
  final OutputStream out;final byte[] data=new byte[255];int count,bits,held;
  Blocks(OutputStream out){this.out=out;}
  void code(int value)throws IOException{held|=value<<bits;bits+=9;while(bits>=8){put(held&255);held>>>=8;bits-=8;}}
  void put(int value)throws IOException{data[count++]=(byte)value;if(count==255)flush();}
  void flush()throws IOException{if(count>0){out.write(count);out.write(data,0,count);count=0;}}
  void finish()throws IOException{if(bits>0)put(held&255);flush();out.write(0);}
 }
 private GifExporter(){}
}
