package com.ubai.veilpanel.config;

import android.content.*;
import android.graphics.*;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.json.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PanelSettingsArchiveTest {
 private Context context;
 @Before public void setup(){context=RuntimeEnvironment.getApplication();for(String name:new String[]{"veil_panel","panosp_preferences","panel_settings_ui"})context.getSharedPreferences(name,0).edit().clear().commit();}
 @Test public void portableRoundTripPreservesTypesAndAllSettingsGroups()throws Exception {
  var prefs=context.getSharedPreferences("veil_panel",0);prefs.edit().putInt("blur",7).putBoolean("show_status_bar",true).putFloat("sample_float",1.25f).putLong("sample_long",Long.MAX_VALUE).putString("font_family","system").putStringSet("apps",new HashSet<>(Arrays.asList("one","اثنان"))).commit();
  context.getSharedPreferences("panosp_preferences",0).edit().putInt("theme_accent_color",0xff123456).commit();
  context.getSharedPreferences("panel_settings_ui",0).edit().putBoolean("advanced",true).commit();
  byte[] archive=backup();prefs.edit().clear().putInt("blur",60).putBoolean("extra",true).commit();
  restore(archive);assertEquals(7,prefs.getInt("blur",0));assertFalse(prefs.contains("extra"));assertEquals(1.25f,prefs.getFloat("sample_float",0),0);assertEquals(Long.MAX_VALUE,prefs.getLong("sample_long",0));assertTrue(prefs.getBoolean("show_status_bar",false));assertEquals(new HashSet<>(Arrays.asList("one","اثنان")),prefs.getStringSet("apps",null));assertEquals(0xff123456,context.getSharedPreferences("panosp_preferences",0).getInt("theme_accent_color",0));assertTrue(context.getSharedPreferences("panel_settings_ui",0).getBoolean("advanced",false));
 }
 @Test public void wallpaperAndManagedBackgroundAreEmbeddedAndPathsAreRecreated()throws Exception {
  image("panel-wallpaper-source.png",Bitmap.CompressFormat.PNG);image("power-saver-backdrop.png",Bitmap.CompressFormat.PNG);
  File directory=new File(context.getFilesDir(),"background");directory.mkdirs();File background=new File(directory,"original.webp");writeImage(background,Bitmap.CompressFormat.WEBP_LOSSY);
  context.getSharedPreferences("panosp_preferences",0).edit().putString("panel_background_image_uri",Uri.fromFile(background).toString()).commit();
  byte[] archive=backup();background.delete();new File(context.getFilesDir(),"panel-wallpaper-source.png").delete();
  restore(archive);assertTrue(new File(context.getFilesDir(),"panel-wallpaper-source.png").isFile());String uri=context.getSharedPreferences("panosp_preferences",0).getString("panel_background_image_uri",null);assertNotNull(uri);assertNotEquals(Uri.fromFile(background).toString(),uri);assertTrue(new File(Uri.parse(uri).getPath()).isFile());
 }
 @Test public void customFontFileAndSelectionSurviveTransfer()throws Exception {
  File font=new File(context.getFilesDir(),"panel-custom-font");
  try(InputStream in=context.getAssets().open("fonts/josefin_sans.ttf");OutputStream out=new FileOutputStream(font)){in.transferTo(out);}
  context.getSharedPreferences("veil_panel",0).edit().putString("font_family","custom").commit();
  byte[] original=java.nio.file.Files.readAllBytes(font.toPath()), archive=backup();font.delete();restore(archive);
  assertArrayEquals(original,java.nio.file.Files.readAllBytes(font.toPath()));assertEquals("custom",context.getSharedPreferences("veil_panel",0).getString("font_family",null));
 }
 @Test public void unexpectedPathsAreRejectedWithoutChangingPreferences()throws Exception {
  context.getSharedPreferences("veil_panel",0).edit().putInt("blur",9).commit();Map<String,byte[]> entries=entries(backup());entries.put("../escape",new byte[]{1});assertRejected(zip(entries));assertEquals(9,context.getSharedPreferences("veil_panel",0).getInt("blur",0));assertFalse(new File(context.getCacheDir(),"escape").exists());
 }
 @Test public void missingDeclaredAssetRetainsExistingWallpaperAndSettings()throws Exception {
  image("power-saver-backdrop.png",Bitmap.CompressFormat.PNG);Map<String,byte[]> entries=entries(backup());entries.remove("power-saver-backdrop.png");byte[] previous=java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"power-saver-backdrop.png").toPath());assertRejected(zip(entries));assertArrayEquals(previous,java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"power-saver-backdrop.png").toPath()));
 }
 @Test public void invalidTypedValueIsRejectedBeforeAnySettingsArePublished()throws Exception {
  context.getSharedPreferences("veil_panel",0).edit().putInt("blur",9).commit();Map<String,byte[]> entries=entries(backup());JSONObject json=new JSONObject(new String(entries.get("settings.json"),StandardCharsets.UTF_8));json.getJSONObject("preferences").getJSONObject("group0").getJSONObject("blur").put("value",1.5);entries.put("settings.json",json.toString().getBytes(StandardCharsets.UTF_8));assertRejected(zip(entries));assertEquals(9,context.getSharedPreferences("veil_panel",0).getInt("blur",0));
 }
 @Test public void truncatedArchiveDoesNotChangeSettings()throws Exception {context.getSharedPreferences("veil_panel",0).edit().putInt("blur",9).commit();byte[] full=backup();assertRejected(Arrays.copyOf(full,full.length/2));assertEquals(9,context.getSharedPreferences("veil_panel",0).getInt("blur",0));}
 private void assertRejected(byte[] archive)throws Exception{try{restore(archive);fail("Invalid backup accepted");}catch(IOException|JSONException expected){}}
 private byte[] backup()throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();PanelSettingsArchive.write(context,out);return out.toByteArray();}
 private void restore(byte[] bytes)throws Exception{PanelSettingsArchive.restore(context,new ByteArrayInputStream(bytes));}
 private void image(String name,Bitmap.CompressFormat format)throws Exception{writeImage(new File(context.getFilesDir(),name),format);}
 private void writeImage(File file,Bitmap.CompressFormat format)throws Exception{Bitmap image=Bitmap.createBitmap(16,24,Bitmap.Config.ARGB_8888);image.eraseColor(Color.DKGRAY);try(OutputStream out=new FileOutputStream(file)){assertTrue(image.compress(format,95,out));}image.recycle();}
 private Map<String,byte[]> entries(byte[] archive)throws Exception{Map<String,byte[]> result=new LinkedHashMap<>();try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(archive))){ZipEntry entry;while((entry=zip.getNextEntry())!=null)result.put(entry.getName(),zip.readAllBytes());}return result;}
 private byte[] zip(Map<String,byte[]> entries)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(out)){for(var entry:entries.entrySet()){zip.putNextEntry(new ZipEntry(entry.getKey()));zip.write(entry.getValue());zip.closeEntry();}}return out.toByteArray();}
}
