package com.gpbmods.app.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.gpbmods.app.R;
import com.gpbmods.app.data.remote.dto.AdminStatsResponse;
import com.gpbmods.app.data.remote.dto.EncryptionOverviewResponseDto;
import com.gpbmods.app.databinding.FragmentAdminDashboardBinding;
import com.gpbmods.app.viewmodel.DashboardViewModel;

public class AdminDashboardFragment extends Fragment {

    private FragmentAdminDashboardBinding binding;
    private DashboardViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        binding.btnLogout.setOnClickListener(v -> {
            viewModel.logout();
            Navigation.findNavController(view).navigate(R.id.action_adminDashboardFragment_to_loginFragment);
        });

        binding.btnUsers.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_adminDashboardFragment_to_usersFragment));
        binding.btnTickets.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_adminDashboardFragment_to_ticketsFragment));
        binding.btnMods.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_adminDashboardFragment_to_modsFragment));
        binding.btnQueue.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_adminDashboardFragment_to_queueFragment));

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error == null || error.isEmpty()) {
                binding.tvError.setVisibility(View.GONE);
            } else {
                binding.tvError.setText(error);
                binding.tvError.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getStats().observe(getViewLifecycleOwner(), this::renderStats);
        viewModel.getEncryptionOverview().observe(getViewLifecycleOwner(), this::renderEncryptionOverview);

        viewModel.loadStats();
    }

    private void renderStats(AdminStatsResponse stats) {
        if (stats == null || binding == null) {
            return;
        }
        binding.tvTotalSales.setText(String.format("%.2f EUR", stats.totalSales));
        binding.tvSalesCount.setText("Compras totales: " + stats.totalSalesCount + " | 30d: " + stats.salesCountLast30);
        binding.tvNewUsers.setText(String.valueOf(stats.totalUsers));
        binding.tvUsersMeta.setText("Nuevos 30d: " + stats.newUsers);
        binding.tvActiveTickets.setText(String.valueOf(stats.totalTickets));
        binding.tvTicketsMeta.setText("Activos: " + stats.activeTickets + " | Cerrados: " + stats.closedTickets);
        binding.tvModsMeta.setText("Mods: " + stats.totalMods + " | Showroom: " + stats.featuredMods);

        if (stats.nas != null) {
            binding.tvNasOnline.setText("Online: " + (stats.nas.online ? "SI" : "NO"));
            binding.tvNasStorage.setText("Uso: " + formatBytes(stats.nas.usedBytes) + " / " + formatBytes(stats.nas.totalBytes) + " (" + stats.nas.usagePercent + "%)");
            binding.tvNasFiles.setText("Archivos: home=" + stats.nas.homeImagesCount + " | mods=" + stats.nas.modsFilesCount);
        } else {
            binding.tvNasOnline.setText("Online: -");
            binding.tvNasStorage.setText("Uso: -");
            binding.tvNasFiles.setText("Archivos: -");
        }
    }

    private void renderEncryptionOverview(EncryptionOverviewResponseDto overview) {
        if (overview == null || binding == null) {
            return;
        }
        binding.tvQueueStatus.setText(
                "Pend: " + overview.pending +
                " | Run: " + overview.running +
                " | Done: " + overview.done +
                " | Fail: " + overview.failed
        );
        String smtpText = "SMTP: " + (overview.mailConfigured ? "Configurado" : "No configurado") +
                (overview.mailHost == null || overview.mailHost.isEmpty() ? "" : " (" + overview.mailHost + ")") +
                " | Sin aviso: " + overview.doneWithoutNotification;
        binding.tvSmtpStatus.setText(smtpText);
    }

    private String formatBytes(long value) {
        if (value <= 0) return "0 B";
        String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        double size = value;
        int i = 0;
        while (size >= 1024 && i < units.length - 1) {
            size /= 1024;
            i++;
        }
        return String.format(i == 0 ? "%.0f %s" : "%.1f %s", size, units[i]);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
