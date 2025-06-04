package com.twd.twdlaunchernet;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

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

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnFocusChangeListener, View.OnKeyListener {

    private final static String TAG = MainActivity.class.getSimpleName();
    private TextView tv_time;
    private TextView tv_day;
    private ImageView im_wifi;
    private ImageView im_ble;
    private ImageView im_usb;
    private ImageView im_application;
    private ImageView im_settings;
    private ImageView im_files;
    private ImageView im_game;
    private ImageView im_game_1;
    private ImageView im_game_2;
    private ImageView im_game_3;
    private ImageView im_game_more;
    private TextView tv_email;
    public ImageView im_hdmi;
    private View time_bar;
    private Handler timerHandler = new Handler();
    private boolean firstNetwork;
    SharedPreferences sharedPreferences;
    SharedPreferences firstBootPreferences;
    SharedPreferences selectedPreferences;
    SharedPreferences currentFocusPreferences;
    private Utils utils;
    public static View lastFocus;
    String ui_theme_code = Utils.readSystemProp("UI_THEME_STYLE");
    public Handler mainHandler;
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
        ((HandlerApplication) getApplication()).setMainHandler(mainHandler);
        //初始化时间
        sharedPreferences = getSharedPreferences("first_network", Context.MODE_PRIVATE);
        firstBootPreferences = getSharedPreferences("firstBoot",Context.MODE_PRIVATE);
        firstNetwork = sharedPreferences.getBoolean("firstConnected",false);
        updateTimeRunnable.run();
        selectedPreferences = getSharedPreferences("SelectedApps",Context.MODE_PRIVATE);
        currentFocusPreferences = getSharedPreferences("currentFocus",Context.MODE_PRIVATE);
        int currentFocusId = sharedPreferences.getInt("current_focus_id", R.id.im_game);
        mCurrentFocus = findViewById(currentFocusId);

    }

    //获取指定目录下的所有 APK 文件的文件路径
    private List<String> getApkFilesInDirectory(String directoryPath){
        List<String> apkFilePaths = new ArrayList<>();
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()){
            File[] files = directory.listFiles();
            if (files != null){
                for (File file : files){
                    if(file.isFile() && file.getName().endsWith(".apk")){
                        apkFilePaths.add(file.getAbsolutePath());
                    }
                }
            }
        }
        return apkFilePaths;
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
                    tv_day.setText("");
                    tv_time.setText("");
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
        //unregisterReceiver(usbReceiver);
        unregisterReceiver(tfCardReceiver);
    }

    private void initView(){
        tv_time = findViewById(R.id.tv_time);
        tv_day = findViewById(R.id.tv_day);
        im_wifi = findViewById(R.id.im_wifi);
        im_ble = findViewById(R.id.im_ble);
        im_usb = findViewById(R.id.im_usb);
        time_bar = findViewById(R.id.time_bar);
        tv_email = findViewById(R.id.tv_email);


        tv_email.setText(Utils.readSystemProp("EMAIL_ADDR"));
        tv_email.setVisibility(Utils.readSystemProp("EMAIL_ADDR_VISIABLE").equals("true") ? View.VISIBLE : View.GONE);

        utils = new Utils(this);
        utils.isMacVerify();
        //判断蓝牙是否已连接
        im_ble.setImageResource(utils.isBluetoothConnected() ? R.drawable.icon_ble_connected : R.drawable.icon_ble);
        /*IntentFilter bleFilter = new IntentFilter();
        bleFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        bleFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(customReceiver,bleFilter);*/

        //判断USB是否已经连接
        im_usb.setImageResource(utils.isTfCardPlugged(this) ? R.drawable.icon_tf_connected : R.drawable.icon_tf);
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver,usbFilter);

        // 注册TF卡状态变化的广播接收器
        IntentFilter tfFilter = new IntentFilter();
        tfFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        tfFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        tfFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        tfFilter.addDataScheme("file"); // 必须添加此项才能接收外部存储事件
        registerReceiver(tfCardReceiver, tfFilter);
        im_application = findViewById(R.id.im_application); im_application.setOnFocusChangeListener(this::onFocusChange); im_application.setOnClickListener(this::onClick); im_application.setOnKeyListener(this::onKey);
        im_settings = findViewById(R.id.im_settings); im_settings.setOnFocusChangeListener(this::onFocusChange); im_settings.setOnClickListener(this::onClick);
        im_files = findViewById(R.id.im_files); im_files.setOnFocusChangeListener(this::onFocusChange); im_files.setOnClickListener(this::onClick);
        im_game = findViewById(R.id.im_game); im_game.setOnFocusChangeListener(this::onFocusChange);im_game.setOnClickListener(this::onClick);
        im_game_1 = findViewById(R.id.im_game_1);im_game_1.setOnFocusChangeListener(this::onFocusChange);im_game_1.setOnClickListener(this::onClick);
        im_game_2 = findViewById(R.id.im_game_2);im_game_2.setOnFocusChangeListener(this::onFocusChange);im_game_2.setOnClickListener(this::onClick);
        im_game_3 = findViewById(R.id.im_game_3);im_game_3.setOnFocusChangeListener(this::onFocusChange);im_game_3.setOnClickListener(this::onClick);
        im_game_more = findViewById(R.id.im_game_more);im_game_more.setOnFocusChangeListener(this::onFocusChange);im_game_more.setOnClickListener(this::onClick);
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

    private BroadcastReceiver tfCardReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String path =  intent.getData().getPath();
            Log.d("TwdUtils", "TF 收到广播 " + path);
            //检查是否是我们关注的TF卡路径
            if ("/storage/sdcard1".equals(path) || "/storage/sdcard2".equals(path)){
                Log.d("TwdUtils", "TF 是我们关注的路径 ");
                if (action.equals(Intent.ACTION_MEDIA_MOUNTED)){
                    // TF卡已挂载（插入且可用）
                    Log.d("TwdUtils", "TF card mounted: " + path);
                    im_usb.setImageResource(R.drawable.icon_tf_connected);
                } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED) ||
                        action.equals(Intent.ACTION_MEDIA_REMOVED)) {
                    // TF卡已卸载或移除
                    Log.d("TwdUtils", "TF card unmounted/removed: " + path);
                    im_usb.setImageResource(R.drawable.icon_tf);
                }
            }
        }
    };

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
        }  else if (v.getId() == R.id.im_files) {//file
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
        SharedPreferences thisSharedPreferences = getSharedPreferences("SelectedApps", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = thisSharedPreferences.getAll();
        int size = allEntries.size();
        Log.i("yangxin", "onResume: thisSharedPreferences数量 = " + size);
        Utils.getSelectedApps(this);
        Log.i("yangxin", "onResume: 数量 = " + Utils.getSelectedApps(this).size());
        int currentFocusId = currentFocusPreferences.getInt("current_focus_id", R.id.im_game);
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
        if (hasFocus){
            lastFocus =v;
            // 添加切换动画效果
            Drawable drawable = getResources().getDrawable(R.drawable.border_white);
            ViewCompat.setBackground(v, drawable);
                //v.setForeground(getResources().getDrawable(R.drawable.border_white));
                v.animate().scaleX(1.2f).scaleY(1.2f).translationZ(1f).setDuration(100);
        }else {
                // 隐藏边框
            ViewCompat.setBackground(v, null);
               // v.setForeground(null);
                v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(100);
            }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        Log.i(TAG, "onKey: 触发key按键事件==========");
        if (v.getId() == R.id.im_files){
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.getAction()==KeyEvent.ACTION_DOWN){
                Log.i(TAG, "onKey: 触发key按键事件==========im_googleplay");
                if (v.isFocused()){
                    im_game_1.requestFocus();
                    return true;
                }
            }
        } else if (v.getId() == R.id.im_game_1) {
            Log.i(TAG, "onKey: 触发key按键事件==========im_application");
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && event.getAction() == KeyEvent.ACTION_DOWN){
                if (v.isFocused()){
                    im_files.requestFocus();
                    return true;
                }
            }
        }
        return false;
    }
}