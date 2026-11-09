package com.ubai.veilpanel.ui;

import android.content.Context;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.function.Consumer;

/** Material settings index and focused category pages, with search across every category. */
public final class SettingsNavigator {
 private final Context context;
 private final ScrollView scroll;
 private final EditText search;
 private final Consumer<String> onSelection;
 private final Map<String,View> pages=new LinkedHashMap<>();
 private final TextView empty;
 private final LinearLayout home;
 private String selected="home";
 public SettingsNavigator(Context context,LinearLayout toolbar,LinearLayout content,ScrollView scroll,EditText search,Consumer<String> onSelection){
  this.context=context;this.scroll=scroll;this.search=search;this.onSelection=onSelection;
  home=new LinearLayout(context);home.setTag("settings_categories");home.setOrientation(LinearLayout.VERTICAL);
  TextView title=label("Settings",34,SettingsStyle.primaryText(context));title.setPadding(dp(4),dp(20),0,dp(8));title.setTag("settings_home_title");title.setAccessibilityHeading(true);toolbar.addView(title,1);
  TextView subtitle=label("Veil Panel",16,SettingsStyle.secondaryText(context));subtitle.setTag("settings_home_subtitle");subtitle.setPadding(dp(4),0,0,dp(20));toolbar.addView(subtitle,2);
  String[] keys={"access","themes","wallpaper","header","notifications","typography","layout","liquid","gestures","handle","motion","advanced"};
  String[] labels={"Setup & access","Presets","Wallpaper & glass","Clock & header","Notifications","Typography & icons","Spacing & layout","Liquid Glass","Gestures & shortcuts","Dismiss handle","Motion","About & backup"};
  for(int i=0;i<keys.length;i++){
   String key=keys[i];View page=content.findViewWithTag("themes".equals(key)?"theme_presets":"section:"+key);if(page==null)continue;pages.put(key,page);
   if(page instanceof SettingsSection)((SettingsSection)page).setNavigationPage();
   if(i==2||i==8||i==11){TextView group=label(i==2?"Appearance":i==8?"Interaction":"App",14,SettingsStyle.accent(context));group.setPadding(dp(8),dp(28),0,dp(8));group.setAccessibilityHeading(true);home.addView(group);}
   LinearLayout row=new LinearLayout(context);row.setTag("category:"+key);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(20),dp(20),dp(20),dp(20));row.setMinimumHeight(dp(80));row.setClickable(true);row.setFocusable(true);
   LinearLayout text=new LinearLayout(context);text.setOrientation(LinearLayout.VERTICAL);text.addView(label(labels[i],16,SettingsStyle.primaryText(context)));
   String summary=page instanceof SettingsSection?((SettingsSection)page).summary():"Choose a look, then adjust it to suit you";
   TextView detail=label(summary,14,SettingsStyle.secondaryText(context));detail.setPadding(0,dp(4),0,0);text.addView(detail);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));
   TextView arrow=label("›",26,SettingsStyle.secondaryText(context));arrow.setPadding(dp(16),0,0,0);arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(arrow);
   row.setContentDescription(labels[i]+". "+summary);row.setOnClickListener(v->select(key));home.addView(row,new LinearLayout.LayoutParams(-1,-2));
  }
  SettingsStyle.groupRows(home);content.addView(home,0,new LinearLayout.LayoutParams(-1,-2));
  empty=label("No settings found. Try clock, wallpaper or gestures.",16,SettingsStyle.secondaryText(context));empty.setTag("settings_empty");empty.setPadding(dp(4),dp(24),dp(4),dp(24));empty.setVisibility(View.GONE);empty.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);content.addView(empty);
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void afterTextChanged(Editable text){}public void onTextChanged(CharSequence text,int start,int before,int count){filter(text.toString().trim().toLowerCase(Locale.ROOT));}});
  search.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){hideKeyboard();return true;}return false;});
 }
 public boolean back(){if(search.length()>0){search.setText("");hideKeyboard();return true;}if(!"home".equals(selected)){select("home");return true;}return false;}
 public void select(String key){
  selected=pages.containsKey(key)?key:"home";search.setText("");hideKeyboard();onSelection.accept(selected);filter("");
 }
 private void hideKeyboard(){((android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);search.clearFocus();home.setFocusableInTouchMode(true);home.requestFocus();}
 private void filter(String query){
  boolean atHome="home".equals(selected);View toolbar=(View)search.getParent();toolbar.findViewWithTag("settings_home_title").setVisibility(atHome?View.VISIBLE:View.GONE);toolbar.findViewWithTag("settings_home_subtitle").setVisibility(atHome?View.VISIBLE:View.GONE);home.setVisibility(atHome&&query.isEmpty()?View.VISIBLE:View.GONE);search.setVisibility(atHome?View.VISIBLE:View.GONE);int found=0;
  for(Map.Entry<String,View> entry:pages.entrySet()){
   boolean visible=query.isEmpty()?entry.getKey().equals(selected):matches(entry.getValue(),query);entry.getValue().setVisibility(visible?View.VISIBLE:View.GONE);if(visible)found++;
   View row=home.findViewWithTag("category:"+entry.getKey());row.setSelected(entry.getKey().equals(selected));
  }
  empty.setVisibility(!query.isEmpty()&&found==0?View.VISIBLE:View.GONE);scroll.scrollTo(0,0);
 }
 private boolean matches(View view,String query){
  if(view instanceof TextView&&((TextView)view).getText().toString().toLowerCase(Locale.ROOT).contains(query))return true;
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(matches(group.getChildAt(i),query))return true;}return false;
 }
 private TextView label(String value,int size,int color){TextView text=new TextView(context);text.setText(value);text.setTextSize(size);text.setTextColor(color);return text;}
 private int dp(int n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
}
