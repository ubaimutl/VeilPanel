package com.ubai.veilpanel.ui;
import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
/** Settings dialogs share the dedicated settings typography, including text inputs. */
public final class SettingsDialogBuilder extends MaterialAlertDialogBuilder {
 public SettingsDialogBuilder(Context context){super(context);}
 @Override public AlertDialog create(){
  AlertDialog dialog=super.create();dialog.setOnShowListener(ignored->{if(dialog.getWindow()!=null)SettingsTypography.apply(dialog.getWindow().getDecorView());});return dialog;
 }
}
