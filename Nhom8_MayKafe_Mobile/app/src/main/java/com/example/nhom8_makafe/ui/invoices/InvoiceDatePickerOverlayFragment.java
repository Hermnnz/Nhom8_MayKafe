package com.example.nhom8_makafe.ui.invoices;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nhom8_makafe.databinding.DialogInvoiceDatePickerBinding;
import com.example.nhom8_makafe.ui.overlay.OverlayFragment;

import java.util.Locale;

public class InvoiceDatePickerOverlayFragment extends OverlayFragment {
    public static final String TAG = "invoice_date_picker_overlay";
    public static final String REQUEST_KEY = "invoice_date_picker_request";
    public static final String RESULT_DATE = "date";

    private static final String ARG_SELECTED_DATE = "selected_date";

    @Nullable
    private DialogInvoiceDatePickerBinding binding;

    public static InvoiceDatePickerOverlayFragment newInstance(@Nullable String selectedDate) {
        InvoiceDatePickerOverlayFragment fragment = new InvoiceDatePickerOverlayFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_DATE, selectedDate);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogInvoiceDatePickerBinding.inflate(inflater, container, false);
        return createTopDialogOverlay(binding.getRoot(), 0.9f, 440, 32, true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding == null) {
            return;
        }
        bindInitialDate(binding.datePicker);
        binding.buttonCancelDate.setOnClickListener(v -> dismissAllowingStateLoss());
        binding.buttonConfirmDate.setOnClickListener(v -> {
            String selectedDate = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    binding.datePicker.getYear(),
                    binding.datePicker.getMonth() + 1,
                    binding.datePicker.getDayOfMonth()
            );
            Bundle result = new Bundle();
            result.putString(RESULT_DATE, selectedDate);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            dismissAllowingStateLoss();
        });
    }

    private void bindInitialDate(@NonNull DatePicker datePicker) {
        String isoDate = getArguments() == null ? "" : getArguments().getString(ARG_SELECTED_DATE, "");
        if (isoDate == null || isoDate.trim().isEmpty()) {
            return;
        }
        String[] parts = isoDate.split("-");
        if (parts.length != 3) {
            return;
        }
        try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int day = Integer.parseInt(parts[2]);
            datePicker.updateDate(year, month, day);
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
