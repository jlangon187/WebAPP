package com.gpbmods.app.data.repository;

import android.content.Context;

import com.gpbmods.app.core.network.ApiClient;
import com.gpbmods.app.core.util.RepositoryCallback;
import com.gpbmods.app.data.remote.api.AdminApi;
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
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRepository {

    private final AdminApi adminApi;

    public AdminRepository(Context context) {
        adminApi = ApiClient.getInstance(context).create(AdminApi.class);
    }

    public void getStats(RepositoryCallback<AdminStatsResponse> callback) {
        adminApi.getStats().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<AdminStatsResponse> call, Response<AdminStatsResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("No se pudieron cargar las estadisticas.");
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<AdminStatsResponse> call, Throwable t) {
                callback.onError("Error de red al cargar estadisticas.");
            }
        });
    }

    public void getUsers(RepositoryCallback<List<AdminUserDto>> callback) {
        adminApi.getUsers().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<AdminUserDto>> call, Response<List<AdminUserDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("No se pudieron cargar los usuarios.");
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<List<AdminUserDto>> call, Throwable t) {
                callback.onError("Error de red al cargar usuarios.");
            }
        });
    }

    public void getEncryptionOverview(RepositoryCallback<EncryptionOverviewResponseDto> callback) {
        adminApi.getEncryptionOverview().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<EncryptionOverviewResponseDto> call, Response<EncryptionOverviewResponseDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("No se pudo cargar el estado de cifrado.");
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<EncryptionOverviewResponseDto> call, Throwable t) {
                callback.onError("Error de red al cargar estado de cifrado.");
            }
        });
    }

    public void updatePurchaseGuid(long userId, long purchaseId, String guid, RepositoryCallback<AdminUserDto> callback) {
        adminApi.updatePurchaseGuid(userId, purchaseId, new PurchaseGuidUpdateRequest(guid)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<AdminUserDto> call, Response<AdminUserDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("No se pudo actualizar GUID de compra.");
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<AdminUserDto> call, Throwable t) {
                callback.onError("Error de red al actualizar GUID de compra.");
            }
        });
    }

    public void resendDownloadEmail(long userId, long purchaseId, RepositoryCallback<String> callback) {
        adminApi.resendDownloadEmail(userId, purchaseId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (!response.isSuccessful()) {
                    callback.onError("No se pudo reenviar el correo.");
                    return;
                }
                callback.onSuccess(response.body() == null ? "Correo reenviado." : response.body());
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                callback.onError("Error de red al reenviar correo.");
            }
        });
    }

    public void prepareDownload(long userId, long purchaseId, RepositoryCallback<String> callback) {
        adminApi.prepareDownload(userId, purchaseId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (!response.isSuccessful()) {
                    callback.onError("No se pudo solicitar la generacion de enlace.");
                    return;
                }
                callback.onSuccess(response.body() == null ? "Solicitud enviada." : response.body());
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                callback.onError("Error de red al solicitar generacion.");
            }
        });
    }

    public void updateUser(long id, AdminUserUpdateRequest request, RepositoryCallback<AdminUserDto> callback) {
        adminApi.updateUser(id, request).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<AdminUserDto> call, Response<AdminUserDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("No se pudo actualizar el usuario.");
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<AdminUserDto> call, Throwable t) {
                callback.onError("Error de red al actualizar usuario.");
            }
        });
    }

    public void getTickets(RepositoryCallback<List<TicketDto>> callback) {
        adminApi.getTickets().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<TicketDto>> call, Response<List<TicketDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("No se pudieron cargar los tickets.");
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<List<TicketDto>> call, Throwable t) {
                callback.onError("Error de red al cargar tickets.");
            }
        });
    }

    public void replyTicket(long id, String respuesta, RepositoryCallback<TicketDto> callback) {
        adminApi.replyTicket(id, new TicketReplyRequest(respuesta)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<TicketDto> call, Response<TicketDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("No se pudo responder el ticket.");
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<TicketDto> call, Throwable t) {
                callback.onError("Error de red al responder ticket.");
            }
        });
    }

    public void closeTicket(long id, RepositoryCallback<TicketDto> callback) {
        adminApi.closeTicket(id).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<TicketDto> call, Response<TicketDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("No se pudo cerrar el ticket.");
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<TicketDto> call, Throwable t) {
                callback.onError("Error de red al cerrar ticket.");
            }
        });
    }

    public void getMods(RepositoryCallback<List<ModDto>> callback) {
        adminApi.getMods().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<ModDto>> call, Response<List<ModDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("No se pudieron cargar los mods.");
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<List<ModDto>> call, Throwable t) {
                callback.onError("Error de red al cargar mods.");
            }
        });
    }

    public void createMod(ModDto mod, RepositoryCallback<ModDto> callback) {
        adminApi.createMod(mod).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ModDto> call, Response<ModDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("No se pudo crear el mod.");
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<ModDto> call, Throwable t) {
                callback.onError("Error de red al crear mod.");
            }
        });
    }

    public void updateMod(long id, ModDto mod, RepositoryCallback<ModDto> callback) {
        adminApi.updateMod(id, mod).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ModDto> call, Response<ModDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("No se pudo actualizar el mod.");
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<ModDto> call, Throwable t) {
                callback.onError("Error de red al actualizar mod.");
            }
        });
    }

    public void deleteMod(long id, RepositoryCallback<Boolean> callback) {
        adminApi.deleteMod(id).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    callback.onError("No se pudo eliminar el mod.");
                    return;
                }
                callback.onSuccess(true);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError("Error de red al eliminar mod.");
            }
        });
    }

    public void getCategorias(RepositoryCallback<List<CategoriaDto>> callback) {
        adminApi.getCategorias().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<CategoriaDto>> call, Response<List<CategoriaDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("No se pudieron cargar las categorias.");
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<List<CategoriaDto>> call, Throwable t) {
                callback.onError("Error de red al cargar categorias.");
            }
        });
    }
}
