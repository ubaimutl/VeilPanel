package com.ubai.veilpanel.ui;
import static org.junit.Assert.*;
import android.content.Context;
import android.net.Uri;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class CustomFontTest {
 private Context context;
 @Before public void setup(){context=RuntimeEnvironment.getApplication();CustomFont.clear(context);context.getSharedPreferences("veil_panel",0).edit().clear().commit();}
 @After public void cleanup(){CustomFont.clear(context);}
 @Test public void validFontIsPrivateAndSurvivesDeletingSource()throws Exception{
  File source=new File(context.getCacheDir(),"font.ttf");
  try(InputStream in=context.getAssets().open("fonts/josefin_sans.ttf");OutputStream out=new FileOutputStream(source)){in.transferTo(out);}
  CustomFont.importFont(context,Uri.fromFile(source));assertTrue(source.delete());
  assertNotNull(CustomFont.load(context));assertEquals("custom",context.getSharedPreferences("veil_panel",0).getString("font_family",null));
  File invalid=new File(context.getCacheDir(),"invalid.ttf");try(OutputStream out=new FileOutputStream(invalid)){out.write("not a font".getBytes());}
  try{CustomFont.importFont(context,Uri.fromFile(invalid));fail("Invalid font accepted");}catch(IOException expected){}
  assertNotNull(CustomFont.load(context));assertEquals("custom",context.getSharedPreferences("veil_panel",0).getString("font_family",null));
  CustomFont.clear(context);assertNull(CustomFont.load(context));assertEquals("system",context.getSharedPreferences("veil_panel",0).getString("font_family",null));
 }
 @Test public void missingCustomFontFallsBackSafely(){context.getSharedPreferences("veil_panel",0).edit().putString("font_family","custom").commit();assertNotNull(PanelStyle.font(context));}
}
