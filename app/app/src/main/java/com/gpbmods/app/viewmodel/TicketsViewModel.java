package com.gpbmods.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gpbmods.app.core.util.RepositoryCallback;
import com.gpbmods.app.data.remote.dto.TicketDto;
import com.gpbmods.app.data.repository.AdminRepository;

import java.util.ArrayList;
import java.util.List;

public class TicketsViewModel extends AndroidViewModel {

    private final AdminRepository adminRepository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<String> successMessage = new MutableLiveData<>(null);
    private final MutableLiveData<List<TicketDto>> tickets = new MutableLiveData<>(new ArrayList<>());

    public TicketsViewModel(@NonNull Application application) {
        super(application);
        adminRepository = new AdminRepository(application.getApplicationContext());
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<List<TicketDto>> getTickets() { return tickets; }

    public void loadTickets() {
        loading.setValue(true);
        errorMessage.setValue(null);
        adminRepository.getTickets(new RepositoryCallback<>() {
            @Override
            public void onSuccess(List<TicketDto> data) {
                loading.postValue(false);
                tickets.postValue(data);
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void replyTicket(TicketDto ticket, String responseText) {
        loading.setValue(true);
        adminRepository.replyTicket(ticket.id, responseText, new RepositoryCallback<>() {
            @Override
            public void onSuccess(TicketDto data) {
                replaceTicket(data);
                loading.postValue(false);
                successMessage.postValue("Ticket respondido correctamente.");
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void closeTicket(TicketDto ticket) {
        loading.setValue(true);
        adminRepository.closeTicket(ticket.id, new RepositoryCallback<>() {
            @Override
            public void onSuccess(TicketDto data) {
                replaceTicket(data);
                loading.postValue(false);
                successMessage.postValue("Ticket cerrado correctamente.");
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    private void replaceTicket(TicketDto updated) {
        List<TicketDto> current = tickets.getValue();
        if (current == null) current = new ArrayList<>();
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).id == updated.id) {
                current.set(i, updated);
                break;
            }
        }
        tickets.postValue(current);
    }
}
