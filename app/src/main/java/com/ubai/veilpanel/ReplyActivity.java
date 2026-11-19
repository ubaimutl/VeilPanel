package com.ubai.veilpanel;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.ubai.veilpanel.ui.*;
import com.ubai.veilpanel.system.NotificationActionExecutor;

/** Private, dialog-sized reply composer. Draft text never enters saved state. */
public final class ReplyActivity extends Activity {
    private EditText input;
    private TextView error;
    private Button send;
    private Notification.Action action;
    private boolean sending;
    private int surface,primary,secondary,accent;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE|WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setDimAmount(.36f);
        setFinishOnTouchOutside(false);
        action=getIntent().getParcelableExtra("action",Notification.Action.class);
        if(!NotificationActionExecutor.supportsReply(action)||locked()){finish();return;}
        surface=PanOSPTheme.notificationBackground(this);
        primary=PanOSPTheme.notificationPrimaryText(this);
        secondary=PanOSPTheme.notificationSecondaryText(this);
        accent=PanOSPTheme.accent(this)|0xff000000;

        android.content.SharedPreferences prefs=getSharedPreferences("veil_panel",0);
        int spacing=Math.max(8,Math.min(24,prefs.getInt("reply_spacing",12)));
        ScrollView scroll=new ScrollView(this);scroll.setTag("reply_sheet");
        scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setBackground(NotificationCardSurface.opaque(this));scroll.setOutlineProvider(new android.view.ViewOutlineProvider(){
            @Override public void getOutline(View view,android.graphics.Outline outline){outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),NotificationCardSurface.radius(ReplyActivity.this));}
        });scroll.setClipToOutline(true);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(spacing),dp(spacing),dp(spacing),dp(8));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        String packageName=getIntent().getStringExtra("package");
        if(prefs.getBoolean("reply_show_app_icon",false)&&packageName!=null)try{
            ImageView icon=new ImageView(this);icon.setImageDrawable(getPackageManager().getApplicationIcon(packageName));
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);icon.setBackground(shape(surface,10));icon.setClipToOutline(true);
            LinearLayout.LayoutParams placement=new LinearLayout.LayoutParams(dp(40),dp(40));placement.setMarginEnd(dp(12));header.addView(icon,placement);
        }catch(android.content.pm.PackageManager.NameNotFoundException|RuntimeException unavailable){}
        LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);
        CharSequence app=getIntent().getCharSequenceExtra("app");
        if(!TextUtils.isEmpty(app)){TextView appLabel=label(app,13,secondary);appLabel.setMaxLines(1);appLabel.setEllipsize(TextUtils.TruncateAt.END);labels.addView(appLabel);}
        CharSequence conversation=getIntent().getCharSequenceExtra("conversation");
        TextView title=label(TextUtils.isEmpty(conversation)?getString(R.string.veil_reply_title):conversation,20,primary);
        title.setTag("reply_title");title.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);title.setAccessibilityHeading(true);
        labels.addView(title);header.addView(labels,new LinearLayout.LayoutParams(0,-2,1));body.addView(header,new LinearLayout.LayoutParams(-1,-2));

        CharSequence preview=getIntent().getCharSequenceExtra("message");
        if(prefs.getBoolean("reply_show_preview",true)&&!TextUtils.isEmpty(preview)){
            TextView message=label(preview,14,secondary);message.setTag("reply_preview");message.setMaxLines(3);message.setEllipsize(TextUtils.TruncateAt.END);
            message.setPadding(0,dp(4),0,0);
            LinearLayout.LayoutParams placement=new LinearLayout.LayoutParams(-1,-2);placement.topMargin=dp(8);body.addView(message,placement);
        }
        input=new EditText(this);input.setTag("reply_input");input.setHint(R.string.veil_reply_hint);input.setTextSize(Math.max(12,Math.min(22,prefs.getInt("reply_text_size",16))));
        input.setMinLines(2);input.setMaxLines(5);input.setGravity(Gravity.TOP|Gravity.START);
        input.setPadding(dp(16),dp(14),dp(16),dp(14));input.setTextColor(primary);input.setHintTextColor(secondary);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND|EditorInfo.IME_FLAG_NO_EXTRACT_UI|EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        input.setSaveEnabled(false);input.setHighlightColor(PanOSPTheme.blend(surface,accent,.35f));input.setBackground(shape(PanOSPTheme.blend(surface,primary,.07f),Math.min(12,com.ubai.veilpanel.config.PanelAppearance.get(this,"card_radius"))));input.setBackgroundTintList(null);
        LinearLayout.LayoutParams editorPlacement=new LinearLayout.LayoutParams(-1,-2);editorPlacement.topMargin=dp(16);body.addView(input,editorPlacement);
        error=label(getString(R.string.veil_reply_failed),14,Color.luminance(surface)>.5f?0xffa92323:0xffffb4ab);
        error.setTag("reply_error");error.setVisibility(View.GONE);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);error.setPadding(dp(4),dp(8),dp(4),0);body.addView(error,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout buttons=new LinearLayout(this);buttons.setGravity(Gravity.CENTER_VERTICAL);
        Button cancel=button(getString(android.R.string.cancel),false);cancel.setTag("reply_cancel");cancel.setOnClickListener(v->finish());cancel.setVisibility(prefs.getBoolean("reply_show_cancel",true)?View.VISIBLE:View.GONE);buttons.addView(cancel,new LinearLayout.LayoutParams(-2,dp(48)));
        buttons.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
        boolean filled="filled".equals(prefs.getString("reply_send_style","text"));
        send=button(getString(R.string.veil_send),filled);if(!filled)send.setTextColor(accent);send.setTag("reply_send");send.setOnClickListener(v->sendReply());buttons.addView(send,new LinearLayout.LayoutParams(-2,dp(48)));
        LinearLayout.LayoutParams actionsPlacement=new LinearLayout.LayoutParams(-1,-2);actionsPlacement.topMargin=dp(12);body.addView(buttons,actionsPlacement);
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){error.setVisibility(View.GONE);updateSend();}public void afterTextChanged(Editable value){}});
        input.setOnEditorActionListener((view,id,event)->{if(id==EditorInfo.IME_ACTION_SEND){sendReply();return true;}return false;});
        PanelStyle.apply(body);setContentView(scroll);updateSend();resizeSheet();
        input.requestFocus();getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView label(CharSequence text,int size,int color){TextView label=new TextView(this);label.setText(text);label.setTextSize(size);label.setTextColor(color);label.setSaveEnabled(false);return label;}
    private GradientDrawable shape(int color,int radius){
        GradientDrawable shape=new GradientDrawable();shape.setColor(color);shape.setCornerRadius(dp(radius));
        if(color!=Color.TRANSPARENT)shape.setStroke(dp(1),PanOSPTheme.blend(color,primary,.13f));return shape;
    }
    private Button button(String text,boolean filled){
        Button button=new Button(this);button.setText(text);button.setTextSize(16);button.setAllCaps(false);button.setPadding(dp(22),0,dp(22),0);button.setMinWidth(dp(88));button.setMinimumWidth(dp(88));button.setMinimumHeight(dp(48));button.setStateListAnimator(null);
        int foreground=filled?(androidx.core.graphics.ColorUtils.calculateContrast(Color.WHITE,accent)>androidx.core.graphics.ColorUtils.calculateContrast(Color.BLACK,accent)?Color.WHITE:Color.BLACK):primary;
        button.setTextColor(foreground);button.setBackground(new RippleDrawable(ColorStateList.valueOf(PanOSPTheme.blend(filled?accent:surface,foreground,.18f)),shape(filled?accent:Color.TRANSPARENT,24),shape(Color.WHITE,24)));button.setBackgroundTintList(null);return button;
    }
    private boolean hasText(){return input!=null&&input.getText().toString().codePoints().anyMatch(c->!Character.isWhitespace(c)&&!Character.isSpaceChar(c));}
    private void updateSend(){if(send!=null){send.setEnabled(!sending&&hasText());send.setAlpha(send.isEnabled()?1:.45f);}}
    private void sendReply(){
        if(sending||!hasText())return;
        if(locked()){finish();return;}
        sending=true;updateSend();
        NotificationActionExecutor.Result result=new NotificationActionExecutor(this).reply(action,input.getText().toString());
        if(result==NotificationActionExecutor.Result.SENT){input.setText("");finish();}
        else {sending=false;updateSend();error.setVisibility(View.VISIBLE);}
    }
    private void resizeSheet(){
        int width=getWindowManager().getCurrentWindowMetrics().getBounds().width();
        getWindow().setLayout(Math.min(dp(560),NotificationCardSurface.width(this,width)),WindowManager.LayoutParams.WRAP_CONTENT);
        getWindow().setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        WindowManager.LayoutParams params=getWindow().getAttributes();params.y=dp(12);getWindow().setAttributes(params);
    }
    private boolean locked(){KeyguardManager keyguard=getSystemService(KeyguardManager.class);return keyguard==null||keyguard.isDeviceLocked();}
    @Override protected void onResume(){super.onResume();if(locked())finish();}
    @Override protected void onDestroy(){if(input!=null)input.setText("");action=null;super.onDestroy();}
}
