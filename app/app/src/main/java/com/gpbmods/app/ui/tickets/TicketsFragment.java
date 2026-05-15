package com.gpbmods.app.ui.tickets;

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

import com.gpbmods.app.data.remote.dto.TicketDto;
import com.gpbmods.app.databinding.DialogReplyTicketBinding;
import com.gpbmods.app.databinding.FragmentTicketsBinding;
import com.gpbmods.app.viewmodel.TicketsViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TicketsFragment extends Fragment implements TicketsAdapter.Listener {

    private FragmentTicketsBinding binding;
    private TicketsViewModel viewModel;
    private TicketsAdapter adapter;
    private List<TicketDto> allTickets = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTicketsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TicketsViewModel.class);
        adapter = new TicketsAdapter(this);

        binding.recyclerTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTickets.setAdapter(adapter);
        binding.swipeRefresh.setColorSchemeResources(com.gpbmods.app.R.color.brand_primary);
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadTickets());
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
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

        viewModel.getTickets().observe(getViewLifecycleOwner(), list -> {
            allTickets = list == null ? new ArrayList<>() : new ArrayList<>(list);
            applyFilter();
        });
        viewModel.loadTickets();
    }

    private void applyFilter() {
        String term = value(binding.etSearch.getText()).toLowerCase(Locale.ROOT);
        if (term.isEmpty()) {
            adapter.submitList(allTickets);
            binding.tvEmpty.setVisibility(allTickets.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        List<TicketDto> filtered = new ArrayList<>();
        for (TicketDto ticket : allTickets) {
            String userEmail = ticket.usuario == null ? "" : safe(ticket.usuario.email);
            String userName = ticket.usuario == null ? "" : safe(ticket.usuario.nombre);
            String haystack = ("#" + ticket.id + " " + userEmail + " " + userName + " " + safe(ticket.mensaje) + " " + safe(ticket.estado)).toLowerCase(Locale.ROOT);
            if (haystack.contains(term)) {
                filtered.add(ticket);
            }
        }
        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void onReply(TicketDto ticket) {
        DialogReplyTicketBinding dialogBinding = DialogReplyTicketBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Responder ticket #" + ticket.id)
                .setView(dialogBinding.getRoot())
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Enviar", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String response = value(dialogBinding.etResponse.getText());
            if (response.isEmpty()) {
                dialogBinding.tvDialogError.setText("La respuesta es obligatoria.");
                dialogBinding.tvDialogError.setVisibility(View.VISIBLE);
                return;
            }
            viewModel.replyTicket(ticket, response);
            dialog.dismiss();
        }));
        dialog.show();
    }

    @Override
    public void onClose(TicketDto ticket) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cerrar ticket")
                .setMessage("¿Seguro que quieres cerrar el ticket #" + ticket.id + "?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Cerrar", (d, w) -> viewModel.closeTicket(ticket))
                .show();
    }

    private String value(Editable e) {
        return e == null ? "" : e.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
