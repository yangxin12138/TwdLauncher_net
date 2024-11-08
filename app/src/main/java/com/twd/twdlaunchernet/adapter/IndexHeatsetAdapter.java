package com.twd.twdlaunchernet.adapter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.twd.twdlaunchernet.ApplicationActivity;
import com.twd.twdlaunchernet.MainActivity;
import com.twd.twdlaunchernet.R;

import java.util.List;

/**
 * @Author:Yangxin
 * @Description:
 * @time: Create in 10:33 2024/5/23
 */
public class IndexHeatsetAdapter extends BaseAdapter {

    private Context mContext;
    private List<ApplicationInfo> mApplist;
    private LayoutInflater mInflater;
    private int selectionPosition = -1;
    public IndexHeatsetAdapter(Context context, List<ApplicationInfo> appList) {
        mContext = context;
        mApplist = appList;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mApplist.size() + 1;//添加一个固定的ImageView
    }

    @Override
    public Object getItem(int position) {
        return mApplist.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null){
            view = mInflater.inflate(R.layout.index_heat_item,parent,false);
        }

        ImageView icon = view.findViewById(R.id.appIcon);
        TextView name = view.findViewById(R.id.appName);
        LinearLayout LL_center = view.findViewById(R.id.LL_center);
        if (position < mApplist.size()){
            final  ApplicationInfo appInfo = mApplist.get(position);
            icon.setImageDrawable(appInfo.loadIcon(mContext.getPackageManager()));
            name.setText(appInfo.loadLabel(mContext.getPackageManager()));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("yangxin", "onClick: 点到其他app了");
                    selectionPosition = position;
                    Intent launchIntent = new Intent();
                    if (appInfo.packageName.equals("com.rockchips.mediacenter")){
                        launchIntent.setComponent(new ComponentName("com.rockchips.mediacenter", "com.rockchips.mediacenter.activity.MainActivity"));

                    }else {
                        launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(appInfo.packageName);
                    }
                    if (launchIntent != null){
                        mContext.startActivity(launchIntent);
                    }
                }
            });
        }else {
            //最后一个子项是固定的ImageView
            icon.setImageDrawable(mContext.getDrawable(R.drawable.index_add));
            name.setText(R.string.index_add_title);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("yangxin", "onClick: 点到加号了");
                    selectionPosition = position;
                    Intent intent = new Intent(mContext.getApplicationContext(), ApplicationActivity.class);
                    intent.putExtra("list_mode",2);
                    mContext.startActivity(intent);
                }
            });
        }

        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus){
                    MainActivity.isHeat = true;
                    ((MainActivity) mContext).onFocusChange(v,hasFocus);
                    LL_center.animate().scaleX(1.1f).scaleY(1.1f).translationZ(1f).setDuration(100);
                    LL_center.setForeground(mContext.getResources().getDrawable(R.drawable.border_white));
                    name.setSelected(true);
                    selectionPosition = position;
                }else {
                    MainActivity.isHeat = false;
                    ((MainActivity) mContext).onFocusChange(v,hasFocus);
                    LL_center.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(100);
                    LL_center.setForeground(null);
                    name.setSelected(false);
                }
            }
        });

        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.i("yangxin", "onKey: setOnKeyListener 触发==");
                if (position == 0){
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && event.getAction()==KeyEvent.ACTION_DOWN){
                        Log.i("yangxin", "onKey: -------执行requestFocus");
                        ((MainActivity) mContext).im_hdmi.requestFocus();
                        return true;
                    }
                }
                if (name.getText() == mContext.getString(R.string.index_add_title) ){
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.getAction()==KeyEvent.ACTION_DOWN){
                        Log.i("yangxin", "onKey: -------执行 -- 添加按右");
                        return true;
                    }
                }
                return false;
            }
        });
        return view;
    }

    public int getSelectionPosition(){
        return selectionPosition;
    }

    public void setSelectionPosition(int position){
        selectionPosition = position;
    }
}
