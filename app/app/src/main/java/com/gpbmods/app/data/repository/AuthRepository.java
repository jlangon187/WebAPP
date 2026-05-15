package com.gpbmods.app.data.repository;

import android.content.Context;

import com.gpbmods.app.core.network.ApiClient;
import com.gpbmods.app.core.security.TokenStore;
import com.gpbmods.app.core.util.RepositoryCallback;
import com.gpbmods.app.data.remote.api.AuthApi;
import com.gpbmods.app.data.remote.dto.AuthResponse;
import com.gpbmods.app.data.remote.dto.LoginRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final AuthApi authApi;
    private final TokenStore tokenStore;

    public AuthRepository(Context context) {
        authApi = ApiClient.getInstance(context).create(AuthApi.class);
        tokenStore = new TokenStore(context);
    }

    public void login(String email, String password, RepositoryCallback<AuthResponse> callback) {
        authApi.login(new LoginRequest(email, password)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Credenciales invalidas o error de servidor.");
                    return;
                }

                AuthResponse data = response.body();
                if (data.rol == null || !"admin".equalsIgnoreCase(data.rol)) {
                    callback.onError("Esta app es solo para cuentas administradoras.");
                    return;
                }

                tokenStore.saveSession(data.token, data.rol, data.nombre);
                callback.onSuccess(data);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("No se pudo conectar con el servidor.");
            }
        });
    }

    public boolean hasSession() {
        String token = tokenStore.getToken();
        return token != null && !token.isEmpty();
    }

    public void logout() {
        tokenStore.clear();
    }
}
