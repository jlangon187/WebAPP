package com.gpbmods.app.ui.mods;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.gpbmods.app.data.remote.dto.CategoriaDto;
import com.gpbmods.app.data.remote.dto.ModDto;
import com.gpbmods.app.data.repository.AdminRepository;
import com.gpbmods.app.databinding.FragmentEditModBinding;

import java.util.ArrayList;
import java.util.List;

public class EditModFragment extends Fragment {
    private FragmentEditModBinding binding;
    private Long modId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEditModBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args != null && args.containsKey("modId")) modId = args.getLong("modId");

        binding.etNombre.setText(args == null ? "" : args.getString("nombre", ""));
        binding.etDescripcion.setText(args == null ? "" : args.getString("descripcion", ""));
        binding.etVersion.setText(args == null ? "" : args.getString("version", ""));
        binding.etArchivoOriginal.setText(args == null ? "" : args.getString("archivoOriginal", ""));
        binding.etYoutubeUrl.setText(args == null ? "" : args.getString("youtubeUrl", ""));
        binding.etCarpetaBaseMod.setText(args == null ? "" : args.getString("carpetaBaseMod", ""));
        binding.etPrecio.setText(String.valueOf(args == null ? 0 : args.getDouble("precio", 0)));
        binding.switchDestacadoHome.setChecked(args != null && args.getBoolean("destacadoHome", false));
        int ordenShowroom = (args != null && args.containsKey("ordenShowroom")) ? args.getInt("ordenShowroom", 0) : 0;
        binding.etOrdenShowroom.setText(ordenShowroom > 0 ? String.valueOf(ordenShowroom) : "");

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.btnSave.setOnClickListener(v -> save());

        loadCategories(args == null ? "" : args.getString("categoria", ""));
    }

    private void loadCategories(String selected) {
        new AdminRepository(requireContext()).getCategorias(new com.gpbmods.app.core.util.RepositoryCallback<>() {
            @Override
            public void onSuccess(List<CategoriaDto> data) {
                if (!isAdded()) return;
                List<String> names = new ArrayList<>();
                for (CategoriaDto c : data) names.add(c.nombre == null ? "" : c.nombre);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), com.gpbmods.app.R.layout.spinner_item_light, names);
                adapter.setDropDownViewResource(com.gpbmods.app.R.layout.spinner_item_dropdown_light);
                binding.spCategoria.setAdapter(adapter);
                for (int i = 0; i < names.size(); i++) if (names.get(i).equalsIgnoreCase(selected)) { binding.spCategoria.setSelection(i); break; }
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    private void save() {
        ModDto mod = new ModDto();
        mod.id = modId;
        mod.nombre = value(binding.etNombre.getText());
        mod.descripcion = value(binding.etDescripcion.getText());
        mod.version = value(binding.etVersion.getText());
        mod.archivoOriginal = value(binding.etArchivoOriginal.getText());
        mod.youtubeUrl = value(binding.etYoutubeUrl.getText());
        mod.carpetaBaseMod = value(binding.etCarpetaBaseMod.getText());
        mod.destacadoHome = binding.switchDestacadoHome.isChecked();
        String ordenText = value(binding.etOrdenShowroom.getText());
        try { mod.ordenShowroom = ordenText.isEmpty() ? null : Integer.parseInt(ordenText); } catch (Exception ex) { mod.ordenShowroom = null; }
        try { mod.precio = Double.parseDouble(value(binding.etPrecio.getText())); } catch (Exception ex) { mod.precio = null; }

        if (mod.nombre.isEmpty() || mod.descripcion.isEmpty() || mod.version.isEmpty() || mod.archivoOriginal.isEmpty() || mod.precio == null) {
            binding.tvError.setText("Completa nombre, descripcion, version, archivo y precio valido.");
            binding.tvError.setVisibility(View.VISIBLE);
            return;
        }

        if (binding.spCategoria.getSelectedItem() != null) {
            CategoriaDto cat = new CategoriaDto();
            cat.nombre = binding.spCategoria.getSelectedItem().toString();
            mod.categoria = cat;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        AdminRepository repo = new AdminRepository(requireContext());
        com.gpbmods.app.core.util.RepositoryCallback<ModDto> cb = new com.gpbmods.app.core.util.RepositoryCallback<>() {
            @Override
            public void onSuccess(ModDto data) {
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
        };
        if (modId == null) repo.createMod(mod, cb); else repo.updateMod(modId, mod, cb);
    }

    private String value(android.text.Editable e) { return e == null ? "" : e.toString().trim(); }
}
