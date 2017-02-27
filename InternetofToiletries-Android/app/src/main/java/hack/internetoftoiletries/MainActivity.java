package hack.internetoftoiletries;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.api.Listener;
import com.amazon.identity.auth.device.api.authorization.AuthCancellation;
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager;
import com.amazon.identity.auth.device.api.authorization.AuthorizeListener;
import com.amazon.identity.auth.device.api.authorization.AuthorizeRequest;
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult;
import com.amazon.identity.auth.device.api.authorization.ProfileScope;
import com.amazon.identity.auth.device.api.authorization.Scope;
import com.amazon.identity.auth.device.api.authorization.User;
import com.amazon.identity.auth.device.api.workflow.RequestContext;
import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
import com.amazon.identity.auth.device.authorization.api.AuthzConstants;

import org.codeandmagic.android.gauge.GaugeView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType STRING
            = MediaType.parse("application/x-www-form-urlencoded");
    public String DRS_URL = "https://dash-replenishment-service-na.amazon.com/replenish/";

    private TextView mProfileText;
    private TextView mProfileText2;
    private TextView mLogoutTextView;
    private ProgressBar mLogInProgress;
    private RequestContext requestContext;
    private boolean mIsLoggedIn;
    private View mLoginButton;
    private ViewGroup mWrapperProfile;
    private GaugeView mGaugeView1;
    private GaugeView mGaugeView2;

    private TextView product1;
    private TextView product2;

    private ScheduledFuture<?> future;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    OkHttpClient client = new OkHttpClient();

    private String slot1 = "f76a78ff-d73b-4c64-967f-d28f54645c9d";
    private String slot2 = "59a29d6d-39f3-4adb-8b88-96fc5e2261cc";

        private AmazonAuthorizationManager mAuthManager;

    private boolean started = false;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuthManager = new AmazonAuthorizationManager(this, Bundle.EMPTY);

        requestContext = RequestContext.create(this);

        /*
        requestContext.registerListener(new AuthorizeListener() {
            @Override
            public void onSuccess(AuthorizeResult authorizeResult) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // At this point we know the authorization completed, so remove the ability to return to the app to sign-in again
                        setLoggingInState(true);
                    }
                });
                fetchUserProfile();
            }

            @Override
            public void onError(AuthError authError) {
                Log.e(TAG, "AuthError during authorization", authError);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showAuthToast("Error during authorization.  Please try again.");
                        resetProfileView();
                        setLoggingInState(false);
                    }
                });
            }

            @Override
            public void onCancel(AuthCancellation authCancellation) {
                Log.e(TAG, "User cancelled authorization");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showAuthToast("Authorization cancelled");
                        resetProfileView();
                    }
                });
            }
        });*/


        setContentView(R.layout.activity_main);
        initializeUI();

        //new HttpGetRequestRefill().execute("");

        start();

        //Schedule for every 24 hours, this is just for demo, will be used in the future
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                    SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
                    if(sharedPref.contains("access_token") && sharedPref.contains("refresh_token"))
                    {
                        new HttpGetRequestRefill().execute("");
                    }
            }
        };

        //schedule this once every day
        future = scheduler.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.DAYS);

    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            new HttpGetRequest().execute("");
            start();
        }
    };

    public void start() {
        started = true;
        handler.postDelayed(runnable, 2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestContext.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        if(sharedPref.contains("refresh_token") && sharedPref.contains("access_token"))
        {
            mLoginButton.setVisibility(View.GONE);
            mWrapperProfile.setVisibility(View.VISIBLE);
        }
        else
        {
            mLoginButton.setVisibility(View.VISIBLE);
            mWrapperProfile.setVisibility(View.GONE);
        }

        /*
        Scope[] scopes = {ProfileScope.profile(), ProfileScope.postalCode()};
        AuthorizationManager.getToken(this, scopes, new Listener<AuthorizeResult, AuthError>() {
            @Override
            public void onSuccess(AuthorizeResult result) {
                if (result.getAccessToken() != null) {

                    fetchUserProfile();
                    mAccessToken = result.getAccessToken();
                } else {

                }
            }

            @Override
            public void onError(AuthError ae) {

            }
        });*/
    }


    private void fetchUserProfile() {
        User.fetch(this, new Listener<User, AuthError>() {

            /* fetch completed successfully. */
            @Override
            public void onSuccess(User user) {
                final String name = user.getUserName();
                final String email = user.getUserEmail();
                final String account = user.getUserId();
                final String zipCode = user.getUserPostalCode();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateProfileData(name, email, account, zipCode);
                    }
                });
            }

            /* There was an error during the attempt to get the profile. */
            @Override
            public void onError(AuthError ae) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoggedOutState();
                        String errorMessage = "Error retrieving profile information.\nPlease log in again";
                        Toast errorToast = Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG);
                        errorToast.setGravity(Gravity.CENTER, 0, 0);
                        errorToast.show();
                    }
                });
            }
        });
    }

    private void updateProfileData(String name, String email, String account, String zipCode) {
        StringBuilder profileBuilder = new StringBuilder();
        profileBuilder.append(String.format("Welcome, %s!\n", name));
        profileBuilder.append(String.format("Your email is %s\n", email));
        profileBuilder.append(String.format("Your zipCode is %s\n", zipCode));
        final String profile = profileBuilder.toString();
        Log.d(TAG, "Profile Response: " + profile);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateProfileView(profile);
                setLoggedInState();
            }
        });
    }

    /**
     * Initializes all of the UI elements in the activity
     */
    private void initializeUI() {

        mWrapperProfile = (ViewGroup)findViewById(R.id.wrapperProfile);
        mLoginButton = findViewById(R.id.login_with_amazon);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle options = new Bundle();
                // device_model is generated by the wizard when you create a device - replace "modelX"; serial is something you provide and should be visible to the customer - replace "serialY".
                String scope_data = "{\"dash:replenish\":{\"device_model\":\"Internet_of_Toiletries\", \"serial\":\"0\", \"is_test_device \":\"true\"} }";
                options.putString(AuthzConstants.BUNDLE_KEY.SCOPE_DATA.val, scope_data);

                // Request the authorization code instead of an access token
                options.putBoolean(AuthzConstants.BUNDLE_KEY.GET_AUTH_CODE.val, true);
                // Plain = code verifier; S256 uses a Base64url encoding of the code verifier's hash
                options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE.val, "e9598da04c204deaf2dff8892efdd9cb0e180b44f406c31ee916175a99511231");
                // Set code challenge method - "plain" or "S256"
                options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE_METHOD.val, "plain");

                mAuthManager.authorize(new String []{"dash:replenish"}, options, new AuthorizeListener());

                /*
                AuthorizationManager.authorize(
                        new AuthorizeRequest.Builder(requestContext)
                                .addScopes(ProfileScope.profile(), ProfileScope.postalCode())
                                .build()
                );*/
            }
        });

        mGaugeView1 = (GaugeView)findViewById(R.id.gauge_view1);
        mGaugeView1.setTargetValue(0);
        mGaugeView1.initDrawingRects();

        mGaugeView2 = (GaugeView)findViewById(R.id.gauge_view2);
        mGaugeView2.setTargetValue(0);
        mGaugeView2.initDrawingRects();

        product1 = (TextView)findViewById(R.id.product1);
        product2 = (TextView)findViewById(R.id.product2);

        // Find the button with the logout ID and set up a click handler
        View logoutButton = findViewById(R.id.logout);
        logoutButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AuthorizationManager.signOut(getApplicationContext(), new Listener<Void, AuthError>() {
                    @Override
                    public void onSuccess(Void response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLoggedOutState();
                            }
                        });
                    }

                    @Override
                    public void onError(AuthError authError) {
                        Log.e(TAG, "Error clearing authorization state.", authError);
                    }
                });
            }
        });

        String logoutText = getString(R.string.logout);
        mProfileText = (TextView) findViewById(R.id.profile_info);
        mProfileText2 = (TextView) findViewById(R.id.profile_info_2);
        mLogoutTextView = (TextView) logoutButton;
        mLogoutTextView.setText(logoutText);
        mLogInProgress = (ProgressBar) findViewById(R.id.log_in_progress);
    }

    /**
     * Sets the text in the mProfileText {@link TextView} to the value of the provided String.
     *
     * @param profileInfo the String with which to update the {@link TextView}.
     */
    private void updateProfileView(String profileInfo) {
        Log.d(TAG, "Updating profile view");
        mProfileText.setText(profileInfo);
        mProfileText2.setText(profileInfo);
    }

    /**
     * Sets the text in the mProfileText {@link TextView} to the prompt it originally displayed.
     */
    private void resetProfileView() {
        setLoggingInState(false);
        mProfileText.setText(getString(R.string.default_message));
    }

    /**
     * Sets the state of the application to reflect that the user is currently authorized.
     */
    private void setLoggedInState() {
        mLoginButton.setVisibility(Button.GONE);
        setLoggedInButtonsVisibility(Button.VISIBLE);
        mIsLoggedIn = true;
        setLoggingInState(false);
    }

    /**
     * Sets the state of the application to reflect that the user is not currently authorized.
     */
    private void setLoggedOutState() {
        mLoginButton.setVisibility(Button.VISIBLE);
        setLoggedInButtonsVisibility(Button.GONE);
        mIsLoggedIn = false;
        resetProfileView();
    }

    /**
     * Changes the visibility for both of the buttons that are available during the logged in state
     *
     * @param visibility the visibility to which the buttons should be set
     */
    private void setLoggedInButtonsVisibility(int visibility) {
        mLogoutTextView.setVisibility(visibility);
    }

    /**
     * Turns on/off display elements which indicate that the user is currently in the process of logging in
     *
     * @param loggingIn whether or not the user is currently in the process of logging in
     */
    private void setLoggingInState(final boolean loggingIn) {
        if (loggingIn) {
            mLoginButton.setVisibility(Button.GONE);
            setLoggedInButtonsVisibility(Button.GONE);
            mLogInProgress.setVisibility(ProgressBar.VISIBLE);
            mProfileText.setVisibility(TextView.GONE);
            mWrapperProfile.setVisibility(View.GONE);
        } else {
            if (mIsLoggedIn) {
                setLoggedInButtonsVisibility(Button.VISIBLE);
                mProfileText.setVisibility(TextView.VISIBLE);
                mWrapperProfile.setVisibility(View.VISIBLE);
            } else {
                mLoginButton.setVisibility(Button.VISIBLE);
                mProfileText.setVisibility(TextView.GONE);
                mWrapperProfile.setVisibility(View.GONE);
            }
            mLogInProgress.setVisibility(ProgressBar.GONE);
        }
    }

    private void showAuthToast(String authToastMessage) {
        Toast authToast = Toast.makeText(getApplicationContext(), authToastMessage, Toast.LENGTH_LONG);
        authToast.setGravity(Gravity.CENTER, 0, 0);
        authToast.show();
    }


    public class DashAuthorize extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params){
            String result = null;
            try {
                if(params.length == 0 || params[0] == null)
                {
                    return null;
                }

                RequestBody body = RequestBody.create(STRING, params[0]);

                Request AMZNrequest = new Request.Builder()
                        .url("https://api.amazon.com/auth/O2/token")
                        .post(body)
                        .build();
                Response AMZNresponse = client.newCall(AMZNrequest).execute();
                String responseBody = AMZNresponse.body().string();
                Log.e("responseBody", responseBody);

                JSONObject authData = new JSONObject(responseBody);
                SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                if(authData.has("access_token"))
                {
                    editor.putString("access_token", authData.getString("access_token"));
                    editor.commit();
                }
                if(authData.has("refresh_token"))
                {
                    editor.putString("refresh_token", authData.getString("refresh_token"));
                    editor.commit();
                }
                return responseBody;
            }
            catch(IOException e){
                e.printStackTrace();
                result = null;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return result;
        }
        protected void onPostExecute(String result){
            super.onPostExecute(result);
            if(result != null)
            {
                try {
                    JSONObject authData = new JSONObject(result);
                    if(authData.has("refresh_token")) {
                        mLoginButton.setVisibility(View.GONE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.e("stuff", result);

            }
        }
    }


    public class DashRefresh extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params){
            String result = null;
            try {

                SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
                if(!sharedPref.contains("access_token") || !sharedPref.contains("refresh_token") || !sharedPref.contains("client_id"))
                {
                    return null;
                }

                String bodyraw = "grant_type=refresh_token&refresh_token=" + sharedPref.getString("sharedPref", "")
                        + "&client_id=" + sharedPref.getString("client_id", "");

                RequestBody body = RequestBody.create(STRING, bodyraw);

                Request AMZNrequest = new Request.Builder()
                        .url("https://api.amazon.com/auth/O2/token")
                        .post(body)
                        .build();
                Response AMZNresponse = client.newCall(AMZNrequest).execute();
                String responseBody = AMZNresponse.body().string();
                Log.e("responseBody", responseBody);

                JSONObject authData = new JSONObject(responseBody);
                SharedPreferences.Editor editor = sharedPref.edit();
                if(authData.has("access_token"))
                {
                    editor.putString("access_token", authData.getString("access_token"));
                    editor.commit();
                }
                if(authData.has("refresh_token"))
                {
                    editor.putString("refresh_token", authData.getString("refresh_token"));
                    editor.commit();
                }

                return responseBody;
            }
            catch(IOException e){
                e.printStackTrace();
                result = null;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return result;
        }
        protected void onPostExecute(String result){
            super.onPostExecute(result);
            super.onPostExecute(result);
            if(result != null)
            {
                try {
                    JSONObject authData = new JSONObject(result);
                    if(authData.has("refresh_token")) {
                        mLoginButton.setVisibility(View.GONE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.e("stuff", result);
                new HttpGetRequestRefill().execute("");

            }
        }
    }

    public class HttpGetRequest extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params){
            String result;
            String inputLine;
            try {
                Request request = new Request.Builder()
                        .url("https://shrouded-citadel-97257.herokuapp.com/getproduct")
                        .build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    return null;
                }

                return response.body().string();
            }
            catch(IOException e){
                e.printStackTrace();
                result = null;
            }
            return result;
        }
        protected void onPostExecute(String result){
            super.onPostExecute(result);
            if(result != null)
            {
                try {
                    JSONArray data = new JSONArray(result);
                    if(data.length() > 0)
                    {
                        JSONObject item = data.getJSONObject(0);
                        String product1_desc = item.optString("Product1Name", "");
                        String product2_desc = item.optString("Product2Name", "");
                        float product_ratio1 = (float) (item.getDouble("Product1weight") / item.getDouble("Product1Max"));
                        float product_ratio2 = (float) (item.getDouble("Product2weight") / item.getDouble("Product2Max"));

                        product1.setText(product1_desc);
                        product2.setText(product2_desc);
                        mGaugeView1.setTargetValue(product_ratio1 * 100.0f);
                        mGaugeView2.setTargetValue(product_ratio2 * 100.0f);

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Amazon Dash
    public class HttpGetRequestRefill extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params){
            String result = null;
            try {

                Request request = new Request.Builder()
                        .url("https://shrouded-citadel-97257.herokuapp.com/getproduct")
                        .build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful())
                {
                    return null;
                }
                String jsonresult = response.body().string();
                Log.e("result", jsonresult);
                JSONArray data = new JSONArray(jsonresult);
                if(data.length() > 0)
                {
                    JSONObject item = data.getJSONObject(0);
                    float product_ratio1 = (float) (item.getDouble("Product1weight") / item.getDouble("Product1Max"));
                    float product_ratio2 = (float) (item.getDouble("Product2weight") / item.getDouble("Product2Max"));
                    String url = "";
                    //If first product is smaller than 25%
                    //if(product_ratio1 * 100.0f < 25.0f)
                    //{
                    url = DRS_URL + slot1;
                    //}
                    //If second product is smaller than 25%
                    if(product_ratio2 * 100.0f < 25.0f)
                    {
                        url = DRS_URL + slot2;
                    }
                    SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
                    RequestBody body = RequestBody.create(JSON, "");
                    Request AMZNrequest = new Request.Builder()
                            .addHeader("Authorization", "Bearer " + sharedPref.getString("access_token", ""))
                            .addHeader("x-amzn-accept-type", "com.amazon.dash.replenishment.DrsReplenishResult@1.0")
                            .addHeader("x-amzn-type-version", "com.amazon.dash.replenishment.DrsReplenishInput@1.0")
                            .url(url)
                            .post(body)
                            .build();
                    Response AMZNresponse = client.newCall(AMZNrequest).execute();
                    String responseBody = AMZNresponse.body().string();
                    return responseBody;
                }
            }
            catch(IOException e){
                e.printStackTrace();
                result = null;
            }
            catch (JSONException e) {
                e.printStackTrace();
                result = null;

            }
            return result;
        }
        protected void onPostExecute(String result){
            super.onPostExecute(result);
            if(result != null)
            {
                JSONObject order = null;
                try {
                    order = new JSONObject(result);
                    if(!order.has("eventInstanceId"))
                    {
                        new DashRefresh().execute("");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.e("result", result);
            }
        }
    }

    public class AuthorizeListener implements AuthorizationListener {

        /* Authorization was completed successfully. */
        @Override
        public void onSuccess(Bundle response) {

            try {
                String authorizationCode = response.getString(AuthzConstants.BUNDLE_KEY.AUTHORIZATION_CODE.val);
                String clientId = mAuthManager.getClientId();
                String redirectUri = mAuthManager.getRedirectUri();


                SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("client_id", clientId);
                editor.commit();
                /*
                RequestBody body = new FormBody.Builder()
                        .add("grant_type", "authorization_code")
                        .add("code", authorizationCode)
                        .add("redirect_uri", redirectUri)
                        .add("client_id", clientId)
                        .add("code_verifier", "e9598da04c204deaf2dff8892efdd9cb0e180b44f406c31ee916175a99511231")
                        .build();*/


                String body = "grant_type=authorization_code&code=" + authorizationCode + "&redirect_uri=" + redirectUri
                        + "&client_id=" + clientId + "&code_verifier=e9598da04c204deaf2dff8892efdd9cb0e180b44f406c31ee916175a99511231";
                Log.e("doh", body);

                //Getting authorized
                new DashAuthorize().execute(body);
            } catch (AuthError authError) {
                authError.printStackTrace();
            }
        }
        /* There was an error during the attempt to authorize the application. */
        @Override
        public void onError(AuthError ae) {
            String errorResponse = ae.getMessage();
            if(errorResponse != null)
            {
                Log.e("doh", errorResponse);
            }
        }
        /* Authorization was cancelled before it could be completed. */
        @Override
        public void onCancel(Bundle cause) {
        }
    }

}
