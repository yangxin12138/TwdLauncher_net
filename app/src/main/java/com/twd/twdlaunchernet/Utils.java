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
import android.text.format.DateFormat;
import android.util.Log;

import com.twd.twdlaunchernet.adapter.ApplicationAdapter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @Author:Yangxin
 * @Description:
 * @time: Create in 17:19 2024/5/20
 */
public class Utils {
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

    public static void execCommand(String packageName) {
        try{
            // 构建卸载命令
            String command = "pm uninstall " + packageName;
            // 使用Shell执行命令
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.flush();

            // 等待命令执行完成
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                // 命令执行成功
                // 可以在这里添加成功的逻辑
                Log.i("yangxin", "execCommand: 命令执行成功");
            } else {
                // 命令执行失败
                // 可以在这里添加失败的逻辑
                Log.i("yangxin", "execCommand: 命令执行失败");
            }
            os.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}
