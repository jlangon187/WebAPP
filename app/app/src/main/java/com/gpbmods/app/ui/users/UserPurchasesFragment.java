package com.gpbmods.app.ui.users;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.gpbmods.app.core.util.RepositoryCallback;
import com.gpbmods.app.data.remote.dto.AdminUserDto;
import com.gpbmods.app.data.repository.AdminRepository;
import com.gpbmods.app.databinding.FragmentUserPurchasesBinding;

import java.util.List;
import java.util.Locale;

public class UserPurchasesFragment extends Fragment {

    private FragmentUserPurchasesBinding binding;
    private AdminUserDto user;
    private AdminUserDto.PurchaseDto selected;

    private AdminRepository getRepository() {
        return new AdminRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUserPurchasesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        long userId = getArguments() == null ? 0 : getArguments().getLong("userId", 0);
        String userName = getArguments() == null ? "Usuario" : getArguments().getString("userName", "Usuario");
        binding.tvTitle.setText("Compras de " + userName);

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.btnSaveGuid.setOnClickListener(v -> saveGuid());
        binding.btnResend.setOnClickListener(v -> resendEmail());
        binding.btnPrepareDownload.setOnClickListener(v -> prepareDownload());

        loadUser(userId);
    }

    private void loadUser(long userId) {
        showLoading(true);
        getRepository().getUsers(new RepositoryCallback<>() {
            @Override
            public void onSuccess(List<AdminUserDto> data) {
                if (!isAdded()) return;
                showLoading(false);
                for (AdminUserDto u : data) {
                    if (u.id == userId) {
                        user = u;
                        break;
                    }
                }
                bindPurchases();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                showLoading(false);
                binding.tvError.setText(message);
                binding.tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void bindPurchases() {
        if (user == null || user.purchases == null || user.purchases.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.contentArea.setVisibility(View.GONE);
            return;
        }

        binding.tvEmpty.setVisibility(View.GONE);
        binding.contentArea.setVisibility(View.VISIBLE);

        String[] labels = new String[user.purchases.size()];
        for (int i = 0; i < user.purchases.size(); i++) {
            AdminUserDto.PurchaseDto p = user.purchases.get(i);
            String modName = (p.mod != null && p.mod.nombre != null) ? p.mod.nombre : "Mod";
            labels[i] = "#" + p.id + " - " + modName + " (" + p.metodoPago + ")";
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), com.gpbmods.app.R.layout.spinner_item_light, labels);
        adapter.setDropDownViewResource(com.gpbmods.app.R.layout.spinner_item_dropdown_light);
        binding.spPurchases.setAdapter(adapter);
        selected = user.purchases.get(0);
        binding.etPurchaseGuid.setText(selected.guidCompra == null ? "" : selected.guidCompra);
        binding.spPurchases.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selected = user.purchases.get(position);
                binding.etPurchaseGuid.setText(selected.guidCompra == null ? "" : selected.guidCompra);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void saveGuid() {
        if (user == null || selected == null) return;
        String guid = String.valueOf(binding.etPurchaseGuid.getText()).trim().toUpperCase(Locale.ROOT);
        if (!guid.matches("^[A-F0-9]{18}$")) {
            binding.tvError.setText("GUID invalido: debe tener 18 hex.");
            binding.tvError.setVisibility(View.VISIBLE);
            return;
        }
        showLoading(true);
        getRepository().updatePurchaseGuid(user.id, selected.id, guid, new RepositoryCallback<>() {
            @Override
            public void onSuccess(AdminUserDto data) {
                if (!isAdded()) return;
                showLoading(false);
                binding.tvSuccess.setText("GUID de compra actualizado.");
                binding.tvSuccess.setVisibility(View.VISIBLE);
                user = data;
                bindPurchases();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                showLoading(false);
                binding.tvError.setText(message);
                binding.tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void resendEmail() {
        if (user == null || selected == null) return;
        showLoading(true);
        getRepository().resendDownloadEmail(user.id, selected.id, new RepositoryCallback<>() {
            @Override
            public void onSuccess(String data) {
                if (!isAdded()) return;
                showLoading(false);
                binding.tvSuccess.setText(data == null || data.isEmpty() ? "Correo reenviado." : data);
                binding.tvSuccess.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                showLoading(false);
                binding.tvError.setText(message);
                binding.tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void prepareDownload() {
        if (user == null || selected == null) return;
        showLoading(true);
        getRepository().prepareDownload(user.id, selected.id, new RepositoryCallback<>() {
            @Override
            public void onSuccess(String data) {
                if (!isAdded()) return;
                showLoading(false);
                binding.tvSuccess.setText(data == null || data.isEmpty() ? "Solicitud de enlace enviada." : data);
                binding.tvSuccess.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                showLoading(false);
                binding.tvError.setText(message);
                binding.tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.tvError.setVisibility(View.GONE);
            binding.tvSuccess.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
