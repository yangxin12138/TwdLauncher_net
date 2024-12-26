package com.twd.twdlaunchernet;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;

import java.io.File;

/**
 * @Author:Yangxin
 * @Description:
 * @time: Create in 14:13 2024/8/20
 */
public class UsbReceiver extends BroadcastReceiver {
    private Context mContext;
    AlertDialog alertDialog = null;
    SharedPreferences selectedPreferences;
    String UI_QUICKLINK_APP_PACKAGE = Utils.readSystemProp("UI_QUICKLINK_APP_PACKAGE");
    String UI_QUICKLINK_STYLE = Utils.readSystemProp("UI_QUICKLINK_STYLE");
    private Handler mHandler;
    String usbPath ;
    public UsbReceiver(Handler mainHandler) {
        mHandler = mainHandler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        mContext = context;
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)){
            Log.i("yangxin", "onReceive:usb u盘插入 ");
           //TODO:弹出确定弹窗
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            dialogBuilder.setMessage(mContext.getString(R.string.index_usb_listener));
            dialogBuilder.setPositiveButton(mContext.getString(R.string.application_confirm), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent_f1 = new Intent();
                    ComponentName cn_f1 = new ComponentName("com.softwinner.TvdFileManager","com.softwinner.TvdFileManager.MainUI");
                    intent_f1.setComponent(cn_f1);
                    intent_f1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent_f1.setAction("android.intent.action.MAIN");
                    mContext.startActivity(intent_f1);
                    alertDialog.dismiss();
                    Log.i("yangxin", "onClick: usb ---- 点击确定");
                }
            });
            dialogBuilder.setNegativeButton(mContext.getString(R.string.application_cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.i("yangxin", "onClick: usb ---- 点击取消");
                    alertDialog.dismiss();
                }
            });
            alertDialog = dialogBuilder.create();
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            alertDialog.show();

/*            //TODO:检测U盘中的配置文件以及配置项
            usbPath = Utils.getUsbFilePath(mContext);
            Log.i("yangxin", "onReceive:usbPath =  " + usbPath);
            Utils.usbFilePath = usbPath;
            String installTag = Utils.getInstallTag();
            checkAndInstallApkFromUSB(installTag);*/
        }else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)){
            Log.i("yangxin", "onReceive:usb u盘拔出 ");
            if (alertDialog!=null && alertDialog.isShowing()){
                alertDialog.dismiss();
            }
        }
    }

    //TODO：先判断是否已经安装了指定包名的软件，如果没有安装就从u盘根目录寻找知道软件进行安装，并且通知主线程更新UI
    private void checkAndInstallApkFromUSB(String installTag){
        if (installTag.equals("APK_INSTALL_TWD")){
            Log.i("yangxin", "checkAndInstallApkFromUSB:installTag等于APK_INSTALL_TWD ");
            //配置了安装才安装,遍历这个目录下的apk文件进行安装
            String apkDir = usbPath + "/APK_INSTALL_TWD";
            File dir = new File(apkDir);
            //判断目录是否存在并且是一个目录
            if (dir.exists() && dir.isDirectory()){
                File[] files = dir.listFiles();
                if (files!=null){
                    for (File file : files){
                        if (file.isFile() && file.getName().toLowerCase().endsWith(".apk")){
                            String apkPath = apkDir + "/" + file.getName();
                            System.out.println(apkPath);
                        }
                    }
                }
            }
        }
    }

    private void handleApkResult(boolean apkExist){
        if (apkExist){ //应用存在并且安装成功
            Log.i("yangxin", "UsbReceiver: 应用存在并且安装成功");
            if (UI_QUICKLINK_STYLE.equals("true")) {
                selectedPreferences = mContext.getSharedPreferences("SelectedApps",Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = selectedPreferences.edit();
                editor.putBoolean(UI_QUICKLINK_APP_PACKAGE, true);
                editor.apply();
            }
            Message msg = mHandler.obtainMessage(2);
            mHandler.sendMessage(msg);
        }
    }

}
