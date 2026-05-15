package com.gpbmods.app.ui.users;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gpbmods.app.data.remote.dto.AdminUserDto;
import com.gpbmods.app.databinding.ItemAdminUserBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    public interface Listener {
        void onEdit(AdminUserDto user);
        void onPurchases(AdminUserDto user);
    }

    private final Listener listener;
    private final List<AdminUserDto> items = new ArrayList<>();

    public UsersAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<AdminUserDto> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminUserBinding binding = ItemAdminUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminUserBinding binding;

        UserViewHolder(ItemAdminUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AdminUserDto user) {
            binding.tvName.setText(user.nombre);
            binding.tvEmail.setText(user.email);
            binding.tvRole.setText("Rol: " + user.rol);
            binding.tvGuid.setText("GUID: " + (user.guid == null || user.guid.isEmpty() ? "N/D" : user.guid));
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
            binding.tvMeta.setText("Compras: " + user.purchasesCount + " | Tickets: " + user.ticketsCount + " | " + nf.format(user.totalSpent));
            binding.tvStatus.setText(user.activo ? "Activo" : "Desactivado");
            binding.tvStatus.setBackgroundResource(user.activo ? com.gpbmods.app.R.drawable.bg_chip_success : com.gpbmods.app.R.drawable.bg_chip_error);
            binding.btnEdit.setOnClickListener(v -> listener.onEdit(user));
            binding.btnPurchases.setOnClickListener(v -> listener.onPurchases(user));
        }
    }
}
