//package com.example.karan.sunshine;
//
//import android.app.IntentService;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.preference.PreferenceManager;
//import android.support.annotation.Nullable;
//import android.util.Log;
//
//import com.google.firebase.iid.FirebaseInstanceId;
//import com.google.firebase.messaging.FirebaseMessaging;
//
///**
// * Created by karan on 30-Mar-17.
// */
//
//public class RegistrationIntentService extends IntentService {
//
//    private static final String TAG = "RegIntentService";
//
//    /**
//     * Creates an IntentService.  Invoked by your subclass's constructor.
//     * <p>
//     * TAG is used to name the worker thread, important only for debugging.
//     */
//    public RegistrationIntentService() {
//        super(TAG);
//    }
//
//    @Override
//    protected void onHandleIntent(@Nullable Intent intent) {
//
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//
//        try {
//            // In the (unlikely) event that multiple refresh operations occur simultaneously,
//            // ensure that they are processed sequentially.
//            synchronized (TAG) {
//                // Initially this call goes out to the network to retrieve the token, subsequent calls
//                // are local.
//                FirebaseInstanceId instanceID = FirebaseInstanceId.getInstance();
//                String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
//                        FirebaseMessaging.INSTANCE_ID_SCOPE);
//                sendRegistrationToServer(token);
//
//                // You should store a boolean that indicates whether the generated token has been
//                // sent to your server. If the boolean is false, send the token to your server,
//                // otherwise your server should have already received the token.
//                sharedPreferences.edit().putBoolean(MainActivity.SENT_TOKEN_TO_SERVER, true).apply();
//            }
//        } catch (Exception e) {
//            Log.d(TAG, "Failed to complete token refresh", e);
//
//            // If an exception happens while fetching the new token or updating our registration data
//            // on a third-party server, this ensures that we'll attempt the update at a later time.
//            sharedPreferences.edit().putBoolean(MainActivity.SENT_TOKEN_TO_SERVER, false).apply();
//        }
//
//    }
//
//    /**
//     * Normally, you would want to persist the registration to third-party servers. Because we do
//     * not have a server, and are faking it with a website, you'll want to log the token instead.
//     * That way you can see the value in logcat, and note it for future use in the website.
//     *
//     * @param token The new token.
//     */
//    private void sendRegistrationToServer(String token) {
//        Log.i(TAG, "GCM Registration Token: " + token);
//    }
//
//}
