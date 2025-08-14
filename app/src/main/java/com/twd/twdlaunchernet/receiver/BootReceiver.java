package com.twd.twdlaunchernet.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.twd.twdlaunchernet.Utils;

/**
 * @Author:Yangxin
 * @Description:
 * @time: Create in 上午11:22 13/8/2025
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        String packageName = Utils.readSystemProp("BOOT_LAUNCH_PACKAGE");
        String className = Utils.readSystemProp("BOOT_LAUNCH_CLASS");
        String boot_flag = Utils.getProperty("persist.sys.boot.app","0");
        Log.d(TAG, "onReceive: packageName = "+packageName+",className = " +className);
        if (boot_flag.equals("0")){
            Log.d(TAG, "onReceive: 快速开启关闭");
            return;
        }
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            try{
                Intent launcherIntent = new Intent();
                launcherIntent.setComponent(new ComponentName(packageName,className));
                launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 广播中启动Activity需加此标志
                context.startActivity(launcherIntent);
                Log.d(TAG, "onReceive: 开启成功");
            }catch (Exception e){
                e.printStackTrace();
                Log.d(TAG, "onReceive: 开启失败，包名类名错误，或者没有这个应用");
            }

        }
    }
}
