package com.twd.twdlaunchernet;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.content.IntentSender;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.twd.twdlaunchernet.application.HandlerApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ApkInstallReceiver extends BroadcastReceiver {
    private static final String APK_BASE_DIR = "./system/operator/preinstall";
    private static final String TAG = "ApkInstallReceiver";
    private static final String ACTION_INSTALL_RESULT = "com.twd.twdlaunchernet.INSTALL_RESULT";
    @Override
    public void onReceive(Context context, Intent intent) {
        Context globalContext = HandlerApplication.getGlobalContext();
        String action = intent.getAction();
        if ("com.example.SYSTEM_BROADCAST_INSTALL_APK".equals(action)) {
            String targetPackageName = intent.getStringExtra("TARGET_PACKAGE_NAME");
            if (targetPackageName == null || targetPackageName.isEmpty()) {
                Log.e(TAG, "安装失败：广播未携带目标包名");
                return;
            }
            Log.i(TAG, "接收到安装指令，目标包名=" + targetPackageName);

            String apkPath = findApkByPackageName(globalContext, APK_BASE_DIR, targetPackageName);
            if (apkPath == null) {
                Log.e(TAG, "安装失败：未在" + APK_BASE_DIR + "找到包名为" + targetPackageName + "的APK");
                return;
            }
            // 执行安装
            installApk(globalContext, apkPath, targetPackageName);
        }else if (ACTION_INSTALL_RESULT.equals(action)) {
            handleInstallResult(intent); // 处理安装结果
        }
    }
    private String findApkByPackageName(Context context, String dirPath, String targetPackageName) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            Log.e(TAG, "APK目录不存在或不是文件夹：" + dirPath);
            return null;
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            Log.e(TAG, "APK目录为空：" + dirPath);
            return null;
        }

        PackageManager packageManager = context.getPackageManager();
        // 遍历目录下所有APK，匹配包名
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".apk")) {
                String apkPackageName = Utils.getApkPackageName(packageManager, file.getAbsolutePath());
                if (targetPackageName.equals(apkPackageName)) {
                    Log.i(TAG, "找到匹配的APK：" + file.getAbsolutePath());
                    return file.getAbsolutePath();
                }
            }
        }
        return null;
    }

    private void installApk(Context context, String apkPath, String targetPackageName) {
        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            Log.e(TAG, "安装失败：APK文件不存在，路径=" + apkPath);
            return;
        }

        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        try {
            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);

            // 写入APK文件到安装会话
            OutputStream outputStream = session.openWrite("apk_install", 0, -1);
            FileInputStream inputStream = new FileInputStream(apkFile);
            byte[] buffer = new byte[1024 * 1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            session.fsync(outputStream);
            inputStream.close();
            outputStream.close();

            // 构建安装结果回调（携带包名，方便后续固定）
            Intent resultIntent = new Intent(ACTION_INSTALL_RESULT);
            resultIntent.putExtra("sessionId", sessionId);
            resultIntent.putExtra("targetPackageName", targetPackageName); // 携带目标包名
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    resultIntent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : PendingIntent.FLAG_UPDATE_CURRENT
            );

            // 提交安装
            session.commit(pendingIntent.getIntentSender());
            session.close();
            Log.i(TAG, "APK安装请求已提交，会话ID=" + sessionId + "，包名=" + targetPackageName);

        } catch (IOException e) {
            Log.e(TAG, "安装失败：文件写入异常", e); // 日志：异常堆栈
        }
    }

    // 处理安装结果（Logcat输出 + 成功后固定到heat_set）
    private void handleInstallResult(Intent intent) {
        int sessionId = intent.getIntExtra("sessionId", -1);
        String targetPackageName = intent.getStringExtra("targetPackageName");
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);

        // 1. Logcat输出安装结果
        if (status == PackageInstaller.STATUS_SUCCESS) {
            Log.i(TAG, "安装成功！会话ID=" + sessionId + "，包名=" + targetPackageName);
            // 2. 安装成功：固定到heat_set区域（核心逻辑，完全适配你的代码）
            fixToHeatSet(HandlerApplication.getGlobalContext(), targetPackageName);
        } else {
            Log.e(TAG, "安装失败！会话ID=" + sessionId + "，包名=" + targetPackageName + "，状态码=" + status + "，原因=" + message);
            // 可选：记录安装失败的包名（复用你MainActivity的失败列表逻辑）
            List<String> failedApkList = ((HandlerApplication) HandlerApplication.getGlobalContext().getApplicationContext()).getFailedApkList();
            if (failedApkList == null) {
                failedApkList = new ArrayList<>();
            }
            failedApkList.add(targetPackageName);
            ((HandlerApplication) HandlerApplication.getGlobalContext().getApplicationContext()).setFailedApkList(failedApkList);
        }
    }

    private void fixToHeatSet(Context context, String packageName) {
        try {
            // 步骤1：校验应用是否已安装（避免空操作）
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            if (packageInfo == null) {
                Log.w(TAG, "固定到heat_set失败：应用未安装，包名=" + packageName);
                return;
            }

            SharedPreferences selectedPreferences = context.getSharedPreferences("SelectedApps", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = selectedPreferences.edit();
            Map<String, ?> allEntries = selectedPreferences.getAll();
            LinkedList<String> heatSetList = new LinkedList<>();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                if (entry.getValue() instanceof Boolean && (Boolean) entry.getValue()) {
                    heatSetList.add(entry.getKey());
                }
            }
            Log.i(TAG, "读取到原有heat_set列表：" + heatSetList.toString());
            if (heatSetList.contains(packageName)) {
                heatSetList.remove(packageName);
                Log.i(TAG, "应用已在heat_set中，先移除再置顶：" + packageName);
            }
            heatSetList.addFirst(packageName);
            // 步骤4：超过7个，删除最后一个
            if (heatSetList.size() > 7) {
                String removed = heatSetList.removeLast();
                Log.i(TAG, "heat_set已满7个，删除最后一个应用：" + removed);
            }

            // 步骤5：清空旧数据，重新保存新列表（关键）
            editor.clear();
            for (String pkg : heatSetList) {
                editor.putBoolean(pkg, true);
            }
            String orderString = TextUtils.join(",", heatSetList);
            editor.putString("heat_set_order", orderString);
            editor.apply();

            Log.i(TAG, "heat_set处理完成，最新列表：" + heatSetList.toString());

            // 步骤6：通知MainActivity刷新
            Handler mainHandler = ((HandlerApplication) context.getApplicationContext()).getMainHandler();
            if (mainHandler != null) {
                mainHandler.sendEmptyMessage(1);
                Log.i(TAG, "已通知首页刷新heat_set");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "固定到heat_set失败：应用包名不存在", e);
        } catch (Exception e) {
            Log.e(TAG, "固定到heat_set失败：未知异常", e);
        }
    }
}