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

public class ApplicationActivity extends AppCompatActivity {
    GridView gridView ;
    ApplicationAdapter adapter;
    SharedPreferences sharedPreferences;
    int listMode = 0;
    int MyPosition ;
    Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application);
        context = this;
        Intent intent = getIntent();
        if (intent != null){
            listMode = intent.getIntExtra("list_mode",0);
        }
        sharedPreferences = getSharedPreferences("SelectedApps", Context.MODE_PRIVATE);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initView();
    }

    private void initView(){
        gridView = findViewById(R.id.gridView);
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN,null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> installedApps = pm.queryIntentActivities(intent,0);
        Log.i("yangxin", "initView: -------初始化已安装应用------");
        //创建一个迭代器用于遍历installedApps列表
        Iterator<ResolveInfo> iterator = installedApps.iterator();

        //遍历installedApps列表，过滤应用
        while (iterator.hasNext()){
            ResolveInfo resolveInfo = iterator.next();
            String packageName = resolveInfo.activityInfo.packageName;
            if ("com.twd.twdlaunchernet".equals(packageName) || "com.twd.ipdemo".equals(packageName)){
                iterator.remove();//移除
            }
        }
        adapter = new ApplicationAdapter(this,installedApps,listMode);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("yangxin", "onItemClick: mode = " + listMode);
                if(listMode == 1){
                     ResolveInfo app = installedApps.get(position);
                        String packageName = app.activityInfo.packageName;
                        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                        if (launchIntent != null){
                            startActivity(launchIntent);
                        }else {
                            // 应用程序没有启动Intent
                            Log.e("ApplicationAdapter", "Unable to launch app");
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
                            Toast.makeText(getApplicationContext(), "最多只能选择7个应用", Toast.LENGTH_SHORT).show();
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
                    }else if(packageName.equals("com.netflix.mediaclient") || packageName.equals("com.netflix.ninja")){
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