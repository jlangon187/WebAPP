package com.gpbmods.app.ui.tickets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gpbmods.app.data.remote.dto.TicketDto;
import com.gpbmods.app.databinding.ItemTicketBinding;

import java.util.ArrayList;
import java.util.List;

public class TicketsAdapter extends RecyclerView.Adapter<TicketsAdapter.TicketViewHolder> {

    public interface Listener {
        void onReply(TicketDto ticket);
        void onClose(TicketDto ticket);
    }

    private final Listener listener;
    private final List<TicketDto> items = new ArrayList<>();

    public TicketsAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<TicketDto> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTicketBinding binding = ItemTicketBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TicketViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class TicketViewHolder extends RecyclerView.ViewHolder {
        private final ItemTicketBinding binding;

        TicketViewHolder(ItemTicketBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TicketDto ticket) {
            binding.tvTicketTitle.setText("Ticket #" + ticket.id);
            String state = safe(ticket.estado);
            binding.tvState.setText(state.isEmpty() ? "-" : state.toUpperCase());
            if ("cerrado".equalsIgnoreCase(state)) {
                binding.tvState.setBackgroundResource(com.gpbmods.app.R.drawable.bg_chip_error);
            } else if ("respondido".equalsIgnoreCase(state)) {
                binding.tvState.setBackgroundResource(com.gpbmods.app.R.drawable.bg_chip_info);
            } else {
                binding.tvState.setBackgroundResource(com.gpbmods.app.R.drawable.bg_chip_warning);
            }
            String owner = ticket.usuario != null ? (safe(ticket.usuario.nombre) + " <" + safe(ticket.usuario.email) + ">") : "-";
            binding.tvOwner.setText(owner);
            binding.tvDate.setText(formatDate(safe(ticket.creadoEn)));
            binding.tvMessage.setText(safe(ticket.mensaje));

            boolean closed = "cerrado".equalsIgnoreCase(safe(ticket.estado));
            binding.btnClose.setEnabled(!closed);
            binding.btnReply.setEnabled(!closed);
            binding.btnReply.setOnClickListener(v -> listener.onReply(ticket));
            binding.btnClose.setOnClickListener(v -> listener.onClose(ticket));
        }

        private String safe(String s) {
            return s == null ? "" : s;
        }

        private String formatDate(String raw) {
            if (raw == null || raw.isEmpty()) return "";
            if (raw.length() >= 16) {
                return raw.substring(0, 16).replace('T', ' ');
            }
            return raw.replace('T', ' ');
        }
    }
}
