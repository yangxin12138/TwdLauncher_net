package com.twd.twdlaunchernet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.twd.twdlaunchernet.application.HandlerApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author:Yangxin
 * @Description:
 * @time: Create in 下午5:35 10/12/2024
 */
public class UsbApkReceiver extends BroadcastReceiver {
    String usbPath ;
    private Context mContext;
    private AtomicInteger apkCount =  new AtomicInteger(0);;//记录apk文件总数
    private AtomicInteger installedCount = new AtomicInteger(0);//记录已经安装完成的apk文件总数
    private List<String> failedApkList = new ArrayList<>();
    private ExecutorService executorService;
    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        String action = intent.getAction();
        HandlerApplication application = (HandlerApplication)context.getApplicationContext();
        Handler mainHandler = application.getMainHandler();
        executorService = Executors.newSingleThreadExecutor();
        Log.i("UsbApkReceiver", "onReceive: 接收到广播");
        if (action!= null && action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            Log.i("UsbApkReceiver", "onReceive: 进入到U盘挂载");
            usbPath = Utils.getUsbFilePath(mContext);
            Utils.usbFilePath = usbPath;
            String installTag = Utils.getInstallTag();
            checkAndInstallApkFromUSB(installTag,mainHandler);
        }
    }

    private void checkAndInstallApkFromUSB(String installTag,Handler mainHandler){
        if (installTag.equals("APK_INSTALL_TWD")){
            Log.i("UsbApkReceiver", "checkAndInstallApkFromUSB:installTag等于APK_INSTALL_TWD ");
            //配置了安装才安装,遍历这个目录下的apk文件进行安装
            String apkDir = usbPath + "/APK_INSTALL_TWD";
            File dir = new File(apkDir);
            //判断目录是否存在并且是一个目录
            if (dir.exists() && dir.isDirectory()){
                Log.i("UsbApkReceiver", "checkAndInstallApkFromUSB: 目录存在并且是一个目录");
                File[] files = dir.listFiles();
                if (files!=null){
                    apkCount.set(0); installedCount.set(0);
                    int apkFileCount = 0;
                    for (File file : files) {
                        if (file.isFile() && file.getName().toLowerCase().endsWith(".apk")) {
                            apkFileCount++;
                        }
                    }
                    apkCount.set(apkFileCount); // 赋值给apkCount
                    for (File file : files){
                        if (file.isFile() && file.getName().toLowerCase().endsWith(".apk")){
                            executorService.submit(() -> createApkInstallThread(file,apkDir,mainHandler).start());
                        }
                    }
                }
            }
        }
    }

    private Thread createApkInstallThread(File file,String apkDir,Handler mainHandler) {
        String apkPath = apkDir + "/" +file.getName();
        Log.i("UsbApkReceiver", "apkPath = " + apkPath);
        return new Thread(() ->{
            Log.i("UsbApkReceiver", "线程中开始安装");
            boolean isInstallSuccess  = Utils.checkAndInstallApk(apkPath);
            Log.i("UsbApkReceiver", file.getName()+",isInstallSuccess = " + isInstallSuccess);
            if (!isInstallSuccess){
                failedApkList.add(file.getName());
            }
            installedCount.incrementAndGet();
            Log.i("UsbApkReceiver", "installedCount = "+ installedCount.get()+",apkCount = "+apkCount.get());
            if (installedCount.get() == apkCount.get()){
                Log.i("UsbApkReceiver", "apk全部安装完成");
                HandlerApplication application = (HandlerApplication) mContext.getApplicationContext();
                application.setFailedApkList(failedApkList);
                Message msg = new Message();
                msg.what = 3;
                mainHandler.sendMessage(msg);
                if (executorService!= null) {executorService.shutdown();}
            }
        });
    }
}
