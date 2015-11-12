package com.sarahmizzi.singlesignonazureservicesdemo;

import com.facebook.FacebookSdk;

/**
 * Created by Sarah on 12-Nov-15.
 */
public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialise Facebook SDK
        FacebookSdk.sdkInitialize(getApplicationContext());
    }
}
