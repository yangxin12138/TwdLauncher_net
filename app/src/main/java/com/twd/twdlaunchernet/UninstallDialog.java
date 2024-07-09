package com.twd.twdlaunchernet;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * @Author:Yangxin
 * @Description:
 * @time: Create in 17:45 2024/7/4
 */
public class UninstallDialog extends Dialog implements View.OnClickListener {

    private Context context;
    private TextView titleTextView;
    String appName ;
    Drawable appIconRes;
    private ImageView appIcon;
    private TextView confirmTV;
    private TextView cancelTV;
    private OnDialogButtonClickListener onDialogButtonClickListener;


    public UninstallDialog(@NonNull Context context, String appName, Drawable appIconRes) {
        super(context);
        this.context = context;
        this.appName = appName;
        this.appIconRes = appIconRes;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uninstall_dialog_layout);

        titleTextView = findViewById(R.id.dialog_title);
        appIcon = findViewById(R.id.dialog_icon);
        confirmTV = findViewById(R.id.btn_confirm);
        cancelTV = findViewById(R.id.btn_cancel);

        titleTextView.setText(context.getString(R.string.application_isuninstall)+appName);
        appIcon.setImageDrawable(appIconRes);
        cancelTV.requestFocus();

        confirmTV.setOnClickListener(this);
        cancelTV.setOnClickListener(this);
    }

    public void setOnDialogButtonClickListener(OnDialogButtonClickListener listener) {
        this.onDialogButtonClickListener = listener;
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_confirm){
            onDialogButtonClickListener.onConfirmClick();
        } else if (v.getId() == R.id.btn_cancel) {
            onDialogButtonClickListener.onCancelClick();
        }
    }
}
