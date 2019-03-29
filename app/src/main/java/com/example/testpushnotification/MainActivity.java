package com.example.testpushnotification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class MainActivity extends AppCompatActivity{
    final static String TAG = "MainActivity";
    public static final String MY_PREFS_NAME = "MyPrefsFile";
    private RequestQueue mRequestQueue;
    private StringRequest mStringRequest;
    SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }


        // check if the user's firebase register key is set and synchronized with server
        prefs = getSharedPreferences(getString(R.string.PrefFileName), MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();

        //TODO testing code, remove on deployment
        editor.clear();
        editor.apply();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        editor.putString(getString(R.string.ssn), "someDummy");
        editor.apply();

        String register_key = prefs.getString(getString(R.string.firebase_register_key), null);
        if(register_key == null){
            // key not set, so save key in shared prefs and send a copy to server

            // Get token
            // [START retrieve_current_token]
            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                Log.e(TAG, "getInstanceId failed", task.getException());
                                return;
                            }

                            // Get new Instance ID token
                            String token = task.getResult().getToken();

                            editor.putString(getString(R.string.firebase_register_key), token);
                            editor.apply();

                            sendRegisterKeyToServer(token);

                            // Log and toast
                            String msg = getString(R.string.msg_token_fmt, token);
                            Log.d(TAG, msg);


                        }
                    });
            // [END retrieve_current_token]

        }

        Button logTokenButton = findViewById(R.id.logTokenButton);
        logTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get token
                // [START retrieve_current_token]
                FirebaseInstanceId.getInstance().getInstanceId()
                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                            @Override
                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                if (!task.isSuccessful()) {
                                    Log.w(TAG, "getInstanceId failed", task.getException());
                                    return;
                                }

                                // Get new Instance ID token
                                String token = task.getResult().getToken();

                                // Log and toast
                                String msg = getString(R.string.msg_token_fmt, token);
                                Log.d(TAG, msg);
                                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        });
                // [END retrieve_current_token]
            }
        });

        Button register_id = findViewById(R.id.register_id);
        register_id.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String register_id = FirebaseInstanceId.getInstance().getToken();
                if(register_id != null){
                    Log.d(TAG, "Register Id Token: "+register_id);
                }else{
                    Log.e(TAG, "Null registration string error");
                }
            }
        });
    }

    void sendRegisterKeyToServer(String register_key){
        //RequestQueue initialized
        mRequestQueue = Volley.newRequestQueue(this);

        //setup the url
        String ssn = prefs.getString(getString(R.string.ssn), null);
        if(ssn == null){
            Log.e(TAG, "SSN NOT SET");
            return;
        }

        String url = getString(R.string.server_domain)+"/register?ssn_no="+ssn+"&register_id="+register_key;
        Log.d(TAG, url);

        //String Request initialized
        mStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Toast.makeText(getApplicationContext(),"Response :" + response.toString(), Toast.LENGTH_LONG).show();//display the response on screen

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.e(TAG,"Error :" + error.toString());
            }
        });

        mRequestQueue.add(mStringRequest);
    }
}
