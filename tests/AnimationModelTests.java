import app.velyntora.animation.model.AnimationSequence;
import app.velyntora.animation.playback.PlaybackClock;
import app.velyntora.animation.storage.AnimationProject;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class AnimationModelTests {
  static int assertions;

  static void check(boolean condition) {
    assertions++;
    if (!condition) throw new AssertionError("Assertion " + assertions);
  }

  static void rejects(Runnable task) {
    try {
      task.run();
      throw new AssertionError("Expected validation failure");
    } catch (IllegalArgumentException | IllegalStateException expected) {
      assertions++;
    }
  }

  static void seed(AnimationProject project) throws Exception {
    String asset = AnimationSequence.uuid();
    Files.write(project.nativeFile(asset).toPath(), new byte[] {1, 2, 3});
    Files.write(project.previewFile(asset).toPath(), new byte[] {4, 5, 6});
    project.sequence.asset(asset);
    project.checkpoint();
  }

  public static void main(String[] args) throws Exception {
    AnimationSequence s = new AnimationSequence(12);
    rejects(() -> s.delete());
    rejects(() -> s.fps(0));
    rejects(() -> s.fps(31));
    rejects(() -> s.fps(60));
    rejects(() -> s.select(1));
    String asset = AnimationSequence.uuid();
    s.asset(asset);
    String first = s.current().id;
    s.insert(true);
    check(s.size() == 2 && s.selected() == 1 && s.current().asset.equals(asset));
    check(!s.current().id.equals(first));
    s.exposure(3);
    s.select(0);
    s.exposure(2);
    check(s.totalTicks() == 5);
    check(
        s.frameAtTick(0) == 0
            && s.frameAtTick(1) == 0
            && s.frameAtTick(2) == 1
            && s.frameAtTick(4) == 1
            && s.frameAtTick(5) == 0);
    AnimationSequence copy = s.copy();
    s.move(1);
    check(s.current().id.equals(first));
    s.delete();
    check(s.size() == 1);
    s.restore(copy);
    check(s.size() == 2 && s.current().id.equals(first));
    rejects(() -> s.exposure(0));
    rejects(() -> s.replace(Arrays.asList(s.current(), s.current()), 0, 12));
    PlaybackClock clock = new PlaybackClock(s, 1000);
    check(clock.frame(1000) == 0);
    check(clock.frame(1167) == 1);
    check(clock.frame(1417) == 0);
    s.fps(30);
    check(clock.frame(1167) == 1);
    AnimationProject p =
        new AnimationProject(
            Files.createTempDirectory("animation-model").toFile(), "Prueba ñ", 1280, 720, 24);
    seed(p);
    p.sequence.insert(true);
    p.sequence.exposure(4);
    p.checkpoint();
    ByteArrayOutputStream data = new ByteArrayOutputStream();
    p.exportArchive(data);
    AnimationProject restored =
        new AnimationProject(Files.createTempDirectory("animation-import").toFile(), "x", 1, 1, 1);
    restored.importArchive(new ByteArrayInputStream(data.toByteArray()));
    check(
        restored.title.equals("Prueba ñ")
            && restored.sequence.fps() == 24
            && restored.sequence.size() == 2
            && restored.sequence.current().exposure == 4);
    check(restored.width == 1280 && restored.height == 720);
    check(
        Arrays.equals(
            Files.readAllBytes(restored.nativeFile(restored.sequence.current().asset).toPath()),
            new byte[] {1, 2, 3}));
    check(
        new File(restored.directory, "objects").list().length
            == 2); // Duplicates share immutable assets.
    String orphan = AnimationSequence.uuid();
    Files.write(restored.nativeFile(orphan).toPath(), new byte[] {9});
    restored.collectUnusedObjects();
    check(!restored.nativeFile(orphan).exists());
    check(restored.nativeFile(restored.sequence.current().asset).isFile());
    ByteArrayOutputStream malicious = new ByteArrayOutputStream();
    try (ZipOutputStream z = new ZipOutputStream(malicious)) {
      z.putNextEntry(new ZipEntry("../escape"));
      z.write(0);
      z.closeEntry();
    }
    try {
      new AnimationProject(Files.createTempDirectory("animation-bad").toFile(), "x", 1, 1, 1)
          .importArchive(new ByteArrayInputStream(malicious.toByteArray()));
      throw new AssertionError();
    } catch (IOException expected) {
      assertions++;
    }
    String old = Files.readString(new File(p.directory, "project.properties").toPath());
    p.sequence.insert(false);
    try {
      p.checkpoint();
      throw new AssertionError();
    } catch (IOException expected) {
      assertions++;
    }
    check(Files.readString(new File(p.directory, "project.properties").toPath()).equals(old));

    // 130 minutes at 30 FPS: verify real large metadata, archive round trip and indexed timing.
    AnimationProject large=new AnimationProject(Files.createTempDirectory("animation-234000").toFile(),"130 minutos",64,48,30);
    seed(large);
    String shared=large.sequence.current().asset;
    ArrayList<AnimationSequence.Frame> many=new ArrayList<>();
    for(int n=0;n<234000;n++)many.add(new AnimationSequence.Frame(AnimationSequence.uuid(),shared,1));
    large.sequence.replace(many,233999,30);
    check(large.sequence.totalTicks()==234000);
    check(large.sequence.frameAtTick(233999)==233999);
    check(large.sequence.frameAtTick(234000)==0);
    check(large.sequence.frameAtTick(-1)==233999);
    check(large.sequence.firstTick(234000)==234000);
    rejects(()->large.sequence.insert(true));
    large.checkpoint();
    check(new File(large.directory,"project.properties").length()>1048576);
    large.read();
    check(large.sequence.size()==234000 && large.sequence.selected()==233999 && large.sequence.fps()==30);
    ByteArrayOutputStream packed=new ByteArrayOutputStream();large.exportArchive(packed);
    AnimationProject unpacked=new AnimationProject(Files.createTempDirectory("animation-234000-import").toFile(),"x",1,1,1);
    unpacked.importArchive(new ByteArrayInputStream(packed.toByteArray()));
    check(unpacked.sequence.size()==234000 && unpacked.sequence.totalTicks()==234000);
    unpacked.sequence.exposure(2);
    check(unpacked.sequence.totalTicks()==234001 && unpacked.sequence.frameAtTick(234000)==233999);
    unpacked.sequence.move(-1);
    check(unpacked.sequence.firstTick(233999)==234000);
    unpacked.sequence.delete();
    check(unpacked.sequence.totalTicks()==233999);
    rejects(()->unpacked.sequence.replace(many,0,31));

    System.out.println("Animation model/storage/clock: " + assertions + " assertions passed");
  }
}
