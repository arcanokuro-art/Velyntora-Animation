package app.velyntora.animation.export.mp4;
import app.velyntora.animation.export.*;import app.velyntora.animation.export.video.PlatformVideoEncoder;import java.io.*;
public final class Mp4Exporter{
 public static void write(AnimationFrames frames,ExportRequest request,File file)throws IOException{
  PlatformVideoEncoder.write(frames,request,file,"video/avc",android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
 }
 private Mp4Exporter(){}
}
