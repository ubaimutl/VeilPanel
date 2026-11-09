package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.ubai.veilpanel.R;

/** Bundled settings font; independent of all notification/clock font preferences. */
public final class SettingsTypography {
 private static Typeface family;
 private SettingsTypography(){}
 public static Typeface font(Context context,int weight){
  if(family==null)family=context.getResources().getFont(R.font.roboto);
  return Typeface.create(family,weight,false);
 }
 public static void apply(View view){
  if(view instanceof TextView){TextView text=(TextView)view;int weight=text.getTypeface()==null?400:text.getTypeface().getWeight();text.setTypeface(font(view.getContext(),weight));}
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)apply(group.getChildAt(i));}
 }
}
