package com.twd.twdlaunchernet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.twd.twdlaunchernet.adapter.ApplicationAdapter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * @Author:Yangxin
 * @Description:
 * @time: Create in 17:19 2024/5/20
 */
public class Utils {
    private static final String CLASS_NAME = "android.os.SystemProperties";

    public static String usbFilePath;
    private Context context;

    public Utils(Context context) {
        this.context = context;
    }

    public boolean isBluetoothConnected(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null){
            //设备不支持蓝牙
            return false;
        }
        Class<BluetoothAdapter> bluetoothAdapterClass = BluetoothAdapter.class;
        try{
            //得到连接状态的方法
            Method method = bluetoothAdapterClass.getDeclaredMethod("getConnectionState",(Class[]) null );
            //打开权限
            method.setAccessible(true);
            int state = (int) method.invoke(bluetoothAdapter,(Object[]) null );
            return state == BluetoothAdapter.STATE_CONNECTED;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }


    /*
    * 获取手机已安装应用列表
    * */
    @SuppressLint("QueryPermissionsNeeded")
    public static List<String> getAllApps(Context context){
        List<String> apps = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        //获取手机内所有应用
        List<ApplicationInfo> packList = packageManager.getInstalledApplications(0);
        int i = 0;
        int len = packList.size();
        while (i < len){
            ApplicationInfo pak = packList.get(i);
            //if()里的值如果<=0则为自己装的程序,否则为系统工厂自带
            if ((pak.flags & ApplicationInfo.FLAG_SYSTEM )<= 0){
                //添加自己安装的应用程序
                apps.add(pak.packageName);
                Log.i("yangxin", "getAllApps: 自己安装的 = " + pak.packageName);
            }else {
                Log.i("yangxin", "getAllApps: 系统自带的 = " + pak.packageName);
            }
            i++;
        }
        return apps;
    }

    /*
    * 获取所有应用中被选中过的应用*/
    public static List<ApplicationInfo>  getSelectedApps(Context context){
        List<ApplicationInfo> selectedApps = new ArrayList<>();
        SharedPreferences sp = context.getSharedPreferences("SelectedApps", Context.MODE_PRIVATE);
        PackageManager pm = context.getPackageManager();

        // 1. 读取有序的包名字符串
        String orderString = sp.getString("heat_set_order", "");
        if (TextUtils.isEmpty(orderString)) {
            // 兼容旧数据：没有顺序时，遍历所有 Boolean=true 的包名
            Map<String, ?> map = sp.getAll();
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                // 关键：只处理 Boolean 类型，跳过 heat_set_order 等 String 值
                if (value instanceof Boolean && (Boolean) value) {
                    try {
                        ApplicationInfo info = pm.getApplicationInfo(entry.getKey(), 0);
                        selectedApps.add(info);
                    } catch (Exception e) {}
                }
            }
            return selectedApps;
        }

        // 2. 按顺序拆分包名，逐个加载（保证界面顺序和日志一致）
        String[] packageNames = orderString.split(",");
        for (String pkg : packageNames) {
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                selectedApps.add(info);
            } catch (PackageManager.NameNotFoundException e) {
                // 应用卸载了，跳过
                sp.edit().remove(pkg).apply();
            }
        }
        if (selectedApps.size() > 5) {
            return selectedApps.subList(0, 5);
        }
        return selectedApps;
    }

    public static String getTimeFormat(Context context){
        //获取当前local
        Locale locale = Locale.getDefault();

        //检查是否是24小时制
        if (DateFormat.is24HourFormat(context)){
            return "HH:mm";
        } else {
            return "hh:mm a";
        }
    }

    public   boolean isUsbPlugged(Context context){
        UsbManager usbManager =(UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList =usbManager.getDeviceList();
        for (Map.Entry<String,UsbDevice> entry:deviceList.entrySet()){
            UsbDevice device = entry.getValue();
            //检测设备是否已经挂载到USB设备
            return usbManager.hasPermission(device);
        }
        return false;
    }

    public static String readSystemProp(String search_line) {
        String line = "";
        try {
            File file = new File("/system/etc/settings.ini");
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            while ((line = reader.readLine()) != null) {
                if (line.contains(search_line)) {
                    // 这里可以进一步解析line来获取STORAGE_SIMPLE_SYSDATA的值
                    String value = line.split("=")[1].trim(); // 获取等号后面的值
                    reader.close();
                    fis.close();
                    return value;
                }
            }
            reader.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Standard";
    }

    public static String getProperty(String key, String defaultValue) {
        String value = defaultValue;
        try{
            Class<?> c = Class.forName(CLASS_NAME);
            Method get = c.getMethod("get",String.class, String.class);
            value = (String)(get.invoke(c,key,defaultValue));
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            return value;
        }
    }

    public static void setProperty(String key,String value){
        try{
            Class<?> c = Class.forName(CLASS_NAME);
            Method set = c.getMethod("set",String.class,String.class);
            set.invoke(c,key,value);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static boolean execCommand(String... command) {

        Process process = null;

        InputStream errIs = null;

        InputStream inIs = null;

        boolean result ;

        try {
            process = new ProcessBuilder().command(command).start();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read = -1;

            errIs = process.getErrorStream();
            while ((read = errIs.read()) != -1) {
                baos.write(read);
            }

            inIs = process.getInputStream();
            while ((read = inIs.read()) != -1) {
                baos.write(read);
            }
            result = true;
            inIs.close();
            errIs.close();
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    public static boolean checkAndInstallApk(String apkFilePath){
        File apkFile = new File(apkFilePath);
        boolean result;
        if (apkFile.exists()){
            Log.d("ApkInstaller", "APK文件存在，准备安装");
            // 根据不同的Android系统版本选择不同的安装方式
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                // Android 7.0及以上需要使用FileProvider等方式处理文件Uri，这里简化示例，可完善
                // 示例中暂时使用命令行方式（命令行方式在实际中可能存在兼容性等问题，也可改为合适的Intent方式）
                String[] command = {"pm","install","-f",apkFilePath};
                result = execCommand(command);
                Log.d("ApkInstaller", "7.0以上安装结束，结果是"+result);
            }else {
                // Android 7.0以下可直接使用如下方式（更简单的命令行示例，实际中推荐用Intent方式安装）
                String[] command = {"pm", "install", apkFilePath};
                result = execCommand(command);
                Log.d("ApkInstaller", "7.0以下安装结果: " + result);
            }
            return result;
        }else {
            Log.e("ApkInstaller", "指定的APK文件不存在");
            return false;
        }
    }

    public static String getUsbFilePath(Context context){
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        if (storageManager != null){
            Log.i("yangxin", "storageManager不为空.");
            StorageVolume[] storageVolumes = storageManager.getStorageVolumes().toArray(new StorageVolume[0]);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {//Android11以下的用反射
                for (StorageVolume volume : storageVolumes){
                    Log.i("yangxin", "Volume is removable: " + volume.isRemovable()+",name = "+ Objects.requireNonNull(volume.getDirectory()).getAbsolutePath());
                    Log.i("yangxin", "Volume is primary: " + volume.isPrimary()+",name = "+volume.getDirectory().getAbsolutePath());
                    if (volume.isRemovable() && !volume.isPrimary()){
                        Log.i("yangxin", "getUsbFilePath找到可移动并且不是主存储.");
                        Log.i("yangxin", "getUsbFilePath进入最后一关.");
                        return volume.getDirectory().getAbsolutePath();
                    }
                }
            }

        }else {
            Log.i("yangxin", "storageManager是空的.");
        }
        return "未挂载";//未挂载
    }

    public static String getInstallTag(){
        String installTag = "";
        try{
            File file = new File(usbFilePath+"/testInfo.txt");
            if (!file.exists()) {
                Log.e("yangxin", "File does not exist.");
            }
            FileInputStream fis = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String line;
            while ((line = br.readLine()) != null){
                if (line.contains("cmd_install_path")){
                    Log.i("yangxin", "读到有属性cmd_install_path.");
                    String[] parts = line.split("：");
                    if (parts.length == 2) {
                        Log.i("yangxin", "cmd_install_path长度为2，取第二个值.");
                        installTag = parts[1].trim();
                    }
                }
            }
            br.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        Log.d("yangxin", "getInstallTag: installTag = " + installTag );
        return installTag;
    }

    //获取apk的包名
    public static String getApkPackageName(PackageManager packageManager,String apkFilePath){
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(apkFilePath,PackageManager.GET_ACTIVITIES);
        if (packageInfo != null){
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            appInfo.sourceDir = apkFilePath;
            appInfo.publicSourceDir = apkFilePath;
            return appInfo.packageName;
        }
        return null;
    }


    protected void isMacVerify(){
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String macAddress = wifiInfo.getMacAddress();
        if(macAddress!=null){
            macAddress =macAddress.toUpperCase();
            Log.i("yangxin", "isMacVerify: MAC ="+macAddress);
            String[] parts = macAddress.split(":");
            if (parts.length >= 3 && parts[0].equals("76") && parts[1].equals("AF") && parts[2].equals("F7")){
                Log.i("yangxin", "isMacVerify: MAC合法");
            }else {
                String verify = readSystemProp("MAC_VALID_CHECK");
                Log.i("yangxin", "isMacVerify: verify = "+ verify);
                if (verify.equals("true")){
                    Log.i("yangxin", "isMacVerify: 显示错误窗口");
                    showMacErrorDialog("MAC ERROR");
                }
            }
        }
    }

    private  void  showMacErrorDialog(String message){
        Dialog dialog = new Dialog(context, android.R.style.Theme_Material_Light_Dialog_Alert);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 加载自定义布局
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_layout, null);
        dialog.setContentView(view);

        TextView tvMessage = view.findViewById(R.id.error_message);
        tvMessage.setText(message);

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;  // 宽度包裹内容
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT; // 高度包裹内容
            layoutParams.gravity = Gravity.CENTER;  // 居中显示
            window.setAttributes(layoutParams);
        }

        // 显示对话框
        dialog.show();
    }

    protected void hideSystemUI(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * 初始化时从 /system/etc/settings.ini 读取预设的 HEATSET 应用
     * 行格式：HEATSET=com.xxx.yyy  （或 HEATSET1=...、HEATSET_xxx=...，只要以 HEATSET 开头）
     * 解析出的包名写入 SharedPreferences("SelectedApps")，并生成 heat_set_order 顺序
     */
    public static void initSelectedAppsFromSettingsIni(Context context) {
        final String INI_PATH = "/system/etc/settings.ini";
        final String KEY_PREFIX = "HEATSET";
        final String SP_NAME = "SelectedApps";
        final String ORDER_KEY = "heat_set_order";
        // 用于记录「是否已初始化过」的独立 SP，避免和 SelectedApps 混在一起
        final String FLAG_SP = "app_init_flag";
        final String FLAG_KEY = "heatset_inited";

        // ===== 只允许执行一次 =====
        SharedPreferences flagSp = context.getSharedPreferences(FLAG_SP, Context.MODE_PRIVATE);
        if (flagSp.getBoolean(FLAG_KEY, false)) {
            Log.i("yangxin", "initSelectedAppsFromSettingsIni: 已初始化过，跳过");
            return;
        }
        File file = new File(INI_PATH);
        if (!file.exists()) {
            Log.w("yangxin", "initSelectedAppsFromSettingsIni: " + INI_PATH + " 不存在，跳过");
            return;
        }

        // 按顺序收集包名
        List<String> packageNames = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // 跳过空行和注释
                }
                // 必须以 HEATSET 开头
                if (!line.startsWith(KEY_PREFIX)) {
                    continue;
                }
                int eqIndex = line.indexOf('=');
                if (eqIndex < 0 || eqIndex == line.length() - 1) {
                    continue;
                }
                // key 部分（等号前）也必须以 HEATSET 开头，防止误匹配
                String key = line.substring(0, eqIndex).trim();
                if (!key.startsWith(KEY_PREFIX)) {
                    continue;
                }
                String value = line.substring(eqIndex + 1).trim();
                if (value.isEmpty()) {
                    continue;
                }
                packageNames.add(value);
                Log.i("yangxin", "initSelectedAppsFromSettingsIni: 读取到 " + key + " = " + value);
            }
        } catch (Exception e) {
            Log.e("yangxin", "initSelectedAppsFromSettingsIni: 读取失败", e);
            return;
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) {}
            }
        }

        if (packageNames.isEmpty()) {
            Log.i("yangxin", "initSelectedAppsFromSettingsIni: 没有找到 HEATSET 配置，跳过");
            return;
        }

        // 写入 SharedPreferences
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        // 1. 写入每个包名为 true（兼容 getSelectedApps 的旧逻辑）
        for (String pkg : packageNames) {
            editor.putBoolean(pkg, true);
        }

        // 2. 写入顺序（getSelectedApps 优先读取该 key 保证界面顺序）
        editor.putString(ORDER_KEY, TextUtils.join(",", packageNames));

        editor.apply(); // 或 commit() 如果需要在 initView 中立即同步生效
        editor.commit();

        // ===== 标记已初始化 =====
        flagSp.edit().putBoolean(FLAG_KEY, true).commit();
        Log.i("yangxin", "initSelectedAppsFromSettingsIni: 共初始化 " + packageNames.size() + " 个应用");
    }
}
