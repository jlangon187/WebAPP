package com.gpbmods.app.data.remote.api;

import com.gpbmods.app.data.remote.dto.AuthResponse;
import com.gpbmods.app.data.remote.dto.LoginRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);
}
