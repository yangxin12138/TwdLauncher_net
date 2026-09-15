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
import android.graphics.drawable.Drawable;
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
import android.view.animation.LinearInterpolator;
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
    private ImageView im_usb;
    public ImageView im_youtube;
    private ImageView im_googleplay;
    private ImageView im_application;
    private ImageView im_settings;
    private ImageView im_files;
    public ImageView im_hdmi;
    private ImageView im_rutube;
    private ImageView im_vk;
    private ImageView im_rustortv;
    private View time_bar;
    private Handler timerHandler = new Handler();
    private boolean firstNetwork;
    SharedPreferences sharedPreferences;
    SharedPreferences selectedPreferences;
    SharedPreferences currentFocusPreferences;
    IndexHeatsetAdapter heatAdapter;
    GridView gridView;
    List<ApplicationInfo> appList = new ArrayList<>();
    private List<String> failedApkList = new ArrayList<>();
    private Utils utils;
    public static boolean isHeat = false;
    public static View lastFocus;
    public Handler mainHandler;

    private Drawable borderDrawable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setTheme(R.style.Theme_Index_Standard);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        utils.hideSystemUI(this);
        mainHandler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 1){
                    Log.i(TAG, "handleMessage: 通过Handler通信，重新刷新主页面");
                    recreate();
                }
            }
        };
        ((HandlerApplication) getApplication()).setMainHandler(mainHandler);
        //初始化时间
        sharedPreferences = getSharedPreferences("first_network", Context.MODE_PRIVATE);
        firstNetwork = sharedPreferences.getBoolean("firstConnected",false);
        updateTimeRunnable.run();
        selectedPreferences = getSharedPreferences("SelectedApps",Context.MODE_PRIVATE);
        currentFocusPreferences = getSharedPreferences("currentFocus",Context.MODE_PRIVATE);
        int currentFocusId = sharedPreferences.getInt("current_focus_id", R.id.im_rutube);
        mCurrentFocus = findViewById(currentFocusId);

    }
    private Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            if (!firstNetwork){
               // Log.i(TAG, "run: 判断是否联网");
                //检查网络连接状态
                ConnectivityManager connectivityManager =(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

                if (networkInfo != null && networkInfo.isConnected()){
                   // Log.i(TAG, "run: ---------已联网");
                    //如果设备已连接到网络,从网络获取时间和日期数据
                    sharedPreferences.edit().putBoolean("firstConnected",true).apply();
                    firstNetwork = true;
                    getSystemTime();
                    im_wifi.setImageResource(R.drawable.icon_wifi_connected);
                    // 每隔一秒更新一次时间
                    timerHandler.postDelayed(this, 1000);
                    return;
                }else {
                   // Log.i(TAG, "run: ---------未联网");
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
        unregisterReceiver(usbReceiver);
    }

    private void initView(){
        tv_time = findViewById(R.id.tv_time);
        tv_day = findViewById(R.id.tv_day);
        im_wifi = findViewById(R.id.im_wifi);
        im_usb = findViewById(R.id.im_usb);
        time_bar = findViewById(R.id.time_bar);

        utils = new Utils(this);
        //utils.isMacVerify();

        //判断USB是否已经连接
        im_usb.setImageResource(utils.isUsbPlugged(this) ? R.drawable.icon_usb_connected : R.drawable.icon_usb1);
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver,usbFilter);

        im_rutube = findViewById(R.id.im_rutube); im_rutube.setOnFocusChangeListener(this::onFocusChange); im_rutube.setOnClickListener(this::onClick);
        im_vk = findViewById(R.id.im_vk); im_vk.setOnFocusChangeListener(this::onFocusChange); im_vk.setOnClickListener(this::onClick);
        im_rustortv = findViewById(R.id.im_rustortv); im_rustortv.setOnFocusChangeListener(this::onFocusChange); im_rustortv.setOnClickListener(this::onClick);
        im_youtube = findViewById(R.id.im_youtube); im_youtube.setOnFocusChangeListener(this::onFocusChange); im_youtube.setOnClickListener(this::onClick);
        im_googleplay = findViewById(R.id.im_googleplay); im_googleplay.setOnFocusChangeListener(this::onFocusChange); im_googleplay.setOnClickListener(this::onClick); im_googleplay.setOnKeyListener(this::onKey);
        im_application = findViewById(R.id.im_application); im_application.setOnFocusChangeListener(this::onFocusChange); im_application.setOnClickListener(this::onClick); im_application.setOnKeyListener(this::onKey);
        im_settings = findViewById(R.id.im_settings); im_settings.setOnFocusChangeListener(this::onFocusChange); im_settings.setOnClickListener(this::onClick);
        im_files = findViewById(R.id.im_files); im_files.setOnFocusChangeListener(this::onFocusChange); im_files.setOnClickListener(this::onClick);
        im_hdmi = findViewById(R.id.im_hdmi); im_hdmi.setOnFocusChangeListener(this::onFocusChange); im_hdmi.setOnClickListener(this::onClick);im_hdmi.setOnKeyListener(this::onKey);
        gridView = findViewById(R.id.heat_set);
        borderDrawable = getResources().getDrawable(R.drawable.border_white);
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
            //TODO: hdmi跳转
            packageName = Utils.readSystemProp("LAUNCHER_HDMI_PACKAGE");
            className = Utils.readSystemProp("LAUNCHER_HDMI_CLASS");
            intent = new Intent();
            intent.setComponent(new ComponentName(packageName,className));
        } else if (v.getId() == R.id.im_files) {//file
            packageName = Utils.readSystemProp("LAUNCHER_FILE_PACKAGE");
            className = Utils.readSystemProp("LAUNCHER_FILE_CLASS");
            intent = new Intent();
            intent.setComponent(new ComponentName(packageName, className));
        }else if (v.getId() == R.id.im_rutube) {//rutube
            packageName = Utils.readSystemProp("LAUNCHER_RUTUBE_PACKAGE");
            className = Utils.readSystemProp("LAUNCHER_RUTUBE_CLASS");
            intent = new Intent();
            intent.setComponent(new ComponentName(packageName, className));
        }else if (v.getId() == R.id.im_vk) {//vk
            packageName = Utils.readSystemProp("LAUNCHER_VK_PACKAGE");
            className = Utils.readSystemProp("LAUNCHER_VK_CLASS");
            intent = new Intent();
            intent.setComponent(new ComponentName(packageName, className));
        }else if (v.getId() == R.id.im_rustortv) {//rustortv
            packageName = Utils.readSystemProp("LAUNCHER_RUSTORTV_PACKAGE");
            className = Utils.readSystemProp("LAUNCHER_RUSTORTV_CLASS");
            intent = new Intent();
            intent.setComponent(new ComponentName(packageName, className));
        }

        if (intent != null){
            Log.i(TAG, "onClick: intent不为空");
            try {
                startActivity(intent);
            }catch (Exception e){
                Toast.makeText(this, "应用不存在", Toast.LENGTH_SHORT).show();
            }
        }else {
            Log.i(TAG, "onClick: intent是为空");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        utils.hideSystemUI(this);
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

        if (isHeat){
            if (heatAdapter.getSelectionPosition() != -1){
                int finalPosition = heatAdapter.getSelectionPosition();
                // 确保位置在有效范围内
                if (finalPosition >= 3) {
                    finalPosition = 2;
                }
                final int pos = finalPosition;
                gridView.post(new Runnable() {
                    public void run() {
                        Log.i(TAG, "run: position = "+ pos );
                        View child = gridView.getChildAt(pos);
                        if (child != null) {
                            child.requestFocus();
                        } else {
                            gridView.requestFocus();
                            gridView.setSelection(pos);
                        }
                    }
                });
            }
        }else {
            int currentFocusId = currentFocusPreferences.getInt("current_focus_id", R.id.im_rutube);
            mCurrentFocus = findViewById(currentFocusId);
            if (mCurrentFocus == null) {
                mCurrentFocus = findViewById(R.id.im_rutube);
            }
            if (mCurrentFocus != null){
                final View focusView = mCurrentFocus;
                focusView.post(() -> {
                    boolean success = focusView.requestFocus();
                    Log.i(TAG, "onResume: requestFocus " + (success ? "success" : "failed"));
                    // 兜底：如果第一次请求失败，再试一次
                    if (!success) {
                        focusView.requestFocus();
                    }
                });
            }
        }



    }

    private View mCurrentFocus;
    @Override
    protected void onPause() {
        super.onPause();
        View currentFocus = getCurrentFocus();
        if (currentFocus  != null){
            SharedPreferences.Editor editor = currentFocusPreferences.edit();
            editor.putInt("current_focus_id", currentFocus.getId());
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
                v.setForeground(borderDrawable);
                v.postInvalidate(); // 强制视图刷新，避免延迟
                // 动画取消延迟，直接启动
                v.animate().cancel(); // 取消未完成的动画
                v.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .translationZ(1f)
                        .setDuration(80)
                        .setStartDelay(0) // 明确取消启动延迟
                        .setInterpolator(new LinearInterpolator()) // 线性插值，避免动画起步慢
                        .start();
            }
        }else {
            if (!isHeat){
                // 隐藏边框
                v.setForeground(null);
                v.postInvalidate();
                v.animate().cancel(); // 取消未完成的动画
                v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .translationZ(0f)
                        .setDuration(80)
                        .setStartDelay(0)
                        .setInterpolator(new LinearInterpolator())
                        .start();
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
        if (v.getId() == R.id.im_rustortv){
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.getAction()==KeyEvent.ACTION_DOWN){
                Log.i(TAG, "onKey: 触发key按键事件==========im_googleplay");
                if (v.isFocused()){
                    im_application.requestFocus();
                    return true;
                }
            }
        } else if (v.getId() == R.id.im_application) {
            Log.i(TAG, "onKey: 触发key按键事件==========im_application");
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && event.getAction() == KeyEvent.ACTION_DOWN){
                if (v.isFocused()){
                    im_rustortv.requestFocus();
                    return true;
                }
            }
        } else if (v.getId() == R.id.im_hdmi) {
            Log.i(TAG, "onKey: 触发key按键事件==========im_application");
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.getAction() == KeyEvent.ACTION_DOWN){
                if (v.isFocused()){
                    im_googleplay.requestFocus();
                    return true;
                }
            }
        } else if (v.getId() == R.id.im_googleplay) {
            Log.i(TAG, "onKey: 触发key按键事件==========im_application");
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && event.getAction() == KeyEvent.ACTION_DOWN){
                if (v.isFocused()){
                    im_hdmi.requestFocus();
                    return true;
                }
            }
        }else if (v.getId() == R.id.im_youtube) {
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            // 只处理方向键
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                    keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {

                View currentFocus = getCurrentFocus();
                // 如果当前没有焦点，强制把焦点还给默认的im_netflix
                if (currentFocus == null) {
                    View defaultFocus = findViewById(R.id.im_rutube);
                    if (defaultFocus != null) {
                        defaultFocus.requestFocus();
                        Log.i(TAG, "dispatchKeyEvent: focus is null, force to im_netflix");
                        return true; // 消费事件，不交给系统处理
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }
}