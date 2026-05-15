package com.gpbmods.app.core.util;

public interface RepositoryCallback<T> {
    void onSuccess(T data);
    void onError(String message);
}
