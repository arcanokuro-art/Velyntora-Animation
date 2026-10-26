package art.velyntora.core;
import java.io.*;import java.util.*;import java.nio.charset.StandardCharsets;
public final class XRasterReaderTests {
 static InputStream in(String s){return new ByteArrayInputStream(s.getBytes(StandardCharsets.US_ASCII));}
 static void check(boolean v){if(!v)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  String xbm="#define test_width 9\n#define test_height 2\nstatic unsigned char test_bits[] = {0x01,0x01,0,0};";int[] p=XRasterReader.xbm(in(xbm)).pixels;check(p.length==18&&p[0]==0xff000000&&p[8]==0xff000000&&p[9]==0xffffffff);
  check(XRasterReader.xbm(in("#define a_width 17\n#define a_height 1\nstatic short a_bits[]={0xffff,1};")).pixels[16]==0xff000000);
  String xpm="/* XPM */static char *p[]={\"3 2 3 1\",\"a c #f00\",\"b c None\",\"c c Light Goldenrod Yellow\",\"abc\",\"cba\"};";p=XRasterReader.xpm(in(xpm)).pixels;check(Arrays.equals(p,new int[]{0xffff0000,0,0xfffafad2,0xfffafad2,0,0xffff0000}));
  check(XRasterReader.xpm(in("\"1 1 1 2\",\"aa c #ffff00000000\",\"aa\"")).pixels[0]==0xffff0000);
  for(String bad:new String[]{xpm.replace("abc","abd"),xpm.replace("3 2 3 1","10001 10001 3 1"),xpm.replace("#f00","#ff"),xpm.replace("b c None","a c None")}){try{XRasterReader.xpm(in(bad));throw new AssertionError();}catch(IOException expected){}}
  try{XRasterReader.xbm(in(xbm.replace("0x01,0x01,0,0","0x100,1,0,0")));throw new AssertionError();}catch(IOException expected){}
  System.out.println("XBM row padding and XPM transparency, X11 colors, palettes and limits passed");
 }
}
