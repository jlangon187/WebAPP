package com.gpbmods.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gpbmods.app.core.util.RepositoryCallback;
import com.gpbmods.app.data.remote.dto.CategoriaDto;
import com.gpbmods.app.data.remote.dto.ModDto;
import com.gpbmods.app.data.repository.AdminRepository;

import java.util.ArrayList;
import java.util.List;

public class ModsViewModel extends AndroidViewModel {

    private final AdminRepository adminRepository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<String> successMessage = new MutableLiveData<>(null);
    private final MutableLiveData<List<ModDto>> mods = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<CategoriaDto>> categorias = new MutableLiveData<>(new ArrayList<>());

    public ModsViewModel(@NonNull Application application) {
        super(application);
        adminRepository = new AdminRepository(application.getApplicationContext());
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<List<ModDto>> getMods() { return mods; }
    public LiveData<List<CategoriaDto>> getCategorias() { return categorias; }

    public void loadAll() {
        loadMods();
        loadCategorias();
    }

    public void loadMods() {
        loading.setValue(true);
        adminRepository.getMods(new RepositoryCallback<>() {
            @Override
            public void onSuccess(List<ModDto> data) {
                loading.postValue(false);
                mods.postValue(data);
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    private void loadCategorias() {
        adminRepository.getCategorias(new RepositoryCallback<>() {
            @Override
            public void onSuccess(List<CategoriaDto> data) {
                categorias.postValue(data);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue(message);
            }
        });
    }

    public void saveMod(ModDto mod) {
        loading.setValue(true);
        if (mod.id == null) {
            adminRepository.createMod(mod, new RepositoryCallback<>() {
                @Override
                public void onSuccess(ModDto data) {
                    loading.postValue(false);
                    successMessage.postValue("Mod creado correctamente.");
                    loadMods();
                }

                @Override
                public void onError(String message) {
                    loading.postValue(false);
                    errorMessage.postValue(message);
                }
            });
            return;
        }

        adminRepository.updateMod(mod.id, mod, new RepositoryCallback<>() {
            @Override
            public void onSuccess(ModDto data) {
                loading.postValue(false);
                successMessage.postValue("Mod actualizado correctamente.");
                loadMods();
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void deleteMod(ModDto mod) {
        if (mod.id == null) return;
        loading.setValue(true);
        adminRepository.deleteMod(mod.id, new RepositoryCallback<>() {
            @Override
            public void onSuccess(Boolean data) {
                loading.postValue(false);
                successMessage.postValue("Mod eliminado correctamente.");
                loadMods();
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }
}
