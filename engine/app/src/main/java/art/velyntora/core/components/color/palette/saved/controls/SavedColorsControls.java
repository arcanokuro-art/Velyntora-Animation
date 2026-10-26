package art.velyntora.core;

import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import art.velyntora.core.components.color.palette.saved.SavedPalette;

/** Save action and user swatches in the existing horizontally scrolling palette. */
final class SavedColorsControls {
  private final MainActivity host;
  private final SavedPaletteStore store;
  private final SavedPalette palette;
  private final GridLayout grid;
  private SavedColorsControls(MainActivity host,LinearLayout row){
    this.host=host;store=new SavedPaletteStore(host);palette=store.load();
    grid=new GridLayout(host);grid.setRowCount(2);grid.setContentDescription("Colores guardados");
    grid.setPadding(host.dp(8),0,host.dp(8),0);row.addView(grid);render();
  }
  static void attach(MainActivity host,LinearLayout row,Button save){
    SavedColorsControls controls=new SavedColorsControls(host,row);
    save.setOnClickListener(v->controls.save(host.readActiveColor()));
    save.setOnLongClickListener(v->{
      PopupMenu menu=new PopupMenu(host,save);
      menu.getMenu().add(0,1,0,"Guardar color activo");
      menu.getMenu().add(0,2,1,"Guardar color secundario");
      menu.setOnMenuItemClickListener(item->{controls.save(item.getItemId()==2?host.readSecondaryColor():host.readActiveColor());return true;});
      menu.show();return true;
    });
  }
  private void save(int color){
    if(host.readProjectProgress()!=null)return;
    if(palette.contains(color)){Toast.makeText(host,"Este color ya está guardado",Toast.LENGTH_SHORT).show();return;}
    if(!palette.add(color)){Toast.makeText(host,"La paleta admite hasta 64 colores guardados",Toast.LENGTH_SHORT).show();return;}
    store.save(palette);render();Toast.makeText(host,"Color guardado",Toast.LENGTH_SHORT).show();
    grid.post(()->{if(host.readPaletteScroll()!=null)host.readPaletteScroll().smoothScrollTo(grid.getRight(),0);});
  }
  private void render(){
    grid.removeAllViews();grid.setColumnCount(Math.max(1,(palette.colors().size()+1)/2));
    int index=0;
    for(int color:palette.colors()){
      View swatch=new View(host);swatch.setBackground(new ColorSwatchDrawable(color));
      swatch.setFocusable(true);swatch.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
      String label=String.format(java.util.Locale.US,"Color guardado #%08X",color);
      swatch.setContentDescription(label);
      if(android.os.Build.VERSION.SDK_INT>=26)swatch.setTooltipText(label+". Mantener pulsado para opciones");
      GridLayout.LayoutParams cell=new GridLayout.LayoutParams(GridLayout.spec(index%2),GridLayout.spec(index/2));
      cell.width=host.dp(24);cell.height=host.dp(24);grid.addView(swatch,cell);index++;
      swatch.setOnClickListener(v->{if(host.readProjectProgress()==null)host.setActiveColor(color);});
      swatch.setOnLongClickListener(v->{
        if(host.readProjectProgress()!=null)return true;
        PopupMenu menu=new PopupMenu(host,swatch);
        menu.getMenu().add(0,1,0,"Usar como color activo");
        menu.getMenu().add(0,2,1,"Usar como color secundario");
        menu.getMenu().add(0,3,2,"Eliminar color guardado");
        menu.setOnMenuItemClickListener(item->{
          if(host.readProjectProgress()!=null)return true;
          if(item.getItemId()==1)host.setActiveColor(color);
          else if(item.getItemId()==2)host.setSecondaryColor(color);
          else if(palette.remove(color)){store.save(palette);render();}
          return true;
        });menu.show();return true;
      });
    }
    grid.setVisibility(palette.colors().isEmpty()?View.GONE:View.VISIBLE);
  }
}
