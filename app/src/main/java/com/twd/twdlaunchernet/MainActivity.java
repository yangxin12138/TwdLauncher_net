package com.twd.twdlaunchernet;

import android.annotation.SuppressLint;
import android.app.Application;
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
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.twd.twdlaunchernet.adapter.IndexHeatsetAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnFocusChangeListener {

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
    private ImageView im_hdmi;
    private Handler timerHandler = new Handler();
    private boolean firstNetwork;
    SharedPreferences sharedPreferences;
    SharedPreferences selectedPreferences;
    SharedPreferences currentFocusPreferences;
    IndexHeatsetAdapter heatAdapter;
    GridView gridView;
    List<ApplicationInfo> appList = new ArrayList<>();
    private Utils utils;
    public static boolean isHeat = false;
    public static boolean isSecondView = false;
    private   View lastFocus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        //初始化时间
        sharedPreferences = getSharedPreferences("first_network", Context.MODE_PRIVATE);
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

        utils = new Utils();
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
        im_googleplay = findViewById(R.id.im_googleplay); im_googleplay.setOnFocusChangeListener(this::onFocusChange); im_googleplay.setOnClickListener(this::onClick);
        im_application = findViewById(R.id.im_application); im_application.setOnFocusChangeListener(this::onFocusChange); im_application.setOnClickListener(this::onClick);
        im_settings = findViewById(R.id.im_settings); im_settings.setOnFocusChangeListener(this::onFocusChange); im_settings.setOnClickListener(this::onClick);
        im_files = findViewById(R.id.im_files); im_files.setOnFocusChangeListener(this::onFocusChange); im_files.setOnClickListener(this::onClick);
        im_hdmi = findViewById(R.id.im_hdmi); im_hdmi.setOnFocusChangeListener(this::onFocusChange); im_hdmi.setOnClickListener(this::onClick);
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
        if (v.getId() == R.id.im_application){ //所有应用
            intent = new Intent(this,ApplicationActivity.class);
            intent.putExtra("list_mode",1);
        } else if (v.getId() == R.id.im_settings) { //Settings
            intent = new Intent();
            intent.setComponent(new ComponentName("com.twd.setting","com.twd.setting.MainActivity"));
        } else if (v.getId() == R.id.im_netflix) { //Netflix
            intent = new Intent();
            intent.setComponent(new ComponentName("com.netflix.mediaclient","com.netflix.mediaclient.ui.launch.UIWebViewActivity"));
        } else if (v.getId() == R.id.im_youtube) { // youtube
            intent = new Intent();
            intent.setComponent(new ComponentName("com.google.android.youtube.tv","com.google.android.apps.youtube.tv.activity.ShellActivity"));
        } else if (v.getId() == R.id.im_googleplay) { //google paly
            intent = new Intent();
            intent.setComponent(new ComponentName("com.android.vending","com.google.android.finsky.tvmainactivity.TvMainActivity"));
        } else if (v.getId() == R.id.im_hdmi) {
            //TODO: hdmi跳转
            intent = new Intent();
            intent.setComponent(new ComponentName("com.softwinner.awsource","com.softwinner.awsource.MainActivity"));
        } else if (v.getId() == R.id.im_files) {//file
            intent = new Intent();
            intent.setComponent(new ComponentName("com.vsoontech.mos.filemanager", "com.vsoontech.filemanager.business.index.IndexAty"));
        }

        if (intent != null){
            Log.i(TAG, "onClick: intent不为空");
            startActivity(intent);
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
        SharedPreferences.Editor editor = currentFocusPreferences.edit();
        editor.putInt("current_focus_id",lastFocus.getId());
        Log.i(TAG, "onPause: 存进去"+lastFocus.getId());
        editor.apply();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        Log.i(TAG, "onFocusChange: -----获取焦点--- id = " + v.getId());
        FrameLayout borderView = findViewById(R.id.border_view);
        if (hasFocus){
            lastFocus =v;
            if (v.getId() == R.id.im_application || v.getId() == R.id.im_settings || v.getId() == R.id.im_files || v.getId() == R.id.im_hdmi)isSecondView = true;
            v.post(new Runnable() {
                @Override
                public void run() {
                    //为此处得到焦点时的处理内容
                    // 显示边框，并移动到当前控件的位置
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    Log.i(TAG, "onFocusChange: location[0] = "+location[0]+",location[1] = "+location[1]);
                    //获取焦点控件的宽度和高度
                    int width = v.getWidth();
                    int height = v.getHeight();
                    borderView.animate().x(location[0]-(isHeat ?15:(isSecondView?32:35))).y(location[1]-(isHeat ?110:120)).setDuration(250).translationZBy(11f).start();
                    borderView.setVisibility(View.VISIBLE);
                    borderView.getLayoutParams().width = width+(isHeat?30:(isSecondView?60:75));
                    borderView.getLayoutParams().height = height+(isHeat ? 30:38);
                    borderView.requestLayout(); //重新布局以应用大小变化
                }
            });
            // 添加切换动画效果
            v.animate().scaleX(1.2f).scaleY(1.2f).translationZ(10f).setDuration(200);
        }else {
            if (v.getId() == R.id.im_application || v.getId() == R.id.im_settings || v.getId() == R.id.im_files || v.getId() == R.id.im_hdmi)isSecondView = false;
            borderView.setVisibility(View.GONE);
            // 隐藏边框
            v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(200);
        }
    }
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
    }
}