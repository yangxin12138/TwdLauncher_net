package com.twd.twdlaunchernet;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * @Author:Yangxin
 * @Description:
 * @time: Create in 16:14 2024/7/9
 */
public class ImUnsetDialog extends Dialog {


    private TextView message;
    private Context context;
    public ImUnsetDialog(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imunset_dialog_layout);
        message = findViewById(R.id.dialog_text_view);
        message.setText(context.getString(R.string.application_imuninstall));
    }
}
