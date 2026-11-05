package com.ubai.veilpanel;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.Gravity;
import android.widget.*;
import com.ubai.veilpanel.ui.*;
import com.ubai.veilpanel.system.NotificationActionExecutor;
/** An explicit reply screen; no drafts are persisted or exposed on the lock screen. */
public final class ReplyActivity extends Activity {
    private EditText input;
    private Notification.Action action;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        action = getIntent().getParcelableExtra("action", Notification.Action.class);
        if (!NotificationActionExecutor.supportsReply(action)) { finish(); return; }
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(24*getResources().getDisplayMetrics().density);
        root.setPadding(pad,pad,pad,pad); root.setBackgroundColor(PanOSPTheme.panelBackground(this));
        TextView title = new TextView(this); title.setText(getIntent().getStringExtra("app"));
        title.setTextSize(24); title.setTextColor(PanOSPTheme.primaryText(this)); root.addView(title);
        input = new EditText(this); input.setHint(R.string.veil_reply_hint); input.setMinLines(3); input.setSaveEnabled(false);
        input.setTextColor(PanOSPTheme.primaryText(this)); input.setHintTextColor(PanOSPTheme.secondaryText(this));
        root.addView(input, new LinearLayout.LayoutParams(-1,-2));
        LinearLayout buttons = new LinearLayout(this); buttons.setGravity(Gravity.END);
        Button cancel = new Button(this); cancel.setText(android.R.string.cancel); cancel.setOnClickListener(v->finish()); buttons.addView(cancel);
        Button send = new Button(this); send.setText(R.string.veil_send); buttons.addView(send);
        send.setOnClickListener(v->{
            if (input.getText().toString().trim().isEmpty()) return;
            NotificationActionExecutor.Result result = new NotificationActionExecutor(this).reply(action,input.getText().toString());
            if (result == NotificationActionExecutor.Result.SENT) { input.setText(""); finish(); }
            else Toast.makeText(this,R.string.veil_reply_failed,Toast.LENGTH_SHORT).show();
        });
        root.addView(buttons); PanelStyle.apply(root); setContentView(root);
        input.requestFocus(); getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    @Override protected void onResume() {
        super.onResume(); KeyguardManager keyguard=getSystemService(KeyguardManager.class);
        if (keyguard == null || keyguard.isDeviceLocked()) finish();
    }
}
