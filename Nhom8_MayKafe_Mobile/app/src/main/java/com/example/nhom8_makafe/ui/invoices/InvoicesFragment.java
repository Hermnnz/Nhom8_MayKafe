package com.example.nhom8_makafe.ui.invoices;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.adapter.InvoiceAdapter;
import com.example.nhom8_makafe.data.api.ApiCallback;
import com.example.nhom8_makafe.data.api.ApiRepository;
import com.example.nhom8_makafe.databinding.FragmentInvoicesBinding;
import com.example.nhom8_makafe.model.Invoice;
import com.example.nhom8_makafe.model.InvoiceSummaryData;
import com.example.nhom8_makafe.model.OrderStatus;
import com.example.nhom8_makafe.util.FeedbackSnackbar;
import com.example.nhom8_makafe.util.FormatUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class InvoicesFragment extends Fragment {
    private FragmentInvoicesBinding binding;
    private final ApiRepository apiRepository = ApiRepository.getInstance();
    private InvoiceAdapter adapter;
    private String searchQuery = "";
    private String selectedDate = "";
    private StatusFilter statusFilter = StatusFilter.ALL;
    private int baseHeaderTopPadding;

    private enum StatusFilter {
        ALL,
        PAID,
        CANCELLED
    }

    public static InvoicesFragment newInstance() {
        return new InvoicesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInvoicesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        baseHeaderTopPadding = binding.layoutHeaderContent.getPaddingTop();
        applyInsets();

        adapter = new InvoiceAdapter(new InvoiceAdapter.InvoiceActionListener() {
            @Override
            public void onShowDetail(Invoice invoice) {
                showDetailDialog(invoice);
            }

            @Override
            public void onPrint(Invoice invoice) {
                FeedbackSnackbar.showPrinting(binding.getRoot(), invoice.getId());
            }
        });
        binding.recyclerInvoices.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerInvoices.setAdapter(adapter);

        binding.editSearchInvoice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchQuery = s == null ? "" : s.toString().trim();
                loadInvoices();
            }
        });

        setupFocusDismiss();
        binding.layoutDateField.setOnClickListener(v -> openDatePicker());
        binding.imageDateAction.setOnClickListener(v -> {
            if (selectedDate.isEmpty()) {
                openDatePicker();
            } else {
                selectedDate = "";
                loadInvoices();
            }
        });

        binding.buttonFilterAll.setOnClickListener(v -> setStatusFilter(StatusFilter.ALL));
        binding.buttonFilterPaid.setOnClickListener(v -> setStatusFilter(StatusFilter.PAID));
        binding.buttonFilterCancelled.setOnClickListener(v -> setStatusFilter(StatusFilter.CANCELLED));
        loadInvoices();
    }

    private void setupFocusDismiss() {
        View.OnTouchListener dismissListener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                maybeClearSearchFocus(event);
            }
            return false;
        };
        binding.scrollHeader.setOnTouchListener(dismissListener);
        binding.recyclerInvoices.setOnTouchListener(dismissListener);
        binding.textEmptyInvoice.setOnTouchListener(dismissListener);
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollHeader, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.layoutHeaderContent.setPadding(
                    binding.layoutHeaderContent.getPaddingLeft(),
                    baseHeaderTopPadding + systemBars.top,
                    binding.layoutHeaderContent.getPaddingRight(),
                    binding.layoutHeaderContent.getPaddingBottom()
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(binding.scrollHeader);
    }

    private void setStatusFilter(StatusFilter filter) {
        statusFilter = filter;
        loadInvoices();
    }

    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            loadInvoices();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadInvoices() {
        if (binding == null) {
            return;
        }
        apiRepository.fetchInvoices(searchQuery, selectedDate, mapStatusFilter(statusFilter), new ApiCallback<List<Invoice>>() {
            @Override
            public void onSuccess(List<Invoice> data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                adapter.submitList(data);
                binding.textEmptyInvoice.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
                binding.textResultCount.setText(data.size() + " h\u00f3a \u0111\u01a1n");
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || binding == null) {
                    return;
                }
                adapter.submitList(new ArrayList<>());
                binding.textEmptyInvoice.setVisibility(View.VISIBLE);
                binding.textEmptyInvoice.setText(message == null || message.trim().isEmpty()
                        ? "Kh\u00f4ng t\u1ea3i \u0111\u01b0\u1ee3c h\u00f3a \u0111\u01a1n."
                        : message);
                binding.textResultCount.setText("0 h\u00f3a \u0111\u01a1n");
            }
        });

        apiRepository.fetchInvoiceSummary(selectedDate, new ApiCallback<InvoiceSummaryData>() {
            @Override
            public void onSuccess(InvoiceSummaryData data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                binding.textPaidValue.setText(String.valueOf(data.getPaidCount()));
                binding.textCancelledValue.setText(String.valueOf(data.getCancelledCount()));
                binding.textRevenueValue.setText(formatRevenueSummary(data.getRevenue()));
                bindDateField();
                updateFilterButtons();
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || binding == null) {
                    return;
                }
                binding.textPaidValue.setText("0");
                binding.textCancelledValue.setText("0");
                binding.textRevenueValue.setText(formatRevenueSummary(0));
                bindDateField();
                updateFilterButtons();
            }
        });
    }

    private OrderStatus mapStatusFilter(StatusFilter filter) {
        if (filter == StatusFilter.PAID) {
            return OrderStatus.PAID;
        }
        if (filter == StatusFilter.CANCELLED) {
            return OrderStatus.CANCELLED;
        }
        return null;
    }

    private void bindDateField() {
        if (selectedDate.isEmpty()) {
            binding.textDateValue.setText("dd/mm/yyyy");
            binding.textDateValue.setTextColor(requireContext().getColor(R.color.coffee_950));
            binding.imageDateAction.setImageResource(R.drawable.ic_calendar_outline);
            binding.imageDateAction.setColorFilter(requireContext().getColor(R.color.coffee_950));
        } else {
            binding.textDateValue.setText(formatDisplayDate(selectedDate));
            binding.textDateValue.setTextColor(requireContext().getColor(R.color.coffee_950));
            binding.imageDateAction.setImageResource(R.drawable.ic_close_small);
            binding.imageDateAction.setColorFilter(requireContext().getColor(R.color.coffee_500));
        }
    }

    private void maybeClearSearchFocus(MotionEvent event) {
        if (!binding.editSearchInvoice.hasFocus()) {
            return;
        }
        int[] location = new int[2];
        binding.layoutSearchField.getLocationOnScreen(location);
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        boolean insideSearch = rawX >= location[0]
                && rawX <= location[0] + binding.layoutSearchField.getWidth()
                && rawY >= location[1]
                && rawY <= location[1] + binding.layoutSearchField.getHeight();
        if (insideSearch) {
            return;
        }
        binding.editSearchInvoice.clearFocus();
        hideKeyboard();
    }

    private void hideKeyboard() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        InputMethodManager inputMethodManager =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(binding.editSearchInvoice.getWindowToken(), 0);
        }
    }

    private void updateFilterButtons() {
        bindFilterState(binding.buttonFilterAll, statusFilter == StatusFilter.ALL);
        bindFilterState(binding.buttonFilterPaid, statusFilter == StatusFilter.PAID);
        bindFilterState(binding.buttonFilterCancelled, statusFilter == StatusFilter.CANCELLED);
    }

    private void bindFilterState(AppCompatButton button, boolean selected) {
        button.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        button.setTextColor(requireContext().getColor(selected ? R.color.white : R.color.coffee_500));
    }

    private String formatDisplayDate(String isoDate) {
        String[] parts = isoDate.split("-");
        if (parts.length != 3) {
            return isoDate;
        }
        return parts[2] + "/" + parts[1] + "/" + parts[0];
    }

    private String formatRevenueSummary(int revenue) {
        if (revenue >= 1000) {
            return (revenue / 1000) + " N\u0111";
        }
        return FormatUtils.formatCurrency(revenue);
    }

    private void showDetailDialog(Invoice invoice) {
        InvoiceDetailBottomSheetDialogFragment.newInstance(invoice.getId())
                .show(getChildFragmentManager(), "invoice_detail_sheet");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            loadInvoices();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
