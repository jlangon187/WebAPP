package com.gpbmods.app.ui.mods;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gpbmods.app.data.remote.dto.ModDto;
import com.gpbmods.app.databinding.ItemModBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModsAdapter extends RecyclerView.Adapter<ModsAdapter.ModViewHolder> {

    public interface Listener {
        void onEdit(ModDto mod);
        void onDelete(ModDto mod);
    }

    private final Listener listener;
    private final List<ModDto> items = new ArrayList<>();

    public ModsAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<ModDto> mods) {
        items.clear();
        if (mods != null) items.addAll(mods);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ModViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemModBinding binding = ItemModBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ModViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ModViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ModViewHolder extends RecyclerView.ViewHolder {
        private final ItemModBinding binding;

        ModViewHolder(ItemModBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ModDto mod) {
            binding.tvName.setText(safe(mod.nombre));
            binding.tvVersion.setText("Version: " + safe(mod.version));
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
            binding.tvPrice.setText(nf.format(mod.precio == null ? 0 : mod.precio));
            String categoria = mod.categoria != null ? safe(mod.categoria.nombre) : "Sin categoria";
            binding.tvCategory.setText("Categoria: " + categoria);
            binding.btnEdit.setOnClickListener(v -> listener.onEdit(mod));
            binding.btnDelete.setOnClickListener(v -> listener.onDelete(mod));
        }

        private String safe(String text) {
            return text == null ? "" : text;
        }
    }
}
