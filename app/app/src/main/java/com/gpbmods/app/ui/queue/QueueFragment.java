package com.gpbmods.app.ui.queue;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.gpbmods.app.data.remote.dto.EncryptionOverviewResponseDto;
import com.gpbmods.app.data.remote.dto.EncryptionJobOverviewItemDto;
import com.gpbmods.app.databinding.FragmentQueueBinding;
import com.gpbmods.app.viewmodel.QueueViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QueueFragment extends Fragment {

    private FragmentQueueBinding binding;
    private QueueViewModel viewModel;
    private QueueJobsAdapter adapter;
    private List<EncryptionJobOverviewItemDto> allJobs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentQueueBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(QueueViewModel.class);
        adapter = new QueueJobsAdapter();

        String[] statusFilters = new String[]{"Todos", "PENDING", "RUNNING", "DONE", "FAILED"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(), com.gpbmods.app.R.layout.spinner_item_light, statusFilters);
        statusAdapter.setDropDownViewResource(com.gpbmods.app.R.layout.spinner_item_dropdown_light);
        binding.spStatusFilter.setAdapter(statusAdapter);
        binding.spStatusFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.recyclerJobs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerJobs.setAdapter(adapter);
        binding.swipeRefresh.setColorSchemeResources(com.gpbmods.app.R.color.brand_primary);
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadOverview());
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

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
        viewModel.getOverview().observe(getViewLifecycleOwner(), this::renderOverview);

        viewModel.loadOverview();
    }

    private void renderOverview(EncryptionOverviewResponseDto overview) {
        if (overview == null || binding == null) return;
        binding.tvSummary.setText(
                "Pend: " + overview.pending + " | Run: " + overview.running + " | Done: " + overview.done + " | Fail: " + overview.failed
        );
        binding.tvMail.setText("SMTP: " + (overview.mailConfigured ? "OK" : "No") + " - " + (overview.mailHost == null ? "-" : overview.mailHost));
        allJobs = overview.recent == null ? new ArrayList<>() : new ArrayList<>(overview.recent);
        applyFilters();
    }

    private void applyFilters() {
        if (binding == null) return;
        String term = binding.etSearch.getText() == null ? "" : binding.etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        String selected = binding.spStatusFilter.getSelectedItem() == null ? "Todos" : binding.spStatusFilter.getSelectedItem().toString();
        String statusFilter = "Todos".equalsIgnoreCase(selected) ? "" : selected.toUpperCase(Locale.ROOT);

        List<EncryptionJobOverviewItemDto> filtered = new ArrayList<>();
        for (EncryptionJobOverviewItemDto job : allJobs) {
            String status = safe(job.status).toUpperCase(Locale.ROOT);
            if (!statusFilter.isEmpty() && !status.equals(statusFilter)) {
                continue;
            }
            if (!term.isEmpty()) {
                String haystack = ("#" + job.id + " " + safe(job.mod) + " " + safe(job.userEmail) + " " + safe(job.guid) + " " + safe(job.errorMessage)).toLowerCase(Locale.ROOT);
                if (!haystack.contains(term)) {
                    continue;
                }
            }
            filtered.add(job);
        }

        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
