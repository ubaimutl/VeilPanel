package com.ubai.veilpanel.ui;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import com.ubai.veilpanel.R;
/** Independent, remembered accordion section. Collapsing never changes panel preferences. */
public final class SettingsSection extends LinearLayout {
 private final String key;private final LinearLayout body,header;private final ImageView chevron;private boolean expanded;
 public SettingsSection(Context context,String key,String title,String summary,boolean initial){
  super(context);this.key=key;setOrientation(VERTICAL);setTag("section:"+key);
  float density=getResources().getDisplayMetrics().density;
  GradientDrawable surface=new GradientDrawable();int background=PanOSPTheme.panelBackground(context);surface.setColor(PanOSPTheme.blend(background,PanOSPTheme.primaryText(context),.045f));surface.setCornerRadius(dp(16));surface.setStroke(Math.max(1,Math.round(density*.5f)),0x1A888888);setBackground(surface);
  header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(16),dp(16),dp(16),dp(16));header.setMinimumHeight(dp(72));header.setClickable(true);header.setFocusable(true);
  LinearLayout labels=new LinearLayout(context);labels.setOrientation(VERTICAL);
  TextView heading=new TextView(context);heading.setText(title);heading.setTextSize(17);heading.setTextColor(PanOSPTheme.primaryText(context));heading.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));labels.addView(heading);
  TextView detail=new TextView(context);detail.setText(summary);detail.setTextSize(13);detail.setTextColor(PanOSPTheme.secondaryText(context));detail.setPadding(0,dp(4),0,0);labels.addView(detail);
  header.addView(labels,new LayoutParams(0,-2,1));chevron=new ImageView(context);chevron.setImageResource(R.drawable.ic_panel_down);chevron.setColorFilter(PanOSPTheme.secondaryText(context));chevron.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);LayoutParams arrow=new LayoutParams(dp(20),dp(20));arrow.setMarginStart(dp(12));header.addView(chevron,arrow);addView(header,new LayoutParams(-1,-2));
  body=new LinearLayout(context);body.setOrientation(VERTICAL);body.setPadding(dp(16),0,dp(16),dp(16));addView(body,new LayoutParams(-1,-2));
  header.setContentDescription(title+". "+summary);header.setOnClickListener(v->setExpanded(!expanded,true));header.setAccessibilityDelegate(new View.AccessibilityDelegate(){
   @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(Button.class.getName());info.addAction(expanded?AccessibilityNodeInfo.AccessibilityAction.ACTION_COLLAPSE:AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);}
   @Override public boolean performAccessibilityAction(View host,int action,android.os.Bundle args){if(action==AccessibilityNodeInfo.ACTION_EXPAND||action==AccessibilityNodeInfo.ACTION_COLLAPSE){setExpanded(action==AccessibilityNodeInfo.ACTION_EXPAND,true);return true;}return super.performAccessibilityAction(host,action,args);}
  });
  setExpanded(context.getSharedPreferences("panel_settings_ui",0).getBoolean(key,initial),false);
 }
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 public LinearLayout content(){return body;}
 public boolean isExpanded(){return expanded;}
 public void setExpanded(boolean value,boolean remember){expanded=value;body.setVisibility(value?VISIBLE:GONE);header.setStateDescription(value?"Expanded":"Collapsed");chevron.setRotation(value?180:0);if(remember)getContext().getSharedPreferences("panel_settings_ui",0).edit().putBoolean(key,value).apply();}
}
