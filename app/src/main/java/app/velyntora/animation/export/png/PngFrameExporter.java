package app.velyntora.animation.export.png;
import app.velyntora.animation.export.AnimationFrames;import java.io.*;
public final class PngFrameExporter {
 public static void write(AnimationFrames frames,int selected,OutputStream out)throws IOException{frames.png(selected,out);}
 private PngFrameExporter(){}
}
