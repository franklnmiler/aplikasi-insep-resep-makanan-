package com.example.project111;

import android.content.Context;
import android.content.SharedPreferences;

public class UserManager {
    private static final String PREF_NAME = "UserData";
    private static final String KEY_USER_ID = "userId";
    private Context context;

    public UserManager(Context context) {
        this.context = context;
    }

    public String getUserId() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(KEY_USER_ID, null);
        if (userId == null) {
            userId = "user_" + System.currentTimeMillis();
            prefs.edit().putString(KEY_USER_ID, userId).apply();
        }
        return userId;
    }
}
