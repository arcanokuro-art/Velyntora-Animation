package art.velyntora.core;
import android.app.AlertDialog;
import android.text.*;
import android.view.WindowManager;
import android.widget.*;

/** Local typography draft with visual controls. Applying changes only the pending object. */
final class TextDialog {
 final MainActivity host;
 TextDialog(MainActivity host){this.host=host;}
 void configureText(int x,int y){if(host.readProjectProgress()!=null || host.readDrawing().placedImage().active())return;show(new TextSpec("Texto",32,host.readDrawing().readColor(),0,0,Math.max(8,Math.min(6000,host.readDrawing().documentWidth()-x))),x,y,false);}
 static AlertDialog edit(MainActivity host){PlacedImageSession s=host.readDrawing().placedImage();if(s.active() && s.text!=null && host.readProjectProgress()==null)return new TextDialog(host).show(TextSpec.effective(s.text,s.state),0,0,true);return null;}
 AlertDialog show(TextSpec initial,int x,int y,boolean editing){
  LinearLayout form=new LinearLayout(host);form.setOrientation(1);form.setPadding(host.dp(16),host.dp(8),host.dp(16),host.dp(12));
  EditText input=new EditText(host);input.setContentDescription("Contenido del texto");input.setHint("Escribe tu texto");input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);input.setMinLines(2);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4096)});input.setText(editing?initial.content:"");form.addView(input);
  TextDraftPreview preview=new TextDraftPreview(host);form.addView(preview,new LinearLayout.LayoutParams(-1,host.dp(100)));
  TextSlider size=new TextSlider(host,form,"Tamaño del texto en píxeles",4,6000,initial.size,true);
  TextSlider wrap=new TextSlider(host,form,"Ancho máximo del texto en píxeles",8,6000,initial.wrapWidth,false);
  Spinner family=new Spinner(host);family.setContentDescription("Fuente del texto");family.setAdapter(new ArrayAdapter<>(host,android.R.layout.simple_spinner_dropdown_item,new String[]{"Sans serif","Serif","Monoespaciada"}));family.setSelection(initial.family);form.addView(family,new LinearLayout.LayoutParams(-1,host.dp(48)));
  LinearLayout styles=host.row();CheckBox bold=new CheckBox(host),italic=new CheckBox(host);bold.setText("Negrita");bold.setChecked((initial.style&1)!=0);italic.setText("Cursiva");italic.setChecked((initial.style&2)!=0);styles.addView(bold);LinearLayout.LayoutParams italicSpace=new LinearLayout.LayoutParams(-2,host.dp(48));italicSpace.setMargins(host.dp(20),0,0,0);styles.addView(italic,italicSpace);form.addView(styles);
  int[] color={initial.color};Button choose=new Button(host);choose.setAllCaps(false);choose.setText("Color del texto");choose.setContentDescription("Elegir color del texto");form.addView(choose,new LinearLayout.LayoutParams(-1,host.dp(48)));
  Runnable refresh=()->{String content=input.getText().toString();if(content.trim().isEmpty())content="Texto";try{preview.update(new TextSpec(content,size.value(),color[0],Math.max(0,family.getSelectedItemPosition()),(bold.isChecked()?1:0)|(italic.isChecked()?2:0),wrap.value()));}catch(IllegalArgumentException ignored){}android.graphics.drawable.Drawable swatch=new ColorSwatchDrawable(color[0]);swatch.setBounds(0,0,host.dp(28),host.dp(28));choose.setCompoundDrawables(swatch,null,null,null);};
  choose.setOnClickListener(v->ColorPickerDialog.showSingle(host,color[0],selected->{color[0]=selected;refresh.run();}));
  size.listen(refresh);wrap.listen(refresh);bold.setOnCheckedChangeListener((b,c)->refresh.run());italic.setOnCheckedChangeListener((b,c)->refresh.run());family.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,android.view.View v,int pos,long id){refresh.run();}});
  input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){refresh.run();}public void afterTextChanged(Editable e){}});refresh.run();
  AlertDialog dialog=new AlertDialog.Builder(host).setTitle(editing?"Editar texto":"Añadir texto").setView(host.scrollForm(form)).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",null).create();dialog.show();dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  dialog.getButton(-1).setOnClickListener(v->{TextSpec next;try{next=new TextSpec(input.getText().toString(),size.value(),color[0],family.getSelectedItemPosition(),(bold.isChecked()?1:0)|(italic.isChecked()?2:0),wrap.value());}catch(IllegalArgumentException error){input.setError("Escribe de 1 a 4096 caracteres visibles");return;}if(new TextGlyphLayout(next).height>6000){input.setError("Reduce el tamaño o los saltos de línea: el texto supera 6000 px de alto");return;}dialog.dismiss();host.readDrawing().placedImage().applyText(next,x,y,editing);});return dialog;
 }
}
