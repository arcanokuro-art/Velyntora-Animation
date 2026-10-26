import app.velyntora.animation.frames.FrameSequence;
import app.velyntora.animation.playback.PlaybackClock;
import app.velyntora.animation.project.AnimationProject;
import java.nio.file.*;import java.io.*;import java.util.*;import java.util.zip.*;
public class AnimationTests {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  FrameSequence s=new FrameSequence();String a=s.current(),b=s.insert();s.move(-1);check(s.selected()==0&&s.id(0).equals(b));s.delete();check(s.current().equals(a));
  try{s.delete();throw new AssertionError();}catch(IllegalStateException expected){}
  try{s.fps(0);throw new AssertionError();}catch(IllegalArgumentException expected){}
  try{s.restore(Arrays.asList("../../escape"),0,12);throw new AssertionError();}catch(IllegalArgumentException expected){}
  PlaybackClock clock=new PlaybackClock(1000,1,3,12);check(clock.frame(1000)==1);check(clock.frame(1250)==1);check(clock.frame(1500)==1);check(clock.frame(1084)==2);
  AnimationProject p=new AnimationProject(Files.createTempDirectory("anim-test").toFile());
  Files.write(p.frame(p.sequence.current()).toPath(),new byte[]{1,2,3});Files.write(p.preview(p.sequence.current()).toPath(),new byte[]{4,5,6});p.sequence.fps(24);p.checkpoint();
  ByteArrayOutputStream data=new ByteArrayOutputStream();p.export(data,false);
  AnimationProject q=new AnimationProject(Files.createTempDirectory("anim-import").toFile());q.importArchive(new ByteArrayInputStream(data.toByteArray()));check(q.sequence.fps()==24);check(Arrays.equals(Files.readAllBytes(q.frame(q.sequence.current()).toPath()),new byte[]{1,2,3}));
  ByteArrayOutputStream bad=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(bad)){z.putNextEntry(new ZipEntry("../escape"));z.write(0);z.closeEntry();}
  try{new AnimationProject(Files.createTempDirectory("anim-bad").toFile()).importArchive(new ByteArrayInputStream(bad.toByteArray()));throw new AssertionError();}catch(IOException expected){}
  System.out.println("Animation sequence, playback, layered archive round trip and traversal rejection: OK");
 }
}
