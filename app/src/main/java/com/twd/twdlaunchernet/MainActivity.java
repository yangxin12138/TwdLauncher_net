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
        if (v.getId() == R.id.im_application){ //所有应用
            intent = new Intent(this,ApplicationActivity.class);
            intent.putExtra("list_mode",1);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } else if (v.getId() == R.id.im_settings) { //Settings
            intent = new Intent();
            intent.setComponent(new ComponentName("com.twd.setting","com.twd.setting.MainActivity"));
        } else if (v.getId() == R.id.im_netflix) { //Netflix
            intent = new Intent();
            Intent tvIntent = new Intent();
            tvIntent.setComponent(new ComponentName("com.netflix.ninja","com.netflix.ninja.MainActivity"));
            if (getPackageManager().resolveActivity(tvIntent, PackageManager.MATCH_DEFAULT_ONLY) == null) {
                //如果TV版不存在则启动移动版
                intent.setComponent(new ComponentName("com.netflix.mediaclient","com.netflix.mediaclient.ui.launch.UIWebViewActivity"));
            }else {
                intent = tvIntent;
            }
        } else if (v.getId() == R.id.im_youtube) { // youtube
            intent = new Intent();
            intent.setComponent(new ComponentName("com.google.android.youtube.tv","com.google.android.apps.youtube.tv.activity.ShellActivity"));
        } else if (v.getId() == R.id.im_googleplay) { //google paly
            intent = new Intent();
            if(Build.HARDWARE.equals("mt6735")){
                intent.setComponent(new ComponentName("com.android.vending","com.android.vending.AssetBrowserActivity"));
            }else {
                intent.setComponent(new ComponentName("com.android.vending","com.google.android.finsky.tvmainactivity.TvMainActivity"));
            }
        } else if (v.getId() == R.id.im_hdmi) {
            //TODO: hdmi跳转
            intent = new Intent();
            if(Build.HARDWARE.equals("mt6735")){
                intent.setComponent(new ComponentName("com.twd.twdcamera","com.twd.twdcamera.MainActivity"));
            }else {
                intent.setComponent(new ComponentName("com.softwinner.awsource","com.softwinner.awsource.MainActivity"));
            }
        } else if (v.getId() == R.id.im_files) {//file
            intent = new Intent();
            if(Build.HARDWARE.equals("mt6735")){
                intent.setComponent(new ComponentName("com.vsoontech.mos.filemanager", "com.vsoontech.filemanager.business.index.IndexAty"));
            }else {
                intent.setComponent(new ComponentName("com.softwinner.TvdFileManager", "com.softwinner.TvdFileManager.MainUI"));
            }
        }

        if (intent != null){
            Log.i(TAG, "onClick: intent不为空");
            try {
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