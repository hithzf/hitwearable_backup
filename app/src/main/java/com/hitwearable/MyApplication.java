package com.hitwearable;

import android.app.Application;
import android.content.Context;

import org.litepal.LitePalApplication;

/**
 * 用于全局获取Context
 * Created by hzf on 2017/5/19.
 */

public class MyApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        context = getApplicationContext();
        LitePalApplication.initialize(context);
    }

    public static Context getContext(){
        return context;
    }
}
