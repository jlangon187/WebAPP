package com.gpbmods.app.data.remote.api;

import com.gpbmods.app.data.remote.dto.AdminStatsResponse;
import com.gpbmods.app.data.remote.dto.AdminUserDto;
import com.gpbmods.app.data.remote.dto.AdminUserUpdateRequest;
import com.gpbmods.app.data.remote.dto.CategoriaDto;
import com.gpbmods.app.data.remote.dto.EncryptionOverviewResponseDto;
import com.gpbmods.app.data.remote.dto.ModDto;
import com.gpbmods.app.data.remote.dto.PurchaseGuidUpdateRequest;
import com.gpbmods.app.data.remote.dto.TicketDto;
import com.gpbmods.app.data.remote.dto.TicketReplyRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Body;
import retrofit2.http.Path;
import retrofit2.http.POST;

public interface AdminApi {
    @GET("api/admin/stats")
    Call<AdminStatsResponse> getStats();

    @GET("api/admin/encryption-jobs/overview")
    Call<EncryptionOverviewResponseDto> getEncryptionOverview();

    @GET("api/admin/users")
    Call<List<AdminUserDto>> getUsers();

    @PUT("api/admin/users/{id}")
    Call<AdminUserDto> updateUser(@Path("id") long id, @Body AdminUserUpdateRequest request);

    @PUT("api/admin/users/{userId}/purchases/{purchaseId}/guid")
    Call<AdminUserDto> updatePurchaseGuid(@Path("userId") long userId,
                                          @Path("purchaseId") long purchaseId,
                                          @Body PurchaseGuidUpdateRequest request);

    @POST("api/admin/users/{userId}/purchases/{purchaseId}/resend-download-email")
    Call<String> resendDownloadEmail(@Path("userId") long userId,
                                     @Path("purchaseId") long purchaseId);

    @POST("api/admin/users/{userId}/purchases/{purchaseId}/prepare-download")
    Call<String> prepareDownload(@Path("userId") long userId,
                                 @Path("purchaseId") long purchaseId);

    @GET("api/admin/tickets")
    Call<List<TicketDto>> getTickets();

    @PUT("api/admin/tickets/{id}/responder")
    Call<TicketDto> replyTicket(@Path("id") long id, @Body TicketReplyRequest request);

    @PUT("api/admin/tickets/{id}/cerrar")
    Call<TicketDto> closeTicket(@Path("id") long id);

    @GET("api/mods/catalog")
    Call<List<ModDto>> getMods();

    @POST("api/mods")
    Call<ModDto> createMod(@Body ModDto mod);

    @PUT("api/mods/{id}")
    Call<ModDto> updateMod(@Path("id") long id, @Body ModDto mod);

    @DELETE("api/mods/{id}")
    Call<Void> deleteMod(@Path("id") long id);

    @GET("api/categorias")
    Call<List<CategoriaDto>> getCategorias();
}
