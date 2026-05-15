package com.gpbmods.app.core.security;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

public class TokenStore {

    private static final String FILE_NAME = "admin_secure_store";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_NAME = "user_name";

    private final SharedPreferences preferences;

    public TokenStore(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            preferences = EncryptedSharedPreferences.create(
                    FILE_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize secure storage", e);
        }
    }

    public void saveSession(String token, String role, String name) {
        preferences.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_ROLE, role)
                .putString(KEY_NAME, name)
                .apply();
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }

    public String getRole() {
        return preferences.getString(KEY_ROLE, "");
    }

    public String getName() {
        return preferences.getString(KEY_NAME, "");
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
