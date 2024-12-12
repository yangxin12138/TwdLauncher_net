package com.twd.twdlaunchernet.application;

import android.app.Application;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author:Yangxin
 * @Description:
 * @time: Create in 上午11:19 11/12/2024
 */
public class HandlerApplication extends Application {
    private Handler mainHandler;
    private List<String> failedApkList = new ArrayList<>();

    public Handler getMainHandler(){
        return mainHandler;
    }
    public void setMainHandler(Handler handler){
        this.mainHandler = handler;
    }

    public List<String> getFailedApkList() {
        return failedApkList;
    }

    public void setFailedApkList(List<String> failedApkList) {
        this.failedApkList = failedApkList;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
