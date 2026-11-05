package com.ubai.veilpanel;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
/** Public launcher entry point. Returns no notification data and accepts no commands. */
public final class PanelShortcutActivity extends Activity {
 public static final String ACTION_OPEN="com.ubai.veilpanel.OPEN_NOTIFICATIONS";
 public static Intent openIntent(android.content.Context context){return new Intent(context,PanelShortcutActivity.class).setAction(ACTION_OPEN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_ANIMATION);}
 @Override protected void onCreate(Bundle state){
  super.onCreate(state);
  if(Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())){
   Intent result=new Intent().putExtra(Intent.EXTRA_SHORTCUT_INTENT,openIntent(this)).putExtra(Intent.EXTRA_SHORTCUT_NAME,getString(R.string.shortcut_open_notifications)).putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,Intent.ShortcutIconResource.fromContext(this,R.drawable.ic_shortcut_notifications));
   setResult(RESULT_OK,result);finish();return;
  }
  if(!com.ubai.veilpanel.accessibility.PanOSPAccessibilityServiceCore.requestPanelOpen())Toast.makeText(this,R.string.shortcut_enable_service,Toast.LENGTH_LONG).show();
  finish();
 }
}
