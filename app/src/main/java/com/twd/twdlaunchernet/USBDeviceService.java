package com.twd.twdlaunchernet;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @Author:Yangxin
 * @Description:
 * @time: Create in 15:28 2024/8/20
 */
public class USBDeviceService extends Service {
    private BroadcastReceiver usbReceiver;
    private Context mContext;
    private final MainActivity mainActivity = new MainActivity();


    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        usbReceiver = new UsbReceiver(mainActivity.mainHandler);
        Log.d("USBDeviceService", "服务启动");
        // 在这里可以执行一些持续的操作，比如持续监听 U 盘状态变化等
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver,filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Utils utils = new Utils(mContext);
        //TODO:启动定时器，每隔一段时间检查U盘状态
        /*new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
               boolean isConnect =  utils.isUsbPlugged(mContext);
               if (isConnect){
                   Log.i("USBDeviceService","定时器 u盘已插入");
               }else {
                   Log.i("USBDeviceService","定时器 未插入");
               }
            }
        },0,1000);*/
        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (usbReceiver!= null) {
            unregisterReceiver(usbReceiver);
        }
    }
}
