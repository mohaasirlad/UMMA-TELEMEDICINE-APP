package com.example.ummatelemedicineapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ummatelemedicineapp.R;
import com.example.ummatelemedicineapp.adapters.TransactionAdapter;
import com.example.ummatelemedicineapp.database.AppDatabase;
import com.example.ummatelemedicineapp.models.Appointment;
import com.example.ummatelemedicineapp.models.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EarningsReportsFragment extends Fragment {

    private TextView tvTotalEarnings;
    private RecyclerView rvTransactions;
    private AppDatabase db;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_earnings_reports, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbarEarnings);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        tvTotalEarnings = view.findViewById(R.id.tvTotalEarningsAmount);
        rvTransactions = view.findViewById(R.id.rvTransactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));

        db = AppDatabase.getInstance(requireContext());
        loadEarningsData();

        return view;
    }

    private void loadEarningsData() {
        executorService.execute(() -> {
            List<Appointment> allAppointments = db.appointmentDao().getAllAppointments();
            
            // Professional Logic: Only count 'Paid' appointments
            // Use the fee from SharedPreferences if available
            android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("UMMA_PREFS", android.content.Context.MODE_PRIVATE);
            String feeStr = prefs.getString("doctor_fees", "$50").replaceAll("[^0-9]", "");
            double feePerConsultation = feeStr.isEmpty() ? 50.0 : Double.parseDouble(feeStr);
            
            double total = 0;
            List<Transaction> transactions = new ArrayList<>();
            
            for (Appointment app : allAppointments) {
                if ("Paid".equalsIgnoreCase(app.getPaymentStatus())) {
                    total += feePerConsultation;
                    transactions.add(new Transaction(
                            getString(R.string.transaction_consultation_label, app.getPatientName()),
                            app.getDate() + " • " + app.getTime(),
                            "+" + getString(R.string.currency_symbol_usd) + String.format(Locale.getDefault(), "%.2f", feePerConsultation),
                            R.drawable.ic_consultation
                    ));
                }
            }

            // Fallback for demo if no real 'Paid' records exist yet
            if (transactions.isEmpty()) {
                transactions.add(new Transaction(getString(R.string.system_bonus), "Oct 1, 2023", getString(R.string.currency_symbol_usd) + "100.00", R.drawable.ic_consultation));
                total = 100.0;
            }
            
            final double finalTotal = total;
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    tvTotalEarnings.setText(getString(R.string.currency_format_usd, finalTotal));
                    rvTransactions.setAdapter(new TransactionAdapter(transactions));
                });
            }
        });
    }
}
