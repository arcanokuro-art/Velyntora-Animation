package art.velyntora.core;

import app.velyntora.animation.model.AnimationSequence;
import app.velyntora.animation.setup.AnimationSetupDialog.Settings;
import app.velyntora.animation.storage.AnimationProject;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Serialized worker-side session commands; native mutations never race user input. */
final class AnimationSession {
  interface Mutation {
    void apply(AnimationSequence sequence);
  }

  private final FrameDocumentAdapter adapter;
  private final File root;
  private AnimationProject project;
  private long savedRevision = -1;
  private String savedAsset;
  boolean changedDocument;

  AnimationSession(FrameDocumentAdapter adapter, File root) {
    this.adapter = adapter;
    this.root = root;
  }

  AnimationProject project() {
    return project;
  }

  private AnimationProject fresh(Settings settings) throws IOException {
    return new AnimationProject(
        new File(root, AnimationSequence.uuid()),
        settings.title,
        settings.width,
        settings.height,
        settings.fps);
  }

  void create(Settings settings) throws IOException {
    if (project != null) capture();
    AnimationProject old = project;
    AnimationProject next = fresh(settings);
    try {
      adapter.create(settings.width, settings.height);
      project = next;
      savedAsset = null;
      capture();
      changedDocument = true;
    } catch (IOException error) {
      project = old;
      if (old != null) loadCurrent();
      throw error;
    }
  }

  void resume(String folder) throws IOException {
    AnimationProject next = new AnimationProject(new File(folder), "Animación", 1280, 720, 12);
    next.read();
    replace(next);
  }

  void open(InputStream input) throws IOException {
    if (project != null) capture();
    AnimationProject next = fresh(new Settings("Animación", 1280, 720, 12));
    try {
      next.importArchive(input);
      replace(next);
    } catch (IOException e) {
      new File(next.directory, "project.properties").delete();
      throw e;
    }
  }

  private void replace(AnimationProject next) throws IOException {
    AnimationProject old = project;
    try {
      adapter.load(next, next.sequence.current().asset);
      project = next;
      savedAsset = next.sequence.current().asset;
      savedRevision = adapter.revision();
      changedDocument = true;
    } catch (IOException error) {
      if (old != null) loadCurrent();
      throw error;
    }
  }

  boolean hasChanges() {
    return project != null && (savedAsset == null || adapter.revision() != savedRevision);
  }

  void capture() throws IOException {
    if (project == null) return;
    AnimationSequence s = project.sequence;
    if (s.current().asset != null
        && s.current().asset.equals(savedAsset)
        && savedRevision == adapter.revision()) {
      project.checkpoint();
      return;
    }
    String old = s.current().asset;
    try {
      String asset = adapter.capture(project);
      s.asset(asset);
      project.checkpoint();
      savedAsset = asset;
      savedRevision = adapter.revision();
      project.collectUnusedObjects();
    } catch (IOException e) {
      s.asset(old);
      throw e;
    }
  }

  private void loadCurrent() throws IOException {
    adapter.load(project, project.sequence.current().asset);
    savedAsset = project.sequence.current().asset;
    savedRevision = adapter.revision();
    changedDocument = true;
  }

  void mutate(Mutation mutation, boolean blank, boolean reload) throws IOException {
    capture();
    AnimationSequence before = project.sequence.copy();
    try {
      mutation.apply(project.sequence);
      if (blank) {
        adapter.create(project.width, project.height);
        savedAsset = null;
        capture();
        changedDocument = true;
      } else {
        if (reload) loadCurrent();
        project.checkpoint();
        project.collectUnusedObjects();
      }
    } catch (IOException | RuntimeException error) {
      project.sequence.restore(before);
      loadCurrent();
      throw error;
    }
  }

  void exportArchive(OutputStream output) throws IOException {
    capture();
    project.exportArchive(output);
  }

  void exportPng(OutputStream output) throws IOException {
    capture();
    AnimationSequence sequence = project.sequence;
    try (ZipOutputStream zip = new ZipOutputStream(output)) {
      zip.putNextEntry(new ZipEntry("timing.csv"));
      StringBuilder timing = new StringBuilder("frame,exposure,fps\n");
      for (int i = 0; i < sequence.size(); i++)
        timing
            .append(i + 1)
            .append(',')
            .append(sequence.frame(i).exposure)
            .append(',')
            .append(sequence.fps())
            .append('\n');
      zip.write(timing.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      zip.closeEntry();
      for (int i = 0; i < sequence.size(); i++) {
        adapter.load(project, sequence.frame(i).asset);
        zip.putNextEntry(new ZipEntry(String.format(Locale.ROOT, "frame-%05d.png", i + 1)));
        adapter.writePng(zip);
        zip.closeEntry();
      }
    } finally {
      loadCurrent();
    }
  }
}
