package com.sarahmizzi.singlesignonazureservicesdemo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.gson.JsonObject;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";
    private static JsonObject USER_FACEBOOK_TOKEN = null;
    private String AUTHORIZATION_TOKEN = "";

    final String TAG = "MainActivity";

    public MobileServiceClient mClient;
    CallbackManager callbackManager;
    MobileServiceTable<UserInformation> userInformationTable;
    UserInformation item;

    AccessToken accessToken;
    String name, email, birthDate;


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

        // If user is already logged in
        if (loadUserTokenCache(mClient)) {
            Intent intent = new Intent(MainActivity.this, LoggedInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }

        callbackManager = CallbackManager.Factory.create();

//        Button signInFB = (Button) findViewById(R.id.sign_in_button);
//
//        signInFB.setOnClickListener(new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            profile  = Profile.getCurrentProfile().getCurrentProfile();
//            if (profile == null) {
//                // Authenticate passing false to load the current token cache if available.
//                authenticate(false);
//            }
//            }
//        });

        final LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                String accessToken = loginResult.getAccessToken().getToken();
                USER_FACEBOOK_TOKEN = new JsonObject();
                USER_FACEBOOK_TOKEN.addProperty("Access token", accessToken);
                AUTHORIZATION_TOKEN = accessToken;

                //Set the values obtained to mClient
                mClient.setCurrentUser(new MobileServiceUser(loginResult.getAccessToken().getUserId()));
                //mClient.setCurrentUser(new MobileServiceUser(loginResult.getAccessToken().getToken()));
                setUpMobileServiceClient();
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException e) {

            }});
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
           super.onActivityResult(requestCode, resultCode, data);
            callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    public void setUpMobileServiceClient(){
       mClient.login(MobileServiceAuthenticationProvider.Facebook);

           cacheUserToken(mClient.getCurrentUser());
           getFacebookUserDetails();

           Intent intent = new Intent(MainActivity.this, LoggedInActivity.class);
           intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
           startActivity(intent);
           finish();
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
    private void cacheUserToken(MobileServiceUser user) {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, AUTHORIZATION_TOKEN);

        editor.commit();
    }

    private boolean loadUserTokenCache(MobileServiceClient client) {
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

    public void getFacebookUserDetails() {
        List<String> permissions = Arrays.asList("public_profile", "email", "user_birthday");
//        String userID = mClient.getCurrentUser().getUserId();
//        userID = userID.replace("Facebook:", "");
//        accessToken = new AccessToken(AUTHORIZATION_TOKEN, "1185338404814767", userID, permissions, null, AccessTokenSource.WEB_VIEW, null, null);
//        AccessToken.setCurrentAccessToken(accessToken);
        // Get Facebook Account User data as JSON Object using GRAPH API
        GraphRequest graphRequest = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject jsonObject, GraphResponse graphResponse) {
                if (graphResponse.getError() == null) {
                    try {
                        // Read data from JSON Object and set up user
                        if (jsonObject.has("name")) {
                            name = jsonObject.getString("name");
                        }
                        if (jsonObject.has("email")) {
                            email = jsonObject.getString("email");
                        }
                        if (jsonObject.has("birthday")) {
                            birthDate = jsonObject.getString("birthday");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON", e);
                    }
                } else {
                    Log.e(TAG, graphResponse.getError().getErrorMessage());
                }
            }
        });
        Bundle parameters = new Bundle();
        // Set JSON object field structure
        parameters.putString("fields", "name,email,birthday");
        graphRequest.setParameters(parameters);
        graphRequest.executeAsync();
    }

}
