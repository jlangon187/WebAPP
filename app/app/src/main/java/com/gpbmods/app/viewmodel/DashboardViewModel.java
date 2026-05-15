package com.gpbmods.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gpbmods.app.core.util.RepositoryCallback;
import com.gpbmods.app.data.remote.dto.AdminStatsResponse;
import com.gpbmods.app.data.remote.dto.EncryptionOverviewResponseDto;
import com.gpbmods.app.data.repository.AdminRepository;
import com.gpbmods.app.data.repository.AuthRepository;

public class DashboardViewModel extends AndroidViewModel {

    private final AdminRepository adminRepository;
    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<AdminStatsResponse> stats = new MutableLiveData<>(null);
    private final MutableLiveData<EncryptionOverviewResponseDto> encryptionOverview = new MutableLiveData<>(null);

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        adminRepository = new AdminRepository(application.getApplicationContext());
        authRepository = new AuthRepository(application.getApplicationContext());
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<AdminStatsResponse> getStats() {
        return stats;
    }

    public LiveData<EncryptionOverviewResponseDto> getEncryptionOverview() {
        return encryptionOverview;
    }

    public void loadStats() {
        loading.setValue(true);
        errorMessage.setValue(null);

        adminRepository.getStats(new RepositoryCallback<>() {
            @Override
            public void onSuccess(AdminStatsResponse data) {
                loading.postValue(false);
                stats.postValue(data);
                loadEncryptionOverview();
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    private void loadEncryptionOverview() {
        adminRepository.getEncryptionOverview(new RepositoryCallback<>() {
            @Override
            public void onSuccess(EncryptionOverviewResponseDto data) {
                encryptionOverview.postValue(data);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue(message);
            }
        });
    }

    public void logout() {
        authRepository.logout();
    }
}
