package com.twd.twdlaunchernet;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.twd.twdlaunchernet.adapter.IndexHeatsetAdapter;
import com.twd.twdlaunchernet.application.HandlerApplication;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import android.widget.RelativeLayout.LayoutParams;
public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnFocusChangeListener, View.OnKeyListener {

    private final static String TAG = MainActivity.class.getSimpleName();
    private TextView tv_time;
    private TextView tv_day;
    private ImageView im_wifi;
    private ImageView im_ble;
    private ImageView im_usb;
    private ImageView im_netflix;
    private ImageView im_youtube;
    private ImageView im_googleplay;
    private ImageView im_application;
    private ImageView im_settings;
    private ImageView im_files;
    private ImageView im_screenMirror;
    public ImageView im_hdmi;
    private View time_bar;
    private Handler timerHandler = new Handler();
    private boolean firstNetwork;
    SharedPreferences sharedPreferences;
    SharedPreferences firstBootPreferences;
    SharedPreferences selectedPreferences;
    SharedPreferences currentFocusPreferences;
    IndexHeatsetAdapter heatAdapter;
    GridView gridView;
    List<ApplicationInfo> appList = new ArrayList<>();
    private List<String> failedApkList = new ArrayList<>();
    private Utils utils;
    public static boolean isHeat = false;
    public static View lastFocus;
    String ui_theme_code = Utils.readSystemProp("UI_THEME_STYLE");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ui_theme_code.equals("Standard")){
            Log.i(TAG, "onCreate: 走默认UI");
            this.setTheme(R.style.Theme_Index_Standard);
        }else if(ui_theme_code.equals("Yameixun")) {
            Log.i(TAG, "onCreate: 走亚美寻UI");
            this.setTheme(R.style.Theme_Index_Yameixun);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        //初始化时间
        sharedPreferences = getSharedPreferences("first_network", Context.MODE_PRIVATE);
        firstBootPreferences = getSharedPreferences("firstBoot",Context.MODE_PRIVATE);
        firstNetwork = sharedPreferences.getBoolean("firstConnected",false);
        updateTimeRunnable.run();
        selectedPreferences = getSharedPreferences("SelectedApps",Context.MODE_PRIVATE);
        currentFocusPreferences = getSharedPreferences("currentFocus",Context.MODE_PRIVATE);
        int currentFocusId = sharedPreferences.getInt("current_focus_id", R.id.im_netflix);
        mCurrentFocus = findViewById(currentFocusId);
    }

    private Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            if (!firstNetwork){
                Log.i(TAG, "run: 判断是否联网");
                //检查网络连接状态
                ConnectivityManager connectivityManager =(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

                if (networkInfo != null && networkInfo.isConnected()){
                    Log.i(TAG, "run: ---------已联网");
                    //如果设备已连接到网络,从网络获取时间和日期数据
                    sharedPreferences.edit().putBoolean("firstConnected",true).apply();
                    firstNetwork = true;
                    getSystemTime();
                    im_wifi.setImageResource(R.drawable.icon_wifi_connected);
                    // 每隔一秒更新一次时间
                    timerHandler.postDelayed(this, 1000);
                    return;
                }else {
                    Log.i(TAG, "run: ---------未联网");
                    // 如果设备未连接到网络，设置时间为--:--，日期不显示
                    im_wifi.setImageResource(R.drawable.icon_wifi);
                    tv_time.setText("");
                    tv_day.setText("");
                    time_bar.setVisibility(View.GONE);
                    // 每隔一秒检查网络连接状态
                    timerHandler.postDelayed(this, 1000);
                    return;
                }
            }
            //检查网络连接状态
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()){
                im_wifi.setImageResource(R.drawable.icon_wifi_connected);
                getSystemTime();
            }else {
                getSystemTime();
                im_wifi.setImageResource(R.drawable.icon_wifi);
            }
            //每隔一秒更新一次时间
            timerHandler.postDelayed(this,1000);
        }
    };

    private void getSystemTime(){
        //获取当前时间和日期
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();


        //设置日期的格式
        TimeZone timeZone = calendar.getTimeZone();
        DateFormat dateFormat;
        if ("Asia/Shanghai".equals(timeZone.getID())){
            dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        }else {
            dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        }
        String formatterDate = dateFormat.format(currentDate);

        String dayOfWeek = new SimpleDateFormat("EEEE", Locale.getDefault()).format(currentDate);
        formatterDate = dayOfWeek+"\n"+formatterDate;

        String timeFormatString = Utils.getTimeFormat(this);
        //设置时间的格式
        DateFormat timeFormat = new SimpleDateFormat(timeFormatString);
        String formatterTime = timeFormat.format(currentDate);

        //在TextView上更新日期和时间
        tv_day.setText(formatterDate);
        tv_time.setText(formatterTime);
        time_bar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(updateTimeRunnable);
        unregisterReceiver(customReceiver);
        unregisterReceiver(usbReceiver);
    }

    private void initView(){
        tv_time = findViewById(R.id.tv_time);
        tv_day = findViewById(R.id.tv_day);
        im_wifi = findViewById(R.id.im_wifi);
        im_ble = findViewById(R.id.im_ble);
        im_usb = findViewById(R.id.im_usb);
        time_bar = findViewById(R.id.time_bar);

        utils = new Utils(this);
        // utils.isMacVerify();
        //判断蓝牙是否已连接
        im_ble.setImageResource(utils.isBluetoothConnected() ? R.drawable.icon_ble_connected : R.drawable.icon_ble);
        IntentFilter bleFilter = new IntentFilter();
        bleFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        bleFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(customReceiver,bleFilter);

        //判断USB是否已经连接
        im_usb.setImageResource(utils.isUsbPlugged(this) ? R.drawable.icon_usb_connected : R.drawable.icon_usb1);
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver,usbFilter);

        im_netflix = findViewById(R.id.im_netflix); im_netflix.setOnFocusChangeListener(this::onFocusChange); im_netflix.setOnClickListener(this::onClick);
        im_youtube = findViewById(R.id.im_youtube); im_youtube.setOnFocusChangeListener(this::onFocusChange); im_youtube.setOnClickListener(this::onClick);
        im_googleplay = findViewById(R.id.im_googleplay); im_googleplay.setOnFocusChangeListener(this::onFocusChange); im_googleplay.setOnClickListener(this::onClick); im_googleplay.setOnKeyListener(this::onKey);
        im_application = findViewById(R.id.im_application); im_application.setOnFocusChangeListener(this::onFocusChange); im_application.setOnClickListener(this::onClick); im_application.setOnKeyListener(this::onKey);
        im_settings = findViewById(R.id.im_settings); im_settings.setOnFocusChangeListener(this::onFocusChange); im_settings.setOnClickListener(this::onClick);
        im_files = findViewById(R.id.im_files); im_files.setOnFocusChangeListener(this::onFocusChange); im_files.setOnClickListener(this::onClick);
        im_hdmi = findViewById(R.id.im_hdmi); im_hdmi.setOnFocusChangeListener(this::onFocusChange); im_hdmi.setOnClickListener(this::onClick);im_hdmi.setOnKeyListener(this::onKey);
        im_screenMirror = findViewById(R.id.im_screenmirror); im_screenMirror.setOnFocusChangeListener(this::onFocusChange); im_screenMirror.setOnClickListener(this::onClick);im_screenMirror.setOnKeyListener(this::onKey);
        gridView = findViewById(R.id.heat_set);
        appList = Utils.getSelectedApps(this);
        heatAdapter = new IndexHeatsetAdapter(this,appList);
        gridView.setAdapter(heatAdapter);
        List<ApplicationInfo> finalAppList = appList;
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position<finalAppList.size()){
                    ApplicationInfo appInfo = finalAppList.get(position);
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(appInfo.packageName);
                    if (launchIntent != null){
                        startActivity(launchIntent);
                    }
                    Log.i(TAG, "onItemClick: 点到其他app了");
                }else {
                    // 比如弹出对话框、跳转页面等
                    Log.i(TAG, "onItemClick: 点到加号了");
                    Intent intent = new Intent(getApplicationContext(),ApplicationActivity.class);
                    intent.putExtra("list_mode",2);
                    startActivity(intent);
                }
            }
        });
    }

    private BroadcastReceiver customReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)){
                //蓝牙设备已连接
                im_ble.setImageResource(R.drawable.icon_ble_connected);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //蓝牙设备已断开
                im_ble.setImageResource(R.drawable.icon_ble);
            }
        }
    };

    private BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)){
                //USB插入
                Log.d("USB", "USB device attached: " + device.getDeviceName());
                im_usb.setImageResource(R.drawable.icon_usb_connected);
            } else  {
                // USB设备拔出
                Log.d("USB", "USB device detached: " + device.getDeviceName());
                im_usb.setImageResource(R.drawable.icon_usb1);
            }
        }
    };

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
        SharedPreferences thisSharedPreferences = getSharedPreferences("SelectedApps", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = thisSharedPreferences.getAll();
        int size = allEntries.size();
        Log.i("yangxin", "onResume: thisSharedPreferences数量 = " + size);
        Utils.getSelectedApps(this);
        Log.i("yangxin", "onResume: 数量 = " + Utils.getSelectedApps(this).size());
        int position = heatAdapter.getSelectionPosition();
        int originalSize = appList.size();
        appList.clear();
        appList.addAll(Utils.getSelectedApps(this));
        heatAdapter.notifyDataSetChanged();
        int diff = originalSize - appList.size();
        Log.i(TAG, "onResume: originalSize = "+ originalSize + ",appList.size() = " + appList.size() + ",diff = " + diff);
        if (diff > 0 ){
            position = position -diff;
        } else if (diff < 0) {
            position = position + Math.abs(diff);
        }
        heatAdapter.setSelectionPosition(position);
        if (isHeat){
            if (heatAdapter.getSelectionPosition() != -1){
                int finalPosition = position;
                gridView.post(new Runnable() {
                    public void run() {
                        Log.i(TAG, "run: position = "+ finalPosition);
                        Log.i(TAG, "run: heatAdapter.getSelectionPosition() = "+heatAdapter.getSelectionPosition());
                        gridView.getChildAt(finalPosition).requestFocus();
                        Log.i(TAG, "run: heatAdapter.getSelectionPosition() = "+heatAdapter.getSelectionPosition());
                    }
                });
            }
        }else {
            int currentFocusId = currentFocusPreferences.getInt("current_focus_id", R.id.im_netflix);
            mCurrentFocus = findViewById(currentFocusId);
            if (mCurrentFocus != null){
                Log.i(TAG, "onResume: focus不为空  id= "+mCurrentFocus.getId());
                mCurrentFocus.requestFocus();
            }
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
        if (hasFocus){
            lastFocus =v;
            // 添加切换动画效果
            if (!isHeat){
                v.animate().scaleX(1.2f).scaleY(1.2f).translationZ(1f).setDuration(100);
            }
        }else {
            if (!isHeat){
                v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(100);
            }
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
                    im_screenMirror.requestFocus();
                    return true;
                }
            }
        }  else if (v.getId() == R.id.im_hdmi) {
            Log.i(TAG, "onKey: 触发key按键事件==========im_hdmi");
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.getAction()==KeyEvent.ACTION_DOWN){
                if (v.isFocused()){
                    gridView.requestFocus();
                    gridView.setSelection(0);
                    return true;
                }
            }
        }
        return false;
    }
}
