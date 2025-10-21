package com.twd.twdlaunchernet;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.twd.twdlaunchernet.application.HandlerApplication;

import java.util.ArrayList;
import java.util.List;
public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnFocusChangeListener, View.OnKeyListener {

    private final static String TAG = MainActivity.class.getSimpleName();
    SharedPreferences sharedPreferences;
    SharedPreferences currentFocusPreferences;
    public static View lastFocus;
    public Handler mainHandler;
    private ImageView im_netflix;
    private ImageView im_youtube;
    private ImageView im_googleplay;
    private ImageView im_screenmirror;
    private ImageView im_application;
    private ImageView im_settings;
    private ImageView im_files;
    private ImageView im_hdmi;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        ((HandlerApplication) getApplication()).setMainHandler(mainHandler);
        //初始化时间
        sharedPreferences = getSharedPreferences("first_network", Context.MODE_PRIVATE);
        currentFocusPreferences = getSharedPreferences("currentFocus",Context.MODE_PRIVATE);
        int currentFocusId = sharedPreferences.getInt("current_focus_id", R.id.im_netflix);
        mCurrentFocus = findViewById(currentFocusId);

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initView(){
        im_netflix = findViewById(R.id.im_netflix);
        im_youtube = findViewById(R.id.im_youtube);
        im_googleplay = findViewById(R.id.im_googleplay);
        im_screenmirror = findViewById(R.id.im_screenmirror);
        im_application = findViewById(R.id.im_application);
        im_files = findViewById(R.id.im_files);
        im_settings = findViewById(R.id.im_settings);
        im_hdmi = findViewById(R.id.im_hdmi);

        im_netflix.setOnFocusChangeListener(this::onFocusChange); im_youtube.setOnFocusChangeListener(this::onFocusChange);
        im_googleplay.setOnFocusChangeListener(this::onFocusChange); im_screenmirror.setOnFocusChangeListener(this::onFocusChange);
        im_application.setOnFocusChangeListener(this::onFocusChange); im_files.setOnFocusChangeListener(this::onFocusChange);
        im_settings.setOnFocusChangeListener(this::onFocusChange); im_hdmi.setOnFocusChangeListener(this::onFocusChange);

        im_netflix.setOnClickListener(this::onClick); im_youtube.setOnClickListener(this::onClick);
        im_googleplay.setOnClickListener(this::onClick); im_screenmirror.setOnClickListener(this::onClick);
        im_application.setOnClickListener(this::onClick); im_files.setOnClickListener(this::onClick);
        im_settings.setOnClickListener(this::onClick); im_hdmi.setOnClickListener(this::onClick);
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        String packageName = "";
        String className = "";
        if (v.getId() == R.id.im_application){ //所有应用
            intent = new Intent(this,ApplicationActivity.class);
            intent.putExtra("list_mode",1);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } else if (v.getId() == R.id.im_settings) { //Settings
            packageName = Utils.readSystemProp("LAUNCHER_SETTING_PACKAGE");
            className = Utils.readSystemProp("LAUNCHER_SETTING_CLASS");
            intent = new Intent();
            intent.setComponent(new ComponentName(packageName,className));
        } else if (v.getId() == R.id.im_netflix) { //Netflix
            packageName = Utils.readSystemProp("LAUNCHER_NETFLIX_PACKAGE");
            className = Utils.readSystemProp("LAUNCHER_NETFLIX_CLASS");
            intent = new Intent();
            intent.setComponent(new ComponentName(packageName,className));
        } else if (v.getId() == R.id.im_youtube) { // youtube
            packageName = Utils.readSystemProp("LAUNCHER_YOUTUBE_PACKAGE");
            className = Utils.readSystemProp("LAUNCHER_YOUTUBE_CLASS");
            intent = new Intent();
            intent.setComponent(new ComponentName(packageName,className));
        } else if (v.getId() == R.id.im_googleplay) { //google paly
            packageName = Utils.readSystemProp("LAUNCHER_GOOGLE_PACKAGE");
            className = Utils.readSystemProp("LAUNCHER_GOOGLE_CLASS");
            intent = new Intent();
            intent.setComponent(new ComponentName(packageName,className));
        } else if (v.getId() == R.id.im_hdmi) {
            packageName = Utils.readSystemProp("LAUNCHER_HDMI_PACKAGE");
            className = Utils.readSystemProp("LAUNCHER_HDMI_CLASS");
            intent = new Intent();
            intent.setComponent(new ComponentName(packageName,className));
        } else if (v.getId() == R.id.im_files) {//file
            packageName = Utils.readSystemProp("LAUNCHER_FILE_PACKAGE");
            className = Utils.readSystemProp("LAUNCHER_FILE_CLASS");
            intent = new Intent();
            intent.setComponent(new ComponentName(packageName,className));
        } else if (v.getId() == R.id.im_screenmirror) {
            packageName = Utils.readSystemProp("LAUNCHER_MIRROR_PACKAGE");
            className = Utils.readSystemProp("LAUNCHER_MIRROR_CLASS");
            intent = new Intent();
            intent.setComponent(new ComponentName(packageName,className));
        }

        if (intent != null){
            Log.i(TAG, "onClick: intent不为空");
            try {
                Log.i(TAG, "onClick: 获得点击快捷方式启动包名："+packageName+",类名 ="+className);
                startActivity(intent);
            }catch (Exception e){
                Toast.makeText(this, "应用不存在", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
         int currentFocusId = currentFocusPreferences.getInt("current_focus_id", R.id.im_netflix);
         mCurrentFocus = findViewById(currentFocusId);
         if (mCurrentFocus != null){
             Log.i(TAG, "onResume: focus不为空  id= "+mCurrentFocus.getId());
             mCurrentFocus.requestFocus();
        }
    }

    private View mCurrentFocus;
    @Override
    protected void onPause() {
        super.onPause();
        if (lastFocus != null){
            SharedPreferences.Editor editor = currentFocusPreferences.edit();
            editor.putInt("current_focus_id",lastFocus.getId());
            Log.i(TAG, "onPause: 存进去"+lastFocus.getId());
            editor.apply();
        }

    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        Log.i(TAG, "onFocusChange: -----获取焦点--- id = " + v.getId());
        if (hasFocus) {
            lastFocus = v;
            //v.setForeground(getResources().getDrawable(R.drawable.border_white));
            v.animate().scaleX(1.2f).scaleY(1.2f).translationZ(1f).setDuration(100);
        }else {
           // v.setForeground(null);
            v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(100);
        }
    }
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onUserLeaveHint() {
        Log.i(TAG, "onUserLeaveHint: 点击home键");
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        Log.i(TAG, "onKey: 触发key按键事件==========");
        if (v.getId() == R.id.im_screenmirror){
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.getAction()==KeyEvent.ACTION_DOWN){
                Log.i(TAG, "onKey: 触发key按键事件==========im_screenmirror");
                if (v.isFocused()){
                    im_application.requestFocus();
                    return true;
                }
            }
        } else if (v.getId() == R.id.im_application) {
            Log.i(TAG, "onKey: 触发key按键事件==========im_application");
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && event.getAction() == KeyEvent.ACTION_DOWN){
                if (v.isFocused()){
                    im_screenmirror.requestFocus();
                    return true;
                }
            }
        }  /*else if (v.getId() == R.id.im_hdmi) {
            Log.i(TAG, "onKey: 触发key按键事件==========im_hdmi");
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.getAction()==KeyEvent.ACTION_DOWN){
                if (v.isFocused()){
                    gridView.requestFocus();
                    gridView.setSelection(0);
                    return true;
                }
            }
        }*/
        return false;
    }
}