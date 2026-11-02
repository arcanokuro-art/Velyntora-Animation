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
    if (old != null) adapter.rememberHistory(old);
    AnimationProject next = fresh(settings);
    try {
      adapter.create(settings.width, settings.height);
      project = next;
      savedAsset = null;
      capture();
      changedDocument = true;
      FrameDocumentAdapter.clearHistories();
    } catch (IOException error) {
      project = old;
      if (old != null) loadCurrent();
      throw error;
    }
  }

  void resume(String folder) throws IOException {
    if(project!=null)capture();
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
    if (old != null) adapter.rememberHistory(old);
    try {
      adapter.load(next, next.sequence.current().asset);
      project = next;
      savedAsset = next.sequence.current().asset;
      savedRevision = adapter.revision();
      changedDocument = true;
      FrameDocumentAdapter.clearHistories();
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
    adapter.recallHistory(project);
    savedAsset = project.sequence.current().asset;
    savedRevision = adapter.revision();
    changedDocument = true;
  }

  void mutate(Mutation mutation, boolean blank, boolean reload) throws IOException {
    capture();
    AnimationSequence before = project.sequence.copy();
    adapter.rememberHistory(project);
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
      java.util.HashSet<String> retained = new java.util.HashSet<>();
      for (int i=0;i<project.sequence.size();i++) retained.add(project.sequence.frame(i).id);
      for (int i=0;i<before.size();i++)
        if (!retained.contains(before.frame(i).id)) adapter.forgetHistory(project,before.frame(i).id);
    } catch (IOException | RuntimeException error) {
      project.sequence.restore(before);
      loadCurrent();
      throw error;
    }
  }


  /** Commit only a complete decoded sequence; cancel/error restores the previous editable document. */
  void importVideo(android.content.Context context,
      app.velyntora.animation.video.VideoImportRequest request,
      java.util.concurrent.atomic.AtomicBoolean canceled,
      java.util.function.IntConsumer progress) throws IOException {
    if(project!=null){capture();adapter.rememberHistory(project);}
    AnimationProject old=project;
    AnimationProject next=fresh(new Settings(request.title,request.width,request.height,request.fps));
    boolean committed=false;
    try(app.velyntora.animation.video.VideoFrameDecoder decoder=
        new app.velyntora.animation.video.VideoFrameDecoder(context,request)){
      java.util.ArrayList<AnimationSequence.Frame> frames=new java.util.ArrayList<>();
      for(int i=0;i<request.frameCount;i++){
        if(canceled.get())throw new java.util.concurrent.CancellationException();
        android.graphics.Bitmap bitmap=decoder.frame(i);
        try {
          if(canceled.get())throw new java.util.concurrent.CancellationException();
          adapter.importBitmap(bitmap);
        } finally {bitmap.recycle();}
        String asset=adapter.capture(next);
        frames.add(new AnimationSequence.Frame(AnimationSequence.uuid(),asset,1));
        progress.accept(i+1);
      }
      if(canceled.get())throw new java.util.concurrent.CancellationException();
      next.sequence.replace(frames,0,request.fps);
      adapter.load(next,next.sequence.current().asset);
      next.checkpoint();
      project=next;savedAsset=next.sequence.current().asset;savedRevision=adapter.revision();
      changedDocument=true;FrameDocumentAdapter.clearHistories();committed=true;
    } finally {
      if(!committed){
        project=old;
        try {if(old!=null)loadCurrent();}
        finally {
          // The directory is a newly generated private staging project.
          File[] objects=new File(next.directory,"objects").listFiles();
          if(objects!=null)for(File file:objects)file.delete();
          new File(next.directory,"objects").delete();
          new File(next.directory,"project.next").delete();
          new File(next.directory,"project.properties").delete();
          next.directory.delete();
        }
      }
    }
  }

  void exportArchive(OutputStream output) throws IOException {
    capture();
    app.velyntora.animation.export.project.EditableProjectExporter.write(project,output);
  }


  private app.velyntora.animation.export.AnimationFrames frames(
      java.util.concurrent.atomic.AtomicBoolean canceled,java.util.function.IntConsumer progress){
    final AnimationProject source=project;
    return new app.velyntora.animation.export.AnimationFrames(){
      public void check(){if(canceled.get())throw new java.util.concurrent.CancellationException();}
      public int width(){return source.width;}public int height(){return source.height;}
      public int fps(){return source.sequence.fps();}public int count(){return source.sequence.size();}
      public int ticks(){return source.sequence.totalTicks();}public int exposure(int i){return source.sequence.frame(i).exposure;}
      private void load(int index)throws IOException{check();adapter.load(source,source.sequence.frame(index).asset);}
      public android.graphics.Bitmap render(int index,int width,int height,boolean opaque)throws IOException{
        load(index);android.graphics.Bitmap image=adapter.render(width,height,opaque);progress.accept(index+1);return image;
      }
      public void png(int index,OutputStream out)throws IOException{load(index);adapter.writePng(out);progress.accept(index+1);}
    };
  }

  void exportPng(OutputStream output)throws IOException{
    capture();adapter.rememberHistory(project);
    try{
      app.velyntora.animation.export.png_sequence.PngSequenceExporter.write(
          frames(new java.util.concurrent.atomic.AtomicBoolean(),n->{}),output);
    }finally{loadCurrent();}
  }

  void exportFile(app.velyntora.animation.export.ExportRequest request,File file,
      java.util.concurrent.atomic.AtomicBoolean canceled,java.util.function.IntConsumer progress)throws IOException{
    capture();adapter.rememberHistory(project);
    app.velyntora.animation.export.AnimationFrames frames=frames(canceled,progress);
    try{
      frames.check();
      switch(request.kind){
       case MP4:app.velyntora.animation.export.mp4.Mp4Exporter.write(frames,request,file);break;
       case WEBM:app.velyntora.animation.export.webm.WebmExporter.write(frames,request,file);break;
       case THREE_GP:app.velyntora.animation.export.three_gp.ThreeGpExporter.write(frames,request,file);break;
       default:
        try(OutputStream out=new java.io.BufferedOutputStream(new FileOutputStream(file))){
         switch(request.kind){
          case PROJECT:app.velyntora.animation.export.project.EditableProjectExporter.write(project,out);break;
          case PNG_SEQUENCE:app.velyntora.animation.export.png_sequence.PngSequenceExporter.write(frames,out);break;
          case PNG_FRAME:app.velyntora.animation.export.png.PngFrameExporter.write(frames,project.sequence.selected(),out);break;
          case GIF:app.velyntora.animation.export.gif.GifExporter.write(frames,request,out);break;
          case APNG:app.velyntora.animation.export.apng.ApngExporter.write(frames,request,out);break;
          default:throw new IOException("Formato no disponible");
         }
        }
      }
      frames.check();
    }finally{loadCurrent();}
  }
}
