package art.velyntora.core;

/** Figure catalog keeps all existing IDs and controllers stable. */
final class FigurasModule {
  static final int[] SIN_RELLENO={DrawingView.RECTANGLE,DrawingView.ROUNDED_RECTANGLE,DrawingView.ELLIPSE,DrawingView.TRIANGLE,DrawingView.CIRCLE};
  static final int[] CON_RELLENO={DrawingView.FILLED_RECTANGLE,DrawingView.FILLED_ELLIPSE,DrawingView.FILLED_ROUNDED_RECTANGLE,DrawingView.FILLED_TRIANGLE};
  static boolean contains(int id){for(int tool:SIN_RELLENO)if(id==tool)return true;for(int tool:CON_RELLENO)if(id==tool)return true;return false;}
  static String label(int id){
    return switch(id){
      case DrawingView.RECTANGLE -> "Rectángulo";
      case DrawingView.ROUNDED_RECTANGLE -> "Rectángulo redondeado";
      case DrawingView.ELLIPSE -> "Elipse";
      case DrawingView.TRIANGLE -> "Triángulo";
      case DrawingView.CIRCLE -> "Círculo";
      case DrawingView.FILLED_RECTANGLE -> "Rectángulo relleno";
      case DrawingView.FILLED_ELLIPSE -> "Elipse rellena";
      case DrawingView.FILLED_ROUNDED_RECTANGLE -> "Rectángulo redondeado relleno";
      case DrawingView.FILLED_TRIANGLE -> "Triángulo relleno";
      default -> throw new IllegalArgumentException("Unknown figure: "+id);
    };
  }
  static int selected(DrawingView view){
    if(contains(view.currentTool()))return view.currentTool();
    int id=view.getContext().getSharedPreferences("figure-selector",0).getInt("selected",DrawingView.RECTANGLE);
    return contains(id)?id:DrawingView.RECTANGLE;
  }
  static void select(DrawingView view,int id){
    if(!contains(id))throw new IllegalArgumentException("Unknown figure");
    view.setTool(id);
    view.getContext().getSharedPreferences("figure-selector",0).edit().putInt("selected",id).apply();
  }
  private FigurasModule(){}
}
