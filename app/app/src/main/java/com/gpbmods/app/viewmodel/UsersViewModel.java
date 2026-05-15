package com.gpbmods.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gpbmods.app.core.util.RepositoryCallback;
import com.gpbmods.app.data.remote.dto.AdminUserDto;
import com.gpbmods.app.data.remote.dto.AdminUserUpdateRequest;
import com.gpbmods.app.data.repository.AdminRepository;

import java.util.ArrayList;
import java.util.List;

public class UsersViewModel extends AndroidViewModel {

    private final AdminRepository adminRepository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<String> successMessage = new MutableLiveData<>(null);
    private final MutableLiveData<List<AdminUserDto>> users = new MutableLiveData<>(new ArrayList<>());

    public UsersViewModel(@NonNull Application application) {
        super(application);
        this.adminRepository = new AdminRepository(application.getApplicationContext());
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<List<AdminUserDto>> getUsers() { return users; }

    public void loadUsers() {
        loading.setValue(true);
        errorMessage.setValue(null);
        adminRepository.getUsers(new RepositoryCallback<>() {
            @Override
            public void onSuccess(List<AdminUserDto> data) {
                loading.postValue(false);
                users.postValue(data);
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void updateUser(AdminUserDto user, AdminUserUpdateRequest request) {
        loading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        adminRepository.updateUser(user.id, request, new RepositoryCallback<>() {
            @Override
            public void onSuccess(AdminUserDto data) {
                List<AdminUserDto> current = users.getValue();
                if (current == null) current = new ArrayList<>();
                for (int i = 0; i < current.size(); i++) {
                    if (current.get(i).id == data.id) {
                        current.set(i, data);
                        break;
                    }
                }
                users.postValue(current);
                loading.postValue(false);
                successMessage.postValue("Usuario actualizado correctamente.");
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void updatePurchaseGuid(AdminUserDto user, AdminUserDto.PurchaseDto purchase, String guid) {
        loading.setValue(true);
        errorMessage.setValue(null);
        adminRepository.updatePurchaseGuid(user.id, purchase.id, guid, new RepositoryCallback<>() {
            @Override
            public void onSuccess(AdminUserDto data) {
                replaceUser(data);
                loading.postValue(false);
                successMessage.postValue("GUID de compra actualizado.");
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void resendDownloadEmail(AdminUserDto user, AdminUserDto.PurchaseDto purchase) {
        loading.setValue(true);
        errorMessage.setValue(null);
        adminRepository.resendDownloadEmail(user.id, purchase.id, new RepositoryCallback<>() {
            @Override
            public void onSuccess(String data) {
                loading.postValue(false);
                successMessage.postValue(data == null || data.isEmpty() ? "Correo reenviado correctamente." : data);
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    private void replaceUser(AdminUserDto updated) {
        List<AdminUserDto> current = users.getValue();
        if (current == null) current = new ArrayList<>();
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).id == updated.id) {
                current.set(i, updated);
                break;
            }
        }
        users.postValue(current);
    }
}
