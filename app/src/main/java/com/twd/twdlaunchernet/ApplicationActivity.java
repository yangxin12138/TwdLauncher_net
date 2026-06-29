package com.twd.twdlaunchernet;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.twd.twdlaunchernet.adapter.ApplicationAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class ApplicationActivity extends AppCompatActivity {
    private TextView tv_time;
    private TextView tv_day;
    private ImageView im_wifi;
    private ImageView im_ble;
    private ImageView im_usb;
    private View time_bar;
    private Handler timerHandler = new Handler();
    private boolean firstNetwork;
    GridView gridView ;
    ApplicationAdapter adapter;
    SharedPreferences sharedPreferences;
    int listMode = 0;
    int MyPosition ;
    Context context;
    Utils utils;
    String ui_theme_code = "Standard";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ui_theme_code.equals("Standard")){
            this.setTheme(R.style.Theme_Index_Standard);
        }else if(ui_theme_code.equals("Yameixun")) {
            this.setTheme(R.style.Theme_Index_Yameixun);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application);
        context = this;
        utils = new Utils(context);
        Intent intent = getIntent();
        if (intent != null){
            listMode = intent.getIntExtra("list_mode",0);
        }
        utils.hideSystemUI(this);
        sharedPreferences = getSharedPreferences("SelectedApps", Context.MODE_PRIVATE);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        utils.hideSystemUI(this);
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(updateTimeRunnable);
        unregisterReceiver(customReceiver);
        unregisterReceiver(usbReceiver);
    }


    private void initView(){
        gridView = findViewById(R.id.gridView);
        tv_time = findViewById(R.id.tv_time);
        tv_day = findViewById(R.id.tv_day);
        im_wifi = findViewById(R.id.im_wifi);
        im_ble = findViewById(R.id.im_ble);
        im_usb = findViewById(R.id.im_usb);
        time_bar = findViewById(R.id.time_bar);

        im_ble.setImageResource(utils.isBluetoothConnected() ? R.drawable.icon_ble_connected : R.drawable.icon_ble);
        IntentFilter bleFilter = new IntentFilter();
        bleFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        bleFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(customReceiver,bleFilter);

        im_usb.setImageResource(utils.isUsbPlugged(this) ? R.drawable.icon_usb_connected : R.drawable.icon_usb1);
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver,usbFilter);

        updateTimeRunnable.run();
        PackageManager pm = getPackageManager();
        Intent intentLauncher = new Intent(Intent.ACTION_MAIN,null);
        intentLauncher.addCategory(Intent.CATEGORY_LAUNCHER);
        Intent intentLeanbackLauncher = new Intent(Intent.ACTION_MAIN, null);
        intentLeanbackLauncher.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);


        List<ResolveInfo> installedAppsLauncher  = pm.queryIntentActivities(intentLauncher,0);
        List<ResolveInfo> installedAppsLeanbackLauncher = pm.queryIntentActivities(intentLeanbackLauncher, 0);

        //合并两个列表
        List<ResolveInfo> combinedList = new ArrayList<>();
        Set<String> addedPackageNames = new HashSet<>();

        for (ResolveInfo info : installedAppsLauncher) {
            String packageName = info.activityInfo.packageName;
            if (!addedPackageNames.contains(packageName)){
                combinedList.add(info);
                addedPackageNames.add(packageName);
            }
        }
        for (ResolveInfo info : installedAppsLeanbackLauncher){
            String packageName = info.activityInfo.packageName;
            if (!addedPackageNames.contains(packageName)){
                combinedList.add(info);
                addedPackageNames.add(packageName);
            }
        }

        Iterator<ResolveInfo> iterator = combinedList.iterator();

        //遍历installedApps列表，过滤应用
        while (iterator.hasNext()){
            ResolveInfo resolveInfo = iterator.next();
            String packageName = resolveInfo.activityInfo.packageName;
            if ("com.twd.twdlaunchernet".equals(packageName)  || "com.android.tv.settings".equals(packageName) ||
            "com.android.calendar".equals(packageName) || "com.android.deskclock".equals(packageName) ||
            "com.android.email".equals(packageName) || "com.android.music".equals(packageName) ||
            "com.android.soundrecorder".equals(packageName) || "org.codeaurora.gallery".equals(packageName)||
            "org.codeaurora.snapcam".equals(packageName) || "com.android.calculator2".equals(packageName) ||
            "com.android.documentsui".equals(packageName) || "com.android.quicksearchbox".equals(packageName) ||
            "com.example.android.notepad".equals(packageName)){
                iterator.remove();//移除
            }
        }
        adapter = new ApplicationAdapter(this,combinedList,listMode);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(listMode == 1){
                     ResolveInfo app = combinedList.get(position);
                        String packageName = app.activityInfo.packageName;
                        String className = app.activityInfo.name;
                        try{
                            Intent intent = new Intent();
                            intent.setClassName(packageName,className);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                } else if (listMode == 2) {
                    ApplicationAdapter.ViewHold viewHold = (ApplicationAdapter.ViewHold) view.getTag();
                    String packageName = viewHold.packageName;
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    if (sharedPreferences.getBoolean(packageName,false)){
                        //已经选过了，取消选中
                        viewHold.iv_red.setVisibility(View.GONE);
                        editor.putBoolean(packageName,false);
                    }else {
                        //未选中，判断是否超过5个
                        int selectedCount = 0;
                        Map<String,?> allEntries = sharedPreferences.getAll();
                        for (Map.Entry<String,?> entry :allEntries.entrySet()){
                            if ((boolean) entry.getValue()){
                                selectedCount++;
                            }
                        }
                        if (selectedCount < 7) {
                            viewHold.iv_red.setVisibility(View.VISIBLE);
                            editor.putBoolean(packageName,true);
                        }else {
                            //提示用户最多只能选5个
                            Toast.makeText(getApplicationContext(), getString(R.string.application_maxvalue), Toast.LENGTH_SHORT).show();
                        }
                    }
                    editor.apply();
                }
            }
        });

        gridView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MyPosition = (int) adapter.getItemId(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                //菜单键
                Log.i("yangxin", "onKeyDown: 菜单键被按下");
                ResolveInfo app = (ResolveInfo) adapter.getItem(MyPosition);
                String packageName = app.activityInfo.packageName;
                String appName = null;
                Drawable appIcon = null;
                PackageManager manager = getPackageManager();
                try {
                    ApplicationInfo applicationInfo = manager.getApplicationInfo(packageName,0);
                    appName = (String) applicationInfo.loadLabel(manager);
                    appIcon = app.activityInfo.loadIcon(manager);
                    if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0){
                        Log.i("yangxin", "这是一个系统应用,包名："+packageName);
                        //TODO:展示不能卸载对话框
                        showSystemDialog();
                    }else if(packageName.equals("com.netflix.mediaclient") || packageName.equals("com.netflix.ninja") || packageName.equals("com.google.android.youtube.tv")){
                        Log.i("yangxin", "是奈飞,包名："+packageName);
                        //TODO:展示不能卸载对话框
                        showSystemDialog();
                    }else {
                        if (appName!=null && appIcon != null){
                            showUninstallDialog(packageName,appName,appIcon);
                        }else {
                            Log.i("yangxin","获取不到appName和icon");
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Log.i("yangxin", "onKeyDown: 方向右键被按下 gridView.getSelectedItemPosition() = " + gridView.getSelectedItemPosition());
                int count = gridView.getAdapter().getCount();
                if (MyPosition == count - 1){
                    Log.i("yangxin", "onKeyDown: 是最后一个，不予处理 ");
                    super.onKeyDown(keyCode, event);
                    break;
                }
                Log.i("yangxin", "onKeyDown: 不是最后一个，处理position ");
                gridView.setSelection(MyPosition + 1);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Log.i("yangxin", "onKeyDown: 方向左键被按下 gridView.getSelectedItemPosition() = " + gridView.getSelectedItemPosition());
                if (MyPosition == 0){
                    Log.i("yangxin", "onKeyDown: 是第一个，不予处理 ");
                    super.onKeyDown(keyCode, event);
                    break;
                }
                gridView.setSelection(MyPosition  - 1);
                Log.i("yangxin", "onKeyDown: 不是第一个，处理position ");
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
    private void showSystemDialog(){
        ImUnsetDialog imUnsetDialog = new ImUnsetDialog(this);
        imUnsetDialog.show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                imUnsetDialog.dismiss();
            }
        },2000);
    }
    private void showUninstallDialog(String packageName,String appName,Drawable appIcon){
        UninstallDialog uninstallDialog = new UninstallDialog(this,appName,appIcon);
        uninstallDialog.setOnDialogButtonClickListener(new OnDialogButtonClickListener() {
            @Override
            public void onConfirmClick() {
                uninstallApp(packageName);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        initView();
                        uninstallDialog.dismiss();
                    }
                },2000);

            }

            @Override
            public void onCancelClick() {
                uninstallDialog.dismiss();
            }
        });
        uninstallDialog.show();
    }

    public void uninstallApp(String packageName){
        Uri uri = Uri.fromParts("package", packageName, null);
        Intent intent = new Intent(Intent.ACTION_DELETE, uri);
        startActivity(intent);
        //Utils.execCommand(packageName);
    }
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
}