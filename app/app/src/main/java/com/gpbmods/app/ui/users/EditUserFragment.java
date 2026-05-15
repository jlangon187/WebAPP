package com.gpbmods.app.ui.users;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.gpbmods.app.data.remote.dto.AdminUserDto;
import com.gpbmods.app.data.remote.dto.AdminUserUpdateRequest;
import com.gpbmods.app.data.repository.AdminRepository;
import com.gpbmods.app.databinding.FragmentEditUserBinding;

public class EditUserFragment extends Fragment {

    private FragmentEditUserBinding binding;
    private long userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEditUserBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        userId = args == null ? 0 : args.getLong("userId", 0);

        binding.etNombre.setText(args == null ? "" : args.getString("nombre", ""));
        binding.etEmail.setText(args == null ? "" : args.getString("email", ""));
        binding.etGuid.setText(args == null ? "" : args.getString("guid", ""));
        binding.switchActivo.setChecked(args != null && args.getBoolean("activo", true));

        String[] roles = new String[]{"invitado", "registrado", "admin"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(), com.gpbmods.app.R.layout.spinner_item_light, roles);
        roleAdapter.setDropDownViewResource(com.gpbmods.app.R.layout.spinner_item_dropdown_light);
        binding.spRol.setAdapter(roleAdapter);

        String rol = args == null ? "registrado" : args.getString("rol", "registrado");
        if ("admin".equalsIgnoreCase(rol)) binding.spRol.setSelection(2);
        else if ("registrado".equalsIgnoreCase(rol)) binding.spRol.setSelection(1);
        else binding.spRol.setSelection(0);

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.btnSave.setOnClickListener(v -> save());
    }

    private void save() {
        String guid = value(binding.etGuid.getText()).toUpperCase();
        if (!guid.isEmpty() && !guid.matches("^[A-F0-9]{18}$")) {
            binding.tvError.setText("GUID invalido (18 hex). ");
            binding.tvError.setVisibility(View.VISIBLE);
            return;
        }

        AdminUserUpdateRequest req = new AdminUserUpdateRequest();
        req.nombre = value(binding.etNombre.getText());
        req.email = value(binding.etEmail.getText());
        req.guid = guid;
        req.rol = binding.spRol.getSelectedItem().toString();
        req.activo = binding.switchActivo.isChecked();
        req.password = value(binding.etPassword.getText());

        binding.progressBar.setVisibility(View.VISIBLE);
        new AdminRepository(requireContext()).updateUser(userId, req, new com.gpbmods.app.core.util.RepositoryCallback<>() {
            @Override
            public void onSuccess(AdminUserDto data) {
                if (!isAdded()) return;
                binding.progressBar.setVisibility(View.GONE);
                Navigation.findNavController(binding.getRoot()).navigateUp();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.tvError.setText(message);
                binding.tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private String value(android.text.Editable e) {
        return e == null ? "" : e.toString().trim();
    }
}
