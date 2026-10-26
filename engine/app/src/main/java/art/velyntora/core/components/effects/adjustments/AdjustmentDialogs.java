package art.velyntora.core;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

/** Parameter editors for nondestructive-to-history, alpha-preserving color operations. */
final class AdjustmentDialogs {
  private final MainActivity activity;
  private final DrawingView drawing;

  AdjustmentDialogs(MainActivity a, DrawingView d) {
    activity = a;
    drawing = d;
  }

  void addMenu(LinearLayout parent) {
    Button b = new Button(activity);
    b.setText("Ajustes");
    parent.addView(b);
    b.setOnClickListener(
        v ->
            new AlertDialog.Builder(activity)
                .setTitle("Ajustes de color")
                .setItems(
                    new String[] {
                      "Curvas…",
                      "Niveles por canal…",
                      "Niveles automáticos…",
                      "Posterización RGB…",
                      "Tono / saturación / luminosidad…",
                      "Brillo / contraste…",
                      "Blanco y negro",
                      "Invertir colores",
                      "Sepia"
                    },
                    (dialog, index) -> {
                      if (index == 0) curves();
                      else if (index == 1) levels();
                      else if (index == 2)
                        parameters(
                            "Niveles automáticos",
                            2,
                            new String[] {"Recorte por extremo (‰)"},
                            new int[] {0},
                            new int[] {50},
                            new int[] {5});
                      else if (index == 3)
                        parameters(
                            "Posterización RGB",
                            3,
                            new String[] {"Rojo", "Verde", "Azul"},
                            new int[] {2, 2, 2},
                            new int[] {256, 256, 256},
                            new int[] {4, 4, 4});
                      else if (index == 5)
                        parameters(
                            "Brillo / contraste",
                            5,
                            new String[] {"Brillo (%)", "Contraste (%)"},
                            new int[] {-100, -100},
                            new int[] {100, 100},
                            new int[] {0, 0});
                      else if (index >= 6)
                        activity.runColorOperation(
                            () -> drawing.colorAdjustment(6, new int[] {index - 6}));
                      else
                        parameters(
                            "Tono / saturación / luminosidad",
                            4,
                            new String[] {"Tono (°)", "Saturación (%)", "Luminosidad (%)"},
                            new int[] {-180, 0, -100},
                            new int[] {180, 200, 100},
                            new int[] {0, 100, 0});
                    })
                .show());
  }

  void openBlurMenu() {
    new AlertDialog.Builder(activity)
        .setTitle("Desenfoques")
        .setItems(
            new String[] {"Gaussiano", "Movimiento", "Radial", "Zoom"},
            (d, kind) -> blurDialog(kind))
        .show();
  }

  private void blurDialog(int kind) {
    String[] titles = {"Gaussiano", "Movimiento", "Radial", "Zoom"};
    LinearLayout f = form();
    int[] values = {kind == 0 ? 3 : 10, 0, 50, 50};
    slider(
        f,
        kind == 0
            ? "Radio (px)"
            : kind == 1 ? "Distancia (px)" : kind == 2 ? "Ángulo del arco (°)" : "Intensidad (%)",
        0,
        kind == 0 ? 32 : kind == 2 ? 45 : 100,
        values[0],
        n -> values[0] = n);
    if (kind == 1) slider(f, "Dirección (°)", -180, 180, 0, n -> values[1] = n);
    if (kind >= 2) {
      slider(f, "Centro horizontal (%)", 0, 100, 50, n -> values[2] = n);
      slider(f, "Centro vertical (%)", 0, 100, 50, n -> values[3] = n);
    }
    show(
        "Desenfoque " + titles[kind],
        f,
        () ->
            activity.runColorOperation(
                () -> drawing.blur(kind, values[0], values[1], values[2], values[3])));
  }

  void openDistortionMenu() {
    new AlertDialog.Builder(activity)
        .setTitle("Distorsiones")
        .setItems(
            new String[] {
              "Remolino", "Abombar / pellizcar", "Ondas radiales", "Cristales", "Escarcha"
            },
            (d, kind) -> distortionDialog(kind))
        .show();
  }

  private void distortionDialog(int kind) {
    LinearLayout f = form();
    int[] values = {40, 50, 0, 50, 50};
    slider(
        f,
        kind == 0 ? "Giro (°)" : "Intensidad",
        kind == 0 ? -180 : kind == 1 || kind == 2 || kind == 3 ? -100 : 0,
        kind == 0 ? 180 : 100,
        40,
        n -> values[0] = n);
    if (kind != 4)
      slider(f, kind <= 1 ? "Radio (%)" : "Periodo (px)", 1, 100, 50, n -> values[1] = n);
    if (kind == 3) slider(f, "Dirección (°)", -180, 180, 0, n -> values[2] = n);
    if (kind < 4) {
      slider(f, "Centro horizontal (%)", 0, 100, 50, n -> values[3] = n);
      slider(f, "Centro vertical (%)", 0, 100, 50, n -> values[4] = n);
    }
    show(
        "Distorsión",
        f,
        () ->
            activity.runColorOperation(
                () ->
                    drawing.distortion(
                        kind, values[0], values[1], values[2], values[3], values[4])));
  }

  void openArtisticMenu() {
    new AlertDialog.Builder(activity)
        .setTitle("Arte y fotografía")
        .setItems(
            new String[] {
              "Pintura al óleo",
              "Boceto a lápiz",
              "Boceto a tinta",
              "Resplandor",
              "Retrato suave",
              "Mediana / percentil",
              "Reducir ojos rojos"
            },
            (d, kind) -> artisticDialog(kind))
        .show();
  }

  private void artisticDialog(int kind) {
    LinearLayout f = form();
    int[] values = {kind == 0 ? 8 : kind == 5 ? 50 : 100, 2, 80};
    if (kind == 6) label(f, "Selecciona el área de los ojos para limitar la corrección.");
    slider(
        f,
        kind == 0 ? "Niveles de intensidad" : kind == 5 ? "Percentil (%)" : "Intensidad (%)",
        kind == 0 ? 2 : 0,
        kind == 0 ? 32 : 100,
        values[0],
        n -> values[0] = n);
    if (kind != 2 && kind != 6)
      slider(f, "Radio (px)", 1, kind == 0 ? 8 : kind == 5 ? 4 : 16, 2, n -> values[1] = n);
    if (kind == 2 || kind == 6)
      slider(f, kind == 2 ? "Umbral de contorno" : "Rojo mínimo", 0, 255, 80, n -> values[2] = n);
    show(
        "Arte y fotografía",
        f,
        () ->
            activity.runColorOperation(
                () -> drawing.artistic(kind, values[0], values[1], values[2])));
  }

  void openRenderMenu() {
    new AlertDialog.Builder(activity)
        .setTitle("Generadores")
        .setItems(
            new String[] {"Nubes", "Voronoi", "Celdas", "Mandelbrot", "Julia"},
            (d, kind) -> renderDialog(kind))
        .show();
  }

  private void renderDialog(int kind) {
    LinearLayout f = form();
    label(
        f,
        "Primer color: color de dibujo actual. El resultado reemplaza los píxeles de la capa o"
            + " selección.");
    int[] values = {kind < 3 ? 64 : 1, kind == 0 ? 5 : 48, 0};
    slider(
        f,
        kind < 3 ? "Escala (px)" : "Zoom",
        1,
        kind < 3 ? 512 : 100,
        values[0],
        n -> values[0] = n);
    if (kind == 0 || kind >= 3)
      slider(
          f,
          kind == 0 ? "Octavas" : "Iteraciones",
          1,
          kind == 0 ? 8 : 128,
          values[1],
          n -> values[1] = n);
    if (kind != 3) slider(f, "Semilla", 0, 10000, 0, n -> values[2] = n);
    Spinner second = new Spinner(activity);
    second.setAdapter(
        new ArrayAdapter<String>(
            activity,
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {
              "Segundo color: blanco", "Segundo color: negro", "Segundo color: transparente"
            }));
    f.addView(second);
    show(
        "Generar",
        f,
        () -> {
          int selected = second.getSelectedItemPosition(),
              color = selected == 0 ? 0xffffffff : selected == 1 ? 0xff000000 : 0;
          activity.runColorOperation(
              () -> drawing.render(kind, values[0], values[1], values[2], color));
        });
  }

  void openUtilityMenu() {
    new AlertDialog.Builder(activity)
        .setTitle("Más filtros")
        .setItems(
            new String[] {
              "Fragmentar",
              "Desenfoque de lente",
              "Abolladuras",
              "Inversión polar",
              "Reducir ruido",
              "Contorno de bordes",
              "Relieve direccional",
              "Tramado ordenado"
            },
            (d, kind) -> utilityDialog(kind))
        .show();
  }

  private void utilityDialog(int kind) {
    LinearLayout f = form();
    int[] values = {
      kind == 0 || kind == 1 ? 4 : 100,
      kind == 0 ? 4 : kind == 2 ? 32 : kind == 3 ? 100 : kind == 7 ? 4 : 2,
      kind == 4 ? 60 : 0
    };
    slider(
        f,
        kind == 0 ? "Distancia (px)" : kind == 1 ? "Radio (px)" : "Intensidad (%)",
        0,
        kind == 0 ? 64 : kind == 1 ? 32 : 100,
        values[0],
        n -> values[0] = n);
    if (kind != 1)
      slider(
          f,
          kind == 0
              ? "Fragmentos"
              : kind == 2
                  ? "Escala (px)"
                  : kind == 3 ? "Radio (%)" : kind == 7 ? "Niveles por canal" : "Radio (px)",
          kind == 0 || kind == 7 ? 2 : 1,
          kind == 0 || kind == 7 ? 16 : kind == 2 ? 128 : kind == 3 ? 100 : kind == 4 ? 3 : 8,
          values[1],
          n -> values[1] = n);
    if (kind == 0 || kind == 6) slider(f, "Dirección (°)", -180, 180, 0, n -> values[2] = n);
    if (kind == 2) slider(f, "Semilla", 0, 10000, 0, n -> values[2] = n);
    if (kind == 4) slider(f, "Tolerancia RGB", 0, 255, 60, n -> values[2] = n);
    show(
        "Filtro",
        f,
        () ->
            activity.runColorOperation(
                () -> drawing.utility(kind, values[0], values[1], values[2])));
  }

  void openObjectMenu() {
    new AlertDialog.Builder(activity)
        .setTitle("Objetos sobre fondo transparente")
        .setItems(
            new String[] {"Alinear objeto", "Suavizar borde alfa", "Contorno de objeto"},
            (d, kind) -> objectDialog(kind))
        .show();
  }

  private void objectDialog(int kind) {
    LinearLayout f = form();
    label(
        f,
        "Detecta el objeto mediante su alfa. La alineación usa el lienzo o los límites de la"
            + " selección.");
    int[] values = {kind == 0 ? 4 : 6, 20};
    Spinner position = new Spinner(activity);
    if (kind == 0) {
      position.setAdapter(
          new ArrayAdapter<String>(
              activity,
              android.R.layout.simple_spinner_dropdown_item,
              new String[] {
                "Arriba izquierda",
                "Arriba centro",
                "Arriba derecha",
                "Centro izquierda",
                "Centro",
                "Centro derecha",
                "Abajo izquierda",
                "Abajo centro",
                "Abajo derecha"
              }));
      position.setSelection(4);
      f.addView(position);
    } else slider(f, "Radio (px)", 0, 64, 6, n -> values[0] = n);
    slider(f, "Tolerancia alfa", 0, 255, 20, n -> values[1] = n);
    CheckBox option = new CheckBox(activity);
    option.setText(
        kind == 1 ? "Suavizar también el borde del lienzo" : "Contorno con alfa gradual");
    option.setChecked(kind == 2);
    if (kind != 0) f.addView(option);
    if (kind == 2) label(f, "Color del contorno: color de dibujo actual.");
    show(
        "Objeto",
        f,
        () ->
            activity.runColorOperation(
                () ->
                    drawing.objectEffect(
                        kind,
                        kind == 0 ? position.getSelectedItemPosition() : values[0],
                        values[1],
                        option.isChecked())));
  }

  private LinearLayout form() {
    LinearLayout f = new LinearLayout(activity);
    f.setOrientation(LinearLayout.VERTICAL);
    f.setPadding(20, 12, 20, 12);
    return f;
  }

  private TextView label(LinearLayout f, String value) {
    TextView t = new TextView(activity);
    t.setText(value);
    f.addView(t);
    return t;
  }

  private interface ValueChanged {
    void accept(int value);
  }

  private void slider(
      LinearLayout f, String title, int min, int max, int initial, ValueChanged update) {
    TextView t = label(f, title + ": " + initial);
    SeekBar s = new SeekBar(activity);
    s.setContentDescription(title);
    s.setMax(max - min);
    s.setProgress(initial - min);
    f.addView(s);
    s.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar b, int n, boolean user) {
            t.setText(title + ": " + (n + min));
            update.accept(n + min);
          }

          public void onStartTrackingTouch(SeekBar b) {}

          public void onStopTrackingTouch(SeekBar b) {}
        });
  }

  private void show(String title, LinearLayout f, Runnable apply) {
    ScrollView scroll = new ScrollView(activity);
    scroll.addView(f);
    new AlertDialog.Builder(activity)
        .setTitle(title)
        .setView(scroll)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Aplicar", (d, w) -> apply.run())
        .show();
  }

  private void parameters(
      String title, int kind, String[] names, int[] min, int[] max, int[] values) {
    LinearLayout f = form();
    for (int i = 0; i < names.length; i++) {
      final int index = i;
      slider(f, names[i], min[i], max[i], values[i], n -> values[index] = n);
    }
    show(title, f, () -> activity.runColorOperation(() -> drawing.colorAdjustment(kind, values)));
  }

  private void levels() {
    LinearLayout f = form();
    int[] values = new int[15];
    String[] channels = {"Rojo", "Verde", "Azul"};
    String[] names = {
      "Negro de entrada", "Blanco de entrada", "Gamma (%)", "Negro de salida", "Blanco de salida"
    };
    for (int c = 0; c < 3; c++) {
      label(f, channels[c]);
      int[] initial = {0, 255, 100, 0, 255};
      for (int j = 0; j < 5; j++) {
        final int index = c * 5 + j;
        values[index] = initial[j];
        slider(
            f, names[j], j == 2 ? 10 : 0, j == 2 ? 300 : 255, initial[j], n -> values[index] = n);
      }
    }
    ScrollView scroll = new ScrollView(activity);
    scroll.addView(f);
    AlertDialog d =
        new AlertDialog.Builder(activity)
            .setTitle("Niveles RGB independientes")
            .setView(scroll)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", null)
            .create();
    d.setOnShowListener(
        v ->
            d.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    w -> {
                      for (int c = 0; c < 3; c++)
                        if (values[c * 5] >= values[c * 5 + 1]
                            || values[c * 5 + 3] > values[c * 5 + 4]) {
                          Toast.makeText(
                                  activity,
                                  "El negro debe ser menor que el blanco de entrada; revisa también"
                                      + " la salida",
                                  Toast.LENGTH_LONG)
                              .show();
                          return;
                        }
                      d.dismiss();
                      activity.runColorOperation(() -> drawing.colorAdjustment(1, values));
                    }));
    d.show();
  }

  private void curves() {
    LinearLayout f = form();
    Spinner channel = new Spinner(activity);
    channel.setAdapter(
        new ArrayAdapter<String>(
            activity,
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {"RGB", "Rojo", "Verde", "Azul", "Luminosidad"}));
    f.addView(channel);
    label(
        f,
        "Toca para añadir un punto y arrástralo para moverlo. Mantén pulsado para eliminarlo."
            + " Entrada horizontal; salida vertical.");
    CurveView graph = new CurveView();
    f.addView(
        graph,
        new LinearLayout.LayoutParams(
            -1, (int) (260 * activity.getResources().getDisplayMetrics().density)));
    LinearLayout coordinates = form();
    android.widget.EditText input = new android.widget.EditText(activity),
        output = new android.widget.EditText(activity);
    input.setHint("Entrada (0–255)");
    output.setHint("Salida (0–255)");
    input.setContentDescription("Entrada del punto de curva");
    output.setContentDescription("Salida del punto de curva");
    input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    output.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    coordinates.addView(input);
    coordinates.addView(output);
    Button set = new Button(activity);
    set.setText("Añadir / actualizar punto");
    coordinates.addView(set);
    set.setOnClickListener(
        v -> {
          try {
            int x = Integer.parseInt(input.getText().toString()),
                y = Integer.parseInt(output.getText().toString());
            if (x < 0
                || x > 255
                || y < 0
                || y > 255
                || (!graph.curve.points.containsKey(x) && graph.curve.points.size() >= 32))
              throw new NumberFormatException();
            graph.curve.set(x, y);
            graph.invalidate();
          } catch (NumberFormatException e) {
            Toast.makeText(activity, "Usa valores 0–255; máximo 32 puntos", Toast.LENGTH_SHORT)
                .show();
          }
        });
    Button remove = new Button(activity);
    remove.setText("Eliminar punto de entrada");
    coordinates.addView(remove);
    remove.setOnClickListener(
        v -> {
          try {
            graph.curve.remove(Integer.parseInt(input.getText().toString()));
            graph.invalidate();
          } catch (NumberFormatException e) {
            Toast.makeText(activity, "Introduce la entrada del punto", Toast.LENGTH_SHORT).show();
          }
        });
    f.addView(coordinates);
    Button reset = new Button(activity);
    reset.setText("Restablecer curva");
    f.addView(reset);
    reset.setOnClickListener(
        v -> {
          graph.curve.reset();
          graph.invalidate();
        });
    show(
        "Curvas",
        f,
        () -> {
          int[] values = java.util.Arrays.copyOf(graph.curve.table(), 257);
          values[256] = channel.getSelectedItemPosition();
          activity.runColorOperation(() -> drawing.colorAdjustment(0, values));
        });
  }

  private final class CurveView extends View {
    int selected = -1;
    float startX, startY;
    long downTime;
    final ToneCurve curve = new ToneCurve();
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    CurveView() {
      super(activity);
      setContentDescription(
          "Editor de curva tonal: entrada de izquierda a derecha y salida de abajo arriba");
    }

    protected void onDraw(Canvas canvas) {
      canvas.drawColor(0xff202832);
      paint.setColor(0xff657080);
      paint.setStrokeWidth(1);
      for (int i = 0; i <= 4; i++) {
        float x = i * getWidth() / 4f, y = i * getHeight() / 4f;
        canvas.drawLine(x, 0, x, getHeight(), paint);
        canvas.drawLine(0, y, getWidth(), y, paint);
      }
      int[] table = curve.table();
      paint.setColor(0xff63d6b3);
      paint.setStrokeWidth(3);
      for (int i = 1; i < 256; i++)
        canvas.drawLine(
            (i - 1) * getWidth() / 255f,
            getHeight() - table[i - 1] * getHeight() / 255f,
            i * getWidth() / 255f,
            getHeight() - table[i] * getHeight() / 255f,
            paint);
      for (java.util.Map.Entry<Integer, Integer> p : curve.points.entrySet())
        canvas.drawCircle(
            p.getKey() * getWidth() / 255f,
            getHeight() - p.getValue() * getHeight() / 255f,
            6,
            paint);
    }

    public boolean onTouchEvent(MotionEvent event) {
      int x = Math.max(0, Math.min(255, Math.round(event.getX() * 255 / getWidth()))),
          y =
              Math.max(
                  0, Math.min(255, Math.round((getHeight() - event.getY()) * 255 / getHeight())));
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        selected = -1;
        float nearest = 24 * activity.getResources().getDisplayMetrics().density;
        for (java.util.Map.Entry<Integer, Integer> p : curve.points.entrySet()) {
          float distance =
              (float)
                  Math.hypot(
                      event.getX() - p.getKey() * getWidth() / 255f,
                      event.getY() - (getHeight() - p.getValue() * getHeight() / 255f));
          if (distance < nearest) {
            nearest = distance;
            selected = p.getKey();
          }
        }
        if (selected < 0) {
          if (curve.points.size() >= 32) return true;
          selected = x;
          curve.set(x, y);
        }
        startX = event.getX();
        startY = event.getY();
        downTime = event.getEventTime();
        invalidate();
        getParent().requestDisallowInterceptTouchEvent(true);
        return true;
      }
      if (event.getAction() == MotionEvent.ACTION_MOVE && selected >= 0) {
        int next = selected;
        if (selected != 0 && selected != 255) {
          Integer lower = curve.points.lowerKey(selected), upper = curve.points.higherKey(selected);
          next = Math.max(lower + 1, Math.min(upper - 1, x));
          curve.remove(selected);
        }
        curve.set(next, y);
        selected = next;
        invalidate();
        return true;
      }
      if (event.getAction() == MotionEvent.ACTION_UP
          || event.getAction() == MotionEvent.ACTION_CANCEL) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
          if (selected >= 0
              && event.getEventTime() - downTime > 600
              && Math.hypot(event.getX() - startX, event.getY() - startY) < 12) {
            curve.remove(selected);
            invalidate();
          }
          performClick();
        }
        selected = -1;
        getParent().requestDisallowInterceptTouchEvent(false);
        return true;
      }
      return true;
    }

    public boolean performClick() {
      super.performClick();
      return true;
    }
  }
}
