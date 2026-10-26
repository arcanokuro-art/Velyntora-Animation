package art.velyntora.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/** Flat raster stack with normal alpha compositing; unsupported modes/groups are rejected. */
final class OpenRasterArchive {
  interface PngWriter {
    void write(int width, int height, int[] pixels, OutputStream output) throws IOException;
  }

  interface PngReader {
    int[] read(InputStream input, int width, int height, int x, int y) throws IOException;
  }

  static final class Layer {
    String name, source;
    boolean visible = true;
    float opacity = 1;
    int blendMode;
    int x, y;
  }

  static final class Stack {
    int width, height, active;
    final ArrayList<Layer> layers = new ArrayList<>();
  }

  private static String escaped(String text) {
    return text.replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("'", "&apos;")
        .replace("\n", "&#10;")
        .replace("\r", "&#13;")
        .replace("\t", "&#9;");
  }

  private static void entry(ZipOutputStream zip, String name, byte[] bytes, boolean stored)
      throws IOException {
    ZipEntry e = new ZipEntry(name);
    if (stored) {
      CRC32 crc = new CRC32();
      crc.update(bytes);
      e.setMethod(ZipEntry.STORED);
      e.setSize(bytes.length);
      e.setCompressedSize(bytes.length);
      e.setCrc(crc.getValue());
    }
    zip.putNextEntry(e);
    zip.write(bytes);
    zip.closeEntry();
  }

  static void write(InputStream project, OutputStream output, PngWriter png) throws IOException {
    VlyProjectStream.Reader reader = new VlyProjectStream.Reader(project);
    try (ZipOutputStream zip = new ZipOutputStream(output)) {
      entry(zip, "mimetype", "image/openraster".getBytes(StandardCharsets.US_ASCII), true);
      String[] xmlLayers = new String[reader.count];
      int[] merged = new int[reader.width * reader.height];
      for (int i = 0; i < reader.count; i++) {
        VlyProjectStream.Layer layer = reader.next();
        String path = "data/layer" + i + ".png";
        zip.putNextEntry(new ZipEntry(path));
        png.write(reader.width, reader.height, layer.pixels, zip);
        zip.closeEntry();
        xmlLayers[i] =
            "<layer name=\""
                + escaped(layer.name)
                + "\" src=\""
                + path
                + "\" opacity=\""
                + layer.opacity
                + "\" visibility=\""
                + (layer.visible ? "visible" : "hidden")
                + "\" composite-op=\"" + VlyProjectStream.ORA_MODES[layer.blendMode] + "\" x=\"0\" y=\"0\""
                + (i == reader.active ? " selected=\"true\"" : "")
                + "/>";
        if (layer.visible)
          for (int p = 0; p < merged.length; p++) {
            merged[p]=VlyProjectStream.composite(layer.pixels[p],merged[p],layer.opacity,layer.blendMode);
          }
      }
      reader.finish();
      StringBuilder xml =
          new StringBuilder(
              "<?xml version=\"1.0\" encoding=\"UTF-8\"?><image version=\"0.0.6\" w=\""
                  + reader.width
                  + "\" h=\""
                  + reader.height
                  + "\"><stack>");
      for (int i = reader.count - 1; i >= 0; i--) xml.append(xmlLayers[i]);
      xml.append("</stack></image>");
      entry(zip, "stack.xml", xml.toString().getBytes(StandardCharsets.UTF_8), false);
      zip.putNextEntry(new ZipEntry("mergedimage.png"));
      png.write(reader.width, reader.height, merged, zip);
      zip.closeEntry();
      int tw = reader.width, th = reader.height;
      if (Math.max(tw, th) > 256) {
        double scale = 256. / Math.max(tw, th);
        tw = Math.max(1, (int) Math.round(tw * scale));
        th = Math.max(1, (int) Math.round(th * scale));
      }
      int[] thumbnail = new int[tw * th];
      for (int y = 0; y < th; y++)
        for (int x = 0; x < tw; x++)
          thumbnail[y * tw + x] =
              merged[
                  (int) ((long) y * reader.height / th) * reader.width
                      + (int) ((long) x * reader.width / tw)];
      zip.putNextEntry(new ZipEntry("Thumbnails/thumbnail.png"));
      png.write(tw, th, thumbnail, zip);
      zip.closeEntry();
      zip.finish();
      zip.flush();
    }
  }

  static byte[] bounded(InputStream input, int max) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int n, total = 0;
    while ((n = input.read(buffer)) != -1) {
      total += n;
      if (total > max) throw new IOException("Entrada demasiado grande");
      out.write(buffer, 0, n);
    }
    return out.toByteArray();
  }

  static Stack metadata(byte[] xml) throws IOException {
    String raw =
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
            .decode(java.nio.ByteBuffer.wrap(xml))
            .toString();
    if (raw.startsWith("\uFEFF")) raw = raw.substring(1);
    if (raw.contains("<!DOCTYPE") || raw.contains("<!ENTITY"))
      throw new IOException("XML no compatible");
    Stack stack = new Stack();
    try {
      org.xml.sax.XMLReader parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
      parser.setEntityResolver(
          (publicId, systemId) -> {
            throw new SAXException("Entidades externas no permitidas");
          });
      parser.setContentHandler(
          new DefaultHandler() {
            int depth;
            boolean image, root;

            int number(Attributes a, String key, int defaultValue) {
              String v = a.getValue(key);
              return v == null ? defaultValue : Integer.parseInt(v);
            }

            public void startElement(String uri, String local, String name, Attributes a)
                throws SAXException {
              depth++;
              try {
                if (depth == 1 && name.equals("image") && !image) {
                  image = true;
                  stack.width = number(a, "w", 0);
                  stack.height = number(a, "h", 0);
                  VlyProjectStream.limits(stack.width, stack.height, 1, 0);
                } else if (depth == 2 && name.equals("stack") && !root) {
                  root = true;
                  if (a.getValue("opacity") != null
                      || a.getValue("visibility") != null
                      || a.getValue("composite-op") != null)
                    throw new IOException("Propiedades de grupo no compatibles");
                } else if (depth == 3 && name.equals("layer") && root) {
                  Layer layer = new Layer();
                  layer.source = a.getValue("src");
                  if (layer.source == null
                      || !layer.source.startsWith("data/")
                      || !layer.source.endsWith(".png")
                      || layer.source.contains("..")
                      || layer.source.contains("\\"))
                    throw new IOException("Ruta de capa inválida");
                  String mode = a.getValue("composite-op");
                  if(mode!=null){layer.blendMode=java.util.Arrays.asList(VlyProjectStream.ORA_MODES).indexOf(mode);if(layer.blendMode<0)throw new IOException("Modo de mezcla no compatible");}
                  layer.name = a.getValue("name");
                  if (layer.name == null) layer.name = "Capa";
                  if (layer.name.getBytes(StandardCharsets.UTF_8).length > 4096)
                    throw new IOException("Nombre demasiado largo");
                  String visible = a.getValue("visibility");
                  if (visible != null && !visible.equals("visible") && !visible.equals("hidden"))
                    throw new IOException("Visibilidad inválida");
                  layer.visible = !"hidden".equals(visible);
                  String opacity = a.getValue("opacity");
                  if (opacity != null) layer.opacity = Float.parseFloat(opacity);
                  if ((Float.isNaN(layer.opacity) || Float.isInfinite(layer.opacity))
                      || layer.opacity < 0
                      || layer.opacity > 1) throw new IOException("Opacidad inválida");
                  layer.x = number(a, "x", 0);
                  layer.y = number(a, "y", 0);
                  if ("true".equals(a.getValue("selected")) || "1".equals(a.getValue("selected")))
                    stack.active = stack.layers.size();
                  stack.layers.add(layer);
                  VlyProjectStream.limits(stack.width, stack.height, stack.layers.size(), 0);
                } else throw new IOException("Grupos o contenido no compatibles");
              } catch (Exception e) {
                throw new SAXException(e);
              }
            }

            public void endElement(String uri, String local, String name) {
              depth--;
            }

            public void endDocument() throws SAXException {
              if (!image || !root || stack.layers.isEmpty()) throw new SAXException("Sin capas");
            }
          });
      parser.parse(new InputSource(new StringReader(raw)));
    } catch (Exception e) {
      throw new IOException("Estructura OpenRaster incompatible", e);
    }
    return stack;
  }

  static void read(File archive, OutputStream project, PngReader png) throws IOException {
    try (ZipFile zip = new ZipFile(archive)) {
      Enumeration<? extends ZipEntry> entries = zip.entries();
      Set<String> names = new HashSet<>();
      int count = 0;
      while (entries.hasMoreElements()) {
        ZipEntry e = entries.nextElement();
        if (count++ == 0 && (!e.getName().equals("mimetype") || e.getMethod() != ZipEntry.STORED))
          throw new IOException("Mimetype inválido");
        if (count > 128 || !names.add(e.getName()))
          throw new IOException("Demasiadas entradas o entradas duplicadas");
      }
      ZipEntry mime = zip.getEntry("mimetype"), xml = zip.getEntry("stack.xml");
      if (mime == null || xml == null) throw new IOException("Archivo incompleto");
      try (InputStream input = zip.getInputStream(mime)) {
        if (!Arrays.equals(
            bounded(input, 64), "image/openraster".getBytes(StandardCharsets.US_ASCII)))
          throw new IOException("Formato incompatible");
      }
      Stack stack;
      try (InputStream input = zip.getInputStream(xml)) {
        stack = metadata(bounded(input, 1048576));
      }
      DataOutputStream out = new DataOutputStream(new BufferedOutputStream(project));
      VlyProjectStream.header(
          out,
          stack.width,
          stack.height,
          stack.layers.size(),
          stack.layers.size() - 1 - stack.active);
      for (int i = stack.layers.size() - 1; i >= 0; i--) {
        Layer layer = stack.layers.get(i);
        ZipEntry image = zip.getEntry(layer.source);
        if (image == null
            || image.isDirectory()
            || image.getSize() < 1
            || image.getSize() > 33554432) throw new IOException("Imagen de capa inválida");
        int[] pixels;
        try (InputStream input = zip.getInputStream(image)) {
          pixels = png.read(input, stack.width, stack.height, layer.x, layer.y);
        }
        VlyProjectStream.layer(
            out,
            new VlyProjectStream.Layer(layer.name, layer.visible, layer.opacity, pixels,layer.blendMode),
            stack.width * stack.height);
      }
      out.flush();
    }
  }
}
