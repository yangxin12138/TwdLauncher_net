package com.twd.twdlaunchernet;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.twd.twdlaunchernet.adapter.ApplicationAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ApplicationActivity extends AppCompatActivity {
    GridView gridView ;
    ApplicationAdapter adapter;
    SharedPreferences sharedPreferences;
    int listMode = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application);
        Intent intent = getIntent();
        if (intent != null){
            listMode = intent.getIntExtra("list_mode",0);
        }
        sharedPreferences = getSharedPreferences("SelectedApps", Context.MODE_PRIVATE);
        initView();
    }

    private void initView(){
        gridView = findViewById(R.id.gridView);
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN,null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> installedApps = pm.queryIntentActivities(intent,0);

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
                    String appName = viewHold.tv_name.getText().toString();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    if (sharedPreferences.getBoolean(appName,false)){
                        //已经选过了，取消选中
                        viewHold.iv_red.setVisibility(View.GONE);
                        editor.putBoolean(appName,false);
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
                            editor.putBoolean(appName,true);
                        }else {
                            //提示用户最多只能选5个
                            Toast.makeText(getApplicationContext(), "最多只能选择7个应用", Toast.LENGTH_SHORT).show();
                        }
                    }
                    editor.apply();
                }
            }
        });
    }
}