package com.ubai.veilpanel.ui;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.Gravity;
import java.util.ArrayList;
import java.util.List;
/** Compact actions wrap horizontally instead of inheriting OEM Button minimums. */
public final class NotificationActionRow extends ViewGroup {
 private final List<Rect> positions=new ArrayList<>();
 public NotificationActionRow(Context context){super(context);setClipChildren(false);}
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 public TextView addAction(CharSequence label,String description,boolean secondary,Runnable action){
  TextView button=new TextView(getContext()){@Override public CharSequence getAccessibilityClassName(){return android.widget.Button.class.getName();}};
  button.setText(label);button.setContentDescription(description);button.setTextSize(14);button.setGravity(Gravity.CENTER);button.setIncludeFontPadding(false);button.setMinWidth(dp(48));button.setMinimumHeight(dp(44));button.setMaxWidth(dp(160));button.setMaxLines(2);button.setEllipsize(android.text.TextUtils.TruncateAt.END);button.setPadding(dp(8),dp(6),dp(8),dp(6));button.setTextColor(secondary?PanOSPTheme.notificationSecondaryText(getContext()):PanOSPTheme.accent(getContext()));button.setBackgroundColor(android.graphics.Color.TRANSPARENT);button.setClickable(true);button.setFocusable(true);button.setOnClickListener(v->action.run());addView(button,new LayoutParams(-2,-2));return button;
 }
 @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(-2,-2);}
 @Override protected void onMeasure(int widthSpec,int heightSpec){
  int available=Math.max(1,MeasureSpec.getSize(widthSpec)-getPaddingLeft()-getPaddingRight());if(MeasureSpec.getMode(widthSpec)==MeasureSpec.UNSPECIFIED)available=getResources().getDisplayMetrics().widthPixels;
  int x=0,y=0,rowHeight=0,usedWidth=0,gap=dp(4);positions.clear();
  for(int i=0;i<getChildCount();i++){View child=getChildAt(i);if(child.getVisibility()==GONE){positions.add(new Rect());continue;}child.measure(MeasureSpec.makeMeasureSpec(available,MeasureSpec.AT_MOST),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));int w=child.getMeasuredWidth(),h=child.getMeasuredHeight();if(x>0&&x+w>available){y+=rowHeight+gap;x=0;rowHeight=0;}positions.add(new Rect(x,y,x+w,y+h));usedWidth=Math.max(usedWidth,x+w);x+=w+gap;rowHeight=Math.max(rowHeight,h);}
  setMeasuredDimension(resolveSize(usedWidth+getPaddingLeft()+getPaddingRight(),widthSpec),resolveSize(y+rowHeight+getPaddingTop()+getPaddingBottom(),heightSpec));
 }
 @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){boolean rtl=getLayoutDirection()==LAYOUT_DIRECTION_RTL;for(int i=0;i<getChildCount();i++){View child=getChildAt(i);if(child.getVisibility()==GONE)continue;Rect p=positions.get(i);int x=rtl?getWidth()-getPaddingRight()-p.right:getPaddingLeft()+p.left;child.layout(x,getPaddingTop()+p.top,x+p.width(),getPaddingTop()+p.bottom);}}
}
