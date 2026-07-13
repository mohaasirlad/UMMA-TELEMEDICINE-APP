package com.example.ummatelemedicineapp.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.json.JSONException;

public class TokenManager {

    private static final String TAG = "TokenManager";
    // Replace with your actual backend URL that generates Twilio tokens
    private static final String TOKEN_SERVER_URL = "https://your-backend-url.com/getToken";

    public interface TokenCallback {
        void onSuccess(String token);
        void onError(String error);
    }

    public static void fetchTwilioToken(Context context, String identity, String roomName, TokenCallback callback) {
        // IMPORTANT: Replace TOKEN_SERVER_URL with your actual backend endpoint.
        // The mock mode has been removed as requested to initiate real calls.

        OkHttpClient client = new OkHttpClient();
        String url = TOKEN_SERVER_URL + "?identity=" + identity + "&room=" + roomName;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);
                        String token = json.getString("token");
                        callback.onSuccess(token);
                    } catch (JSONException e) {
                        callback.onError("JSON Parsing error");
                    }
                } else {
                    callback.onError("Server error: " + response.code());
                }
            }
        });
    }
}
