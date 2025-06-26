package com.twd.twdlaunchernet;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.twd.twdlaunchernet.adapter.ApplicationAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApplicationActivity extends AppCompatActivity {
    GridView gridView ;
    ApplicationAdapter adapter;
    SharedPreferences sharedPreferences;
    int listMode = 0;
    int MyPosition ;
    Context context;
    Utils utils;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

    private void initView(){
        gridView = findViewById(R.id.gridView);
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

        Log.i("yangxin", "initView: -------初始化已安装应用------");
        //创建一个迭代器用于遍历installedApps列表
        Iterator<ResolveInfo> iterator = combinedList.iterator();

        //遍历installedApps列表，过滤应用
        while (iterator.hasNext()){
            ResolveInfo resolveInfo = iterator.next();
            String packageName = resolveInfo.activityInfo.packageName;
            if ("com.twd.twdlaunchernet".equals(packageName)  || "com.android.tv.settings".equals(packageName)){
                iterator.remove();//移除
            }
        }
        adapter = new ApplicationAdapter(this,combinedList,listMode);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("yangxin", "onItemClick: mode = " + listMode);
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
                if (count % (MyPosition +1) == 0){
                    Log.i("yangxin", "onKeyDown: 是最后一个，不予处理 ");
                    super.onKeyDown(keyCode, event);
                    break;
                }
                Log.i("yangxin", "onKeyDown: 不是最后一个，处理position ");
                gridView.setSelection(gridView.getSelectedItemPosition() + 1);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Log.i("yangxin", "onKeyDown: 方向左键被按下 gridView.getSelectedItemPosition() = " + gridView.getSelectedItemPosition());
                if (MyPosition == 0){
                    Log.i("yangxin", "onKeyDown: 是第一个，不予处理 ");
                    super.onKeyDown(keyCode, event);
                    break;
                }
                gridView.setSelection(gridView.getSelectedItemPosition() - 1);
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
}