package app.velyntora.animation.storage;

import app.velyntora.animation.model.AnimationSequence;
import app.velyntora.animation.model.AnimationSequence.Frame;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Atomic manifest + immutable native objects: failed saves keep the last complete revision. */
public final class AnimationProject {
  public final File directory;
  public final AnimationSequence sequence;
  public String title;
  public int width, height;
  private static final long MAX_BYTES = 2L * 1024 * 1024 * 1024;

  public AnimationProject(File directory, String title, int width, int height, int fps)
      throws IOException {
    this.directory = directory;
    this.title = title;
    this.width = width;
    this.height = height;
    sequence = new AnimationSequence(fps);
    if (!new File(directory, "objects").mkdirs() && !new File(directory, "objects").isDirectory())
      throw new IOException("No se pudo crear la carpeta de la animación");
  }

  public File nativeFile(String asset) {
    checkId(asset);
    return new File(directory, "objects/" + asset + ".vlycore");
  }

  public File previewFile(String asset) {
    checkId(asset);
    return new File(directory, "objects/" + asset + ".png");
  }

  private static void checkId(String id) {
    if (!AnimationSequence.validId(id))
      throw new IllegalArgumentException("Archivo de fotograma inválido");
  }

  public void checkpoint() throws IOException {
    Properties props = new Properties();
    props.setProperty("format", "VLYANIM-1");
    props.setProperty("title", title);
    props.setProperty("width", "" + width);
    props.setProperty("height", "" + height);
    props.setProperty("fps", "" + sequence.fps());
    props.setProperty("selected", "" + sequence.selected());
    props.setProperty("count", "" + sequence.size());
    for (int i = 0; i < sequence.size(); i++) {
      Frame f = sequence.frame(i);
      if (f.asset == null || !nativeFile(f.asset).isFile() || !previewFile(f.asset).isFile())
        throw new IOException("Fotograma sin guardar");
      props.setProperty("frame." + i, f.id + "," + f.asset + "," + f.exposure);
    }
    File temp = new File(directory, "project.next");
    try (FileOutputStream stream = new FileOutputStream(temp);
        Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
      props.store(writer, "Velyntora Animation");
      writer.flush();
      stream.getFD().sync();
    }
    Files.move(
        temp.toPath(),
        new File(directory, "project.properties").toPath(),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE);
  }

  public void read() throws IOException {
    try {
      File manifest = new File(directory, "project.properties");
      if (manifest.length() > 1048576) throw new IOException("Metadatos demasiado grandes");
      Properties p = new Properties();
      try (Reader reader =
          new InputStreamReader(new FileInputStream(manifest), StandardCharsets.UTF_8)) {
        p.load(reader);
      }
      if (!"VLYANIM-1".equals(p.getProperty("format")))
        throw new IOException("El archivo no es un proyecto de Velyntora Animation compatible");
      int count = Integer.parseInt(p.getProperty("count")),
          w = Integer.parseInt(p.getProperty("width")),
          h = Integer.parseInt(p.getProperty("height"));
      if (count < 1
          || count > AnimationSequence.MAX_FRAMES
          || w < 1
          || h < 1
          || w > 6000
          || h > 6000) throw new IOException("Dimensiones o cantidad de fotogramas inválidas");
      ArrayList<Frame> frames = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        String[] parts = p.getProperty("frame." + i, "").split(",", -1);
        if (parts.length != 3) throw new IOException("Fotograma incompleto");
        Frame f = new Frame(parts[0], parts[1], Integer.parseInt(parts[2]));
        if (nativeFile(f.asset).length() == 0 || previewFile(f.asset).length() == 0)
          throw new IOException("Faltan archivos del fotograma " + (i + 1));
        frames.add(f);
      }
      sequence.replace(
          frames,
          Integer.parseInt(p.getProperty("selected")),
          Integer.parseInt(p.getProperty("fps")));
      title = p.getProperty("title", "Animación");
      if (title.length() > 200) throw new IOException("Título demasiado largo");
      width = w;
      height = h;
    } catch (IllegalArgumentException e) {
      throw new IOException("Metadatos de animación inválidos", e);
    }
  }

  public void collectUnusedObjects() {
    HashSet<String> keep = new HashSet<>();
    for (int i = 0; i < sequence.size(); i++) {
      String key = sequence.frame(i).asset;
      keep.add(key + ".vlycore");
      keep.add(key + ".png");
    }
    File[] files = new File(directory, "objects").listFiles();
    if (files != null) for (File file : files) if (!keep.contains(file.getName())) file.delete();
  }

  public void exportArchive(OutputStream output) throws IOException {
    try (ZipOutputStream zip = new ZipOutputStream(output)) {
      entry(zip, new File(directory, "project.properties"), "project.properties");
      HashSet<String> written = new HashSet<>();
      for (int i = 0; i < sequence.size(); i++) {
        String asset = sequence.frame(i).asset;
        if (written.add(asset)) {
          entry(zip, nativeFile(asset), "objects/" + asset + ".vlycore");
          entry(zip, previewFile(asset), "objects/" + asset + ".png");
        }
      }
    }
  }

  private static void entry(ZipOutputStream zip, File file, String name) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    Files.copy(file.toPath(), zip);
    zip.closeEntry();
  }

  public void importArchive(InputStream input) throws IOException {
    long total = 0;
    int entries = 0;
    try (ZipInputStream zip = new ZipInputStream(input)) {
      ZipEntry entry;
      byte[] buffer = new byte[65536];
      HashSet<String> names = new HashSet<>();
      while ((entry = zip.getNextEntry()) != null) {
        String name = entry.getName();
        if (++entries > AnimationSequence.MAX_FRAMES * 2 + 1
            || !names.add(name)
            || (!"project.properties".equals(name)
                && !name.matches(
                    "objects/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.(png|vlycore)")))
          throw new IOException("Contenido de proyecto inválido");
        File destination = new File(directory, name);
        if (destination.exists())
          throw new IOException("La importación requiere una carpeta nueva");
        try (OutputStream out = new FileOutputStream(destination)) {
          int n;
          long size = 0;
          while ((n = zip.read(buffer)) != -1) {
            size += n;
            total += n;
            if (size > 536870912L
                || total > MAX_BYTES
                || (name.equals("project.properties") && size > 1048576))
              throw new IOException("Proyecto demasiado grande");
            out.write(buffer, 0, n);
          }
        }
      }
    }
    try {
      read();
    } catch (IOException e) {
      new File(directory, "project.properties").delete();
      throw e;
    }
  }

  public static File[] recent(File root) {
    File[] dirs =
        root.listFiles(f -> f.isDirectory() && new File(f, "project.properties").isFile());
    if (dirs == null) return new File[0];
    Arrays.sort(
        dirs,
        (a, b) ->
            Long.compare(
                new File(b, "project.properties").lastModified(),
                new File(a, "project.properties").lastModified()));
    return dirs;
  }
}
