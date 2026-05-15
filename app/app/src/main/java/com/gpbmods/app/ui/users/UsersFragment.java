package com.gpbmods.app.ui.users;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.gpbmods.app.R;
import com.gpbmods.app.data.remote.dto.AdminUserDto;
import com.gpbmods.app.databinding.FragmentUsersBinding;
import com.gpbmods.app.viewmodel.UsersViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsersFragment extends Fragment implements UsersAdapter.Listener {

    private FragmentUsersBinding binding;
    private UsersViewModel viewModel;
    private UsersAdapter adapter;
    private List<AdminUserDto> allUsers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onPurchases(AdminUserDto user) {
        Bundle args = new Bundle();
        args.putLong("userId", user.id);
        args.putString("userName", user.nombre == null ? "Usuario" : user.nombre);
        Navigation.findNavController(binding.getRoot()).navigate(R.id.action_usersFragment_to_userPurchasesFragment, args);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(UsersViewModel.class);
        adapter = new UsersAdapter(this);

        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerUsers.setAdapter(adapter);
        binding.swipeRefresh.setColorSchemeResources(R.color.brand_primary);
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadUsers());

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterUsers(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
        {
            boolean isLoading = Boolean.TRUE.equals(loading);
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!isLoading) {
                binding.swipeRefresh.setRefreshing(false);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error == null || error.isEmpty()) {
                binding.tvError.setVisibility(View.GONE);
            } else {
                binding.tvError.setText(error);
                binding.tvError.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.isEmpty()) {
                binding.tvSuccess.setVisibility(View.GONE);
            } else {
                binding.tvSuccess.setText(msg);
                binding.tvSuccess.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
            allUsers = users == null ? new ArrayList<>() : users;
            binding.tvEmpty.setVisibility(allUsers.isEmpty() ? View.VISIBLE : View.GONE);
            filterUsers(binding.etSearch.getText() == null ? "" : binding.etSearch.getText().toString());
        });

        viewModel.loadUsers();
    }

    private void filterUsers(String q) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            adapter.submitList(allUsers);
            binding.tvEmpty.setVisibility(allUsers.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        List<AdminUserDto> filtered = new ArrayList<>();
        for (AdminUserDto user : allUsers) {
            String name = user.nombre == null ? "" : user.nombre.toLowerCase(Locale.ROOT);
            String email = user.email == null ? "" : user.email.toLowerCase(Locale.ROOT);
            String guid = user.guid == null ? "" : user.guid.toLowerCase(Locale.ROOT);
            String role = user.rol == null ? "" : user.rol.toLowerCase(Locale.ROOT);
            if (name.contains(query) || email.contains(query) || guid.contains(query) || role.contains(query)) {
                filtered.add(user);
            }
        }
        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onEdit(AdminUserDto user) {
        Bundle args = new Bundle();
        args.putLong("userId", user.id);
        args.putString("nombre", user.nombre);
        args.putString("email", user.email);
        args.putString("guid", user.guid);
        args.putString("rol", user.rol);
        args.putBoolean("activo", user.activo);
        Navigation.findNavController(binding.getRoot()).navigate(com.gpbmods.app.R.id.action_usersFragment_to_editUserFragment, args);
    }

    private String value(Editable editable) {
        return editable == null ? "" : editable.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadUsers();
        }
    }
}
