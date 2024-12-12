package com.twd.twdlaunchernet;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.format.DateFormat;
import android.util.Log;

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
        SharedPreferences sharedPreferences = context.getSharedPreferences("SelectedApps", Context.MODE_PRIVATE);
        List<ApplicationInfo> selectedApplist = new ArrayList<>();
        Map<String,?> allEntries = sharedPreferences.getAll();
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> appList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);


        for (Map.Entry<String,?> entry : allEntries.entrySet()){
            if (entry.getValue() instanceof Boolean){
                Boolean value = (Boolean) entry.getValue();
                if (value){
                    String packageName  = entry.getKey();
                    try {
                        ApplicationInfo appInfo = null;
                        for (ApplicationInfo info : appList){
                            if (packageName.equals(info.packageName)){
                                appInfo = info;
                                break;
                            }
                        }
                        if (appInfo != null){
                            selectedApplist.add(appInfo);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
        return selectedApplist;
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
}
