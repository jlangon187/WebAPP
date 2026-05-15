package com.gpbmods.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gpbmods.app.core.util.RepositoryCallback;
import com.gpbmods.app.data.remote.dto.EncryptionOverviewResponseDto;
import com.gpbmods.app.data.repository.AdminRepository;

public class QueueViewModel extends AndroidViewModel {

    private final AdminRepository adminRepository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<EncryptionOverviewResponseDto> overview = new MutableLiveData<>(null);

    public QueueViewModel(@NonNull Application application) {
        super(application);
        adminRepository = new AdminRepository(application.getApplicationContext());
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<EncryptionOverviewResponseDto> getOverview() { return overview; }

    public void loadOverview() {
        loading.setValue(true);
        errorMessage.setValue(null);
        adminRepository.getEncryptionOverview(new RepositoryCallback<>() {
            @Override
            public void onSuccess(EncryptionOverviewResponseDto data) {
                loading.postValue(false);
                overview.postValue(data);
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }
}
