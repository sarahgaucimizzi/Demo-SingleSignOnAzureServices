package com.sarahmizzi.singlesignonazureservicesdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.facebook.CallbackManager;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;

import java.net.MalformedURLException;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";
    private MobileServiceClient mClient;
    CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setVisibility(View.GONE);

        try {
            mClient = new MobileServiceClient(
                    "https://singlesignonazuredemo.azurewebsites.net",
                    "088c3c25cb7f4ce5b5cc4093add357f6",
                    this);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        callbackManager = CallbackManager.Factory.create();

        Button signInFB = (Button) findViewById(R.id.sign_in_button);
        signInFB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*LoginManager.getInstance().registerCallback(callbackManager,
                        new FacebookCallback<LoginResult>() {
                            @Override
                            public void onSuccess(LoginResult loginResult) {
                                // App code
                                Log.d(TAG, "Login Successful");
                            }

                            @Override
                            public void onCancel() {
                                // App code
                                Log.e(TAG, "Cancelled");
                            }

                            @Override
                            public void onError(FacebookException exception) {
                                // App code
                                Log.e(TAG, "Error: " + exception.toString());
                            }
                        });*/

                ListenableFuture<MobileServiceUser> mLogin = mClient.login(MobileServiceAuthenticationProvider.Google);

                Futures.addCallback(mLogin, new FutureCallback<MobileServiceUser>() {
                    @Override
                    public void onFailure(Throwable exc) {
                        Log.e(TAG, "Error" + exc.toString());
                    }

                    @Override
                    public void onSuccess(MobileServiceUser user) {
                        Log.d(TAG, "Successful");
                        // TODO: createTable(); i.e. save user
                    }
                });
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
