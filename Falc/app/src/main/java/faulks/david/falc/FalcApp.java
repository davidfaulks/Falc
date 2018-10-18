package faulks.david.falc;

import android.app.Application;
import android.content.Context;

public class FalcApp extends Application {

    private static FalcApp singleton;

    @Override public void onCreate() {
        super.onCreate();
        singleton = this;
        FormatStore.getInstance();
    }

    public static Context getAppContext() {
        return singleton.getApplicationContext();
    }
}
