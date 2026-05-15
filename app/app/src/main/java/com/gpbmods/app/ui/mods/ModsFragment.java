package com.gpbmods.app.ui.mods;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.gpbmods.app.data.remote.dto.ModDto;
import com.gpbmods.app.databinding.FragmentModsBinding;
import com.gpbmods.app.viewmodel.ModsViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModsFragment extends Fragment implements ModsAdapter.Listener {

    private FragmentModsBinding binding;
    private ModsViewModel viewModel;
    private ModsAdapter adapter;
    private List<ModDto> allMods = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentModsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ModsViewModel.class);
        adapter = new ModsAdapter(this);

        binding.recyclerMods.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerMods.setAdapter(adapter);
        binding.swipeRefresh.setColorSchemeResources(com.gpbmods.app.R.color.brand_primary);
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadAll());
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.btnNewMod.setOnClickListener(v -> onEdit(new ModDto()));
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
        {
            boolean isLoading = Boolean.TRUE.equals(loading);
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!isLoading) {
                binding.swipeRefresh.setRefreshing(false);
            }
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.isEmpty()) binding.tvError.setVisibility(View.GONE);
            else {
                binding.tvError.setText(msg);
                binding.tvError.setVisibility(View.VISIBLE);
            }
        });
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.isEmpty()) binding.tvSuccess.setVisibility(View.GONE);
            else {
                binding.tvSuccess.setText(msg);
                binding.tvSuccess.setVisibility(View.VISIBLE);
            }
        });
        viewModel.getMods().observe(getViewLifecycleOwner(), list -> {
            allMods = list == null ? new ArrayList<>() : new ArrayList<>(list);
            applyFilter();
        });
        viewModel.loadAll();
    }

    private void applyFilter() {
        String term = value(binding.etSearch.getText()).toLowerCase(Locale.ROOT);
        if (term.isEmpty()) {
            adapter.submitList(allMods);
            binding.tvEmpty.setVisibility(allMods.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        List<ModDto> filtered = new ArrayList<>();
        for (ModDto mod : allMods) {
            String category = mod.categoria == null ? "" : safe(mod.categoria.nombre);
            String haystack = (safe(mod.nombre) + " " + safe(mod.version) + " " + safe(mod.descripcion) + " " + category).toLowerCase(Locale.ROOT);
            if (haystack.contains(term)) {
                filtered.add(mod);
            }
        }
        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String value(Editable e) {
        return e == null ? "" : e.toString().trim();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    @Override
    public void onEdit(ModDto mod) {
        Bundle args = new Bundle();
        if (mod.id != null) args.putLong("modId", mod.id);
        args.putString("nombre", mod.nombre);
        args.putString("descripcion", mod.descripcion);
        args.putString("version", mod.version);
        args.putString("archivoOriginal", mod.archivoOriginal);
        args.putString("youtubeUrl", mod.youtubeUrl);
        args.putString("carpetaBaseMod", mod.carpetaBaseMod);
        args.putString("categoria", mod.categoria != null ? mod.categoria.nombre : "");
        args.putDouble("precio", mod.precio == null ? 0 : mod.precio);
        args.putBoolean("destacadoHome", Boolean.TRUE.equals(mod.destacadoHome));
        if (mod.ordenShowroom != null) {
            args.putInt("ordenShowroom", mod.ordenShowroom);
        }
        Navigation.findNavController(binding.getRoot()).navigate(com.gpbmods.app.R.id.action_modsFragment_to_editModFragment, args);
    }

    @Override
    public void onDelete(ModDto mod) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar mod")
                .setMessage("¿Seguro que quieres eliminar " + (mod.nombre == null ? "este mod" : mod.nombre) + "?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (d, w) -> viewModel.deleteMod(mod))
                .show();
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
            viewModel.loadAll();
        }
    }
}
