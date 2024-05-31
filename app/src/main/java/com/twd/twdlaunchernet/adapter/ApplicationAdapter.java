package com.twd.twdlaunchernet.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.twd.twdlaunchernet.R;

import java.util.List;

/**
 * @Author:Yangxin
 * @Description:
 * @time: Create in 16:55 2024/5/21
 */
public class ApplicationAdapter extends BaseAdapter {

    private Context context;
    private List<ResolveInfo> appList;
    private int list_mode;
    public ApplicationAdapter(Context context, List<ResolveInfo> appList,int list_mode) {
        this.context = context;
        this.appList = appList;
        this.list_mode = list_mode;
    }

    @Override
    public int getCount() {
        return appList.size();
    }

    @Override
    public Object getItem(int position) {
        return appList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ResolveInfo app = appList.get(position);
        PackageManager manager = context.getPackageManager();
        ViewHold viewHold = null;
        if (convertView == null){
            convertView = View.inflate(context,R.layout.index_item_layout,null);
            viewHold = new ViewHold();
            viewHold.tv_name = convertView.findViewById(R.id.appName);
            viewHold.iv_icon = convertView.findViewById(R.id.appIcon);
            viewHold.iv_red = convertView.findViewById(R.id.iv_red);
            convertView.setTag(viewHold);
        }else {
            viewHold = (ViewHold) convertView.getTag();
        }
        viewHold.packageName = app.activityInfo.packageName;

        try{
            ApplicationInfo applicationInfo = manager.getApplicationInfo(viewHold.packageName,0);
            String appName = (String) applicationInfo.loadLabel(manager);
            viewHold.tv_name.setText(appName);
            viewHold.iv_icon.setImageDrawable(app.activityInfo.loadIcon(manager));
            Log.i("yangxin", "getView: list_mode =" +list_mode);
            if (list_mode == 1){
                //启动应用，这时候不需要显示红点 全部隐藏
                viewHold.iv_red.setVisibility(View.GONE);
            }else if (list_mode == 2){
                //添加应用到主菜单，此时需要区分红点
                // 根据 SharedPreferences 中的数据来显示或隐藏红点
                SharedPreferences selectedPreferences = context.getSharedPreferences("SelectedApps", Context.MODE_PRIVATE);
                boolean isSelected = selectedPreferences.getBoolean(viewHold.packageName,false);
                viewHold.iv_red.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            }
        }catch (PackageManager.NameNotFoundException e){e.printStackTrace();}

        return convertView;
    }

    public class ViewHold {
        public void setTv_name(TextView tv_name) {
            this.tv_name = tv_name;
        }

        public TextView tv_name;
        public ImageView iv_icon;
        public ImageView iv_red;
        public String packageName;
    }
}
