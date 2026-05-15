package com.gpbmods.app.ui.queue;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gpbmods.app.data.remote.dto.EncryptionJobOverviewItemDto;
import com.gpbmods.app.databinding.ItemQueueJobBinding;

import java.util.ArrayList;
import java.util.List;

public class QueueJobsAdapter extends RecyclerView.Adapter<QueueJobsAdapter.JobViewHolder> {

    private final List<EncryptionJobOverviewItemDto> items = new ArrayList<>();

    public void submitList(List<EncryptionJobOverviewItemDto> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemQueueJobBinding binding = ItemQueueJobBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new JobViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {
        private final ItemQueueJobBinding binding;

        JobViewHolder(ItemQueueJobBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(EncryptionJobOverviewItemDto job) {
            String status = safe(job.status);
            binding.tvTitle.setText("Job #" + job.id);
            binding.tvStatus.setText(status.isEmpty() ? "-" : status);
            if ("FAILED".equalsIgnoreCase(status)) {
                binding.tvStatus.setBackgroundResource(com.gpbmods.app.R.drawable.bg_chip_error);
            } else if ("DONE".equalsIgnoreCase(status)) {
                binding.tvStatus.setBackgroundResource(com.gpbmods.app.R.drawable.bg_chip_success);
            } else if ("RUNNING".equalsIgnoreCase(status)) {
                binding.tvStatus.setBackgroundResource(com.gpbmods.app.R.drawable.bg_chip_info);
            } else {
                binding.tvStatus.setBackgroundResource(com.gpbmods.app.R.drawable.bg_chip_warning);
            }
            binding.tvMod.setText("Mod: " + safe(job.mod));
            binding.tvUser.setText("Usuario: " + safe(job.userEmail));
            binding.tvGuid.setText("GUID: " + safe(job.guid));
            binding.tvTime.setText("Creado: " + formatDate(safe(job.createdAt)) + " | Actualizado: " + formatDate(safe(job.updatedAt)));
            binding.tvLifecycle.setText("Expira: " + formatDate(safe(job.expiresAt)) + " | Notificado: " + formatDate(safe(job.notifiedAt)));
            binding.tvError.setText(job.errorMessage == null || job.errorMessage.isEmpty() ? "" : ("Error: " + job.errorMessage));
        }

        private String safe(String s) {
            return s == null ? "" : s;
        }

        private String formatDate(String raw) {
            if (raw == null || raw.isEmpty()) return "-";
            if (raw.length() >= 16) return raw.substring(0, 16).replace('T', ' ');
            return raw.replace('T', ' ');
        }
    }
}
