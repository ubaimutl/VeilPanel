package com.ubai.veilpanel.ui;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import com.ubai.veilpanel.R;
/** Independent, remembered accordion section. Collapsing never changes panel preferences. */
public final class SettingsSection extends LinearLayout {
 private final String key;private final LinearLayout body,header;private final ImageView chevron;private TextView heading,detail;private ImageView icon;private boolean expanded;
 public SettingsSection(Context context,String key,String title,String summary,boolean initial){
  super(context);this.key=key;setOrientation(VERTICAL);setTag("section:"+key);
  float density=getResources().getDisplayMetrics().density;
  GradientDrawable surface=new GradientDrawable();int background=SettingsStyle.panelBackground(context);surface.setColor(PanOSPTheme.blend(background,SettingsStyle.primaryText(context),.045f));surface.setCornerRadius(dp(16));surface.setStroke(Math.max(1,Math.round(density*.5f)),0x1A888888);setBackground(surface);
  header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(16),dp(18),dp(16),dp(18));header.setMinimumHeight(dp(72));header.setClickable(true);header.setFocusable(true);
  int accent=SettingsStyle.color(context,key);
  icon=new ImageView(context);icon.setImageResource(SettingsStyle.icon(key));icon.setColorFilter(accent);icon.setPadding(dp(10),dp(10),dp(10),dp(10));icon.setBackground(SettingsStyle.rounded(context,PanOSPTheme.blend(background,accent,.13f),14));icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  LayoutParams iconParams=new LayoutParams(dp(44),dp(44));iconParams.setMarginEnd(dp(14));header.addView(icon,iconParams);
  header.setBackground(SettingsStyle.touch(context,android.graphics.Color.TRANSPARENT,16));
  LinearLayout labels=new LinearLayout(context);labels.setOrientation(VERTICAL);
  heading=new TextView(context);heading.setText(title);heading.setAccessibilityHeading(true);heading.setTextSize(16);heading.setTextColor(SettingsStyle.primaryText(context));heading.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));labels.addView(heading);
  detail=new TextView(context);detail.setText(summary);detail.setTextSize(14);detail.setTextColor(SettingsStyle.secondaryText(context));detail.setPadding(0,dp(4),0,0);labels.addView(detail);
  header.addView(labels,new LayoutParams(0,-2,1));chevron=new ImageView(context);chevron.setImageResource(R.drawable.ic_panel_down);chevron.setColorFilter(SettingsStyle.secondaryText(context));chevron.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);LayoutParams arrow=new LayoutParams(dp(20),dp(20));arrow.setMarginStart(dp(12));header.addView(chevron,arrow);addView(header,new LayoutParams(-1,-2));
  body=new LinearLayout(context);body.setOrientation(VERTICAL);body.setPadding(dp(16),0,dp(16),dp(16));addView(body,new LayoutParams(-1,-2));
  header.setContentDescription(title+". "+summary);header.setOnClickListener(v->setExpanded(!expanded,true));header.setAccessibilityDelegate(new View.AccessibilityDelegate(){
   @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(Button.class.getName());info.addAction(expanded?AccessibilityNodeInfo.AccessibilityAction.ACTION_COLLAPSE:AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);}
   @Override public boolean performAccessibilityAction(View host,int action,android.os.Bundle args){if(action==AccessibilityNodeInfo.ACTION_EXPAND||action==AccessibilityNodeInfo.ACTION_COLLAPSE){setExpanded(action==AccessibilityNodeInfo.ACTION_EXPAND,true);return true;}return super.performAccessibilityAction(host,action,args);}
  });
  setExpanded(context.getSharedPreferences("panel_settings_ui",0).getBoolean(key,initial),false);
 }
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 public String title(){return heading.getText().toString();}
 public String summary(){return detail.getText().toString();}
 public void setNavigationPage(){setBackground(null);icon.setVisibility(GONE);heading.setTextSize(34);heading.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL));detail.setTextSize(16);header.setPadding(dp(4),dp(20),dp(4),dp(36));body.setPadding(0,0,0,dp(24));header.setOnClickListener(null);header.setClickable(false);header.setFocusable(false);header.setAccessibilityDelegate(null);chevron.setVisibility(GONE);setExpanded(true,false);SettingsStyle.groupRows(body);}
 public LinearLayout content(){return body;}
 public boolean isExpanded(){return expanded;}
 public void setExpanded(boolean value,boolean remember){expanded=value;body.setVisibility(value?VISIBLE:GONE);header.setStateDescription(value?"Expanded":"Collapsed");chevron.setRotation(value?180:0);if(remember)getContext().getSharedPreferences("panel_settings_ui",0).edit().putBoolean(key,value).apply();}
}
