package com.sarahmizzi.singlesignonazureservicesdemo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;

import java.net.MalformedURLException;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";
    public MobileServiceClient mClient;
    CallbackManager callbackManager;
    MobileServiceTable<UserInformation> userInformationTable;
    UserInformation item;

    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setVisibility(View.GONE);

        try {
            mClient = new MobileServiceClient(
                    "https://singlesignonazuredemo.azure-mobile.net/",
                    "kYdexwFIuHLTlQXxNAbvrdhyOhRthu79",
                    this);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        callbackManager = CallbackManager.Factory.create();

        Button signInFB = (Button) findViewById(R.id.sign_in_button);
        signInFB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // If the user is already logged in
                if (loadUserTokenCache(mClient)) {
                    //TODO: Go to loggedIn Activity
                }
                //Perform login
                else {
                    ListenableFuture<MobileServiceUser> mLogin = mClient.login(MobileServiceAuthenticationProvider.Facebook);

                    Futures.addCallback(mLogin, new FutureCallback<MobileServiceUser>() {
                        @Override
                        public void onFailure(Throwable exc) {
                            Log.e(TAG, "Error" + exc.toString());
                        }

                        @Override
                        public void onSuccess(MobileServiceUser user) {
                            Toast.makeText(getApplicationContext(), "Login Successfully", Toast.LENGTH_LONG).show();

                            //Cache the user token
                            cacheUserToken(mClient.getCurrentUser());
                        }

                    });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    //FIXME This method was done just to check that the data is being stored in the Azure db
    private void createTable(String email, String userBirthday, String publicProfile) {
        //Get the Mobile Service instance to use
        userInformationTable = mClient.getTable(UserInformation.class);

        item = new UserInformation();
        item.email = email;
        item.user_birthday = userBirthday;
        item.public_profile = publicProfile;

        //insert new items in the table
        mClient.getTable(UserInformation.class).insert(item, new TableOperationCallback<UserInformation>() {
            public void onCompleted(UserInformation entity, Exception exception, ServiceFilterResponse response) {
                if (exception == null) {
                    // Insert succeeded
                    Log.e(TAG, "Insert Succeeded");
                } else {
                    // Insert failed
                    Log.e(TAG, "Insert Failed");
                }
            }
        });


    }

    /**
     * This method stores the user id and token in a preference file that is marked private.
     *
     * @param user
     */
    private void cacheUserToken(MobileServiceUser user)
    {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, user.getAuthenticationToken());
        editor.commit();
    }

    private boolean loadUserTokenCache(MobileServiceClient client)
    {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        String userId = prefs.getString(USERIDPREF, "undefined");
        if (userId == "undefined")
            return false;
        String token = prefs.getString(TOKENPREF, "undefined");
        if (token == "undefined")
            return false;

        MobileServiceUser user = new MobileServiceUser(userId);
        user.setAuthenticationToken(token);
        client.setCurrentUser(user);

        return true;
    }
}
