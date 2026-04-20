package com.example.nhom8_makafe.ui.invoices;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.adapter.InvoiceDetailItemAdapter;
import com.example.nhom8_makafe.data.api.ApiCallback;
import com.example.nhom8_makafe.data.api.ApiRepository;
import com.example.nhom8_makafe.databinding.BottomSheetInvoiceDetailBinding;
import com.example.nhom8_makafe.databinding.IncludeBottomSheetHeaderBinding;
import com.example.nhom8_makafe.model.Invoice;
import com.example.nhom8_makafe.model.OrderStatus;
import com.example.nhom8_makafe.util.FeedbackSnackbar;
import com.example.nhom8_makafe.util.FormatUtils;
import com.example.nhom8_makafe.ui.overlay.OverlayFragment;

public class InvoiceDetailBottomSheetDialogFragment extends OverlayFragment {
    private static final String ARG_INVOICE_ID = "invoice_id";

    private BottomSheetInvoiceDetailBinding binding;
    private IncludeBottomSheetHeaderBinding headerBinding;
    private final ApiRepository apiRepository = ApiRepository.getInstance();
    private InvoiceDetailItemAdapter adapter;
    private Invoice currentInvoice;

    public static InvoiceDetailBottomSheetDialogFragment newInstance(String invoiceId) {
        InvoiceDetailBottomSheetDialogFragment fragment = new InvoiceDetailBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INVOICE_ID, invoiceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetInvoiceDetailBinding.inflate(inflater, container, false);
        headerBinding = IncludeBottomSheetHeaderBinding.bind(binding.getRoot());
        return createBottomSheetOverlay(binding.getRoot(), true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new InvoiceDetailItemAdapter();
        binding.recyclerDetailItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerDetailItems.setAdapter(adapter);
        if (headerBinding != null) {
            headerBinding.buttonCloseSheet.setOnClickListener(v -> dismissAllowingStateLoss());
        }
        binding.buttonPrintInvoice.setOnClickListener(v -> {
            if (currentInvoice != null && binding.getRoot() instanceof FrameLayout) {
                FeedbackSnackbar.showPrintingOverlay((FrameLayout) binding.getRoot(), currentInvoice.getId());
            }
        });
        loadInvoice();
    }

    private void loadInvoice() {
        String invoiceId = getArguments() == null ? null : getArguments().getString(ARG_INVOICE_ID);
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            dismissAllowingStateLoss();
            return;
        }
        apiRepository.fetchInvoiceByCode(invoiceId, new ApiCallback<Invoice>() {
            @Override
            public void onSuccess(Invoice data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                currentInvoice = data;
                bindInvoice(data);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }
                dismissAllowingStateLoss();
            }
        });
    }

    private void bindInvoice(Invoice invoice) {
        if (headerBinding != null) {
            headerBinding.textSheetTitle.setText("H\u00f3a \u0111\u01a1n " + invoice.getId());
            headerBinding.textSheetMeta.setVisibility(View.VISIBLE);
            headerBinding.textSheetMeta.setText(buildDateTimeMeta(invoice.getDate(), invoice.getTime()));
        }
        bindStatus(invoice.getStatus());
        adapter.submitList(invoice.getItems());
        binding.textTotalValue.setText(FormatUtils.formatCurrency(invoice.getTotal()));
        if (invoice.getPaymentMethodLabel() == null || invoice.getPaymentMethodLabel().isEmpty()) {
            binding.layoutPaymentInfo.setVisibility(View.GONE);
        } else {
            binding.layoutPaymentInfo.setVisibility(View.VISIBLE);
            binding.textPaymentInfo.setText("Thanh to\u00e1n b\u1eb1ng " + invoice.getPaymentMethodLabel());
        }
    }

    private void bindStatus(OrderStatus status) {
        if (status == OrderStatus.CANCELLED) {
            binding.layoutStatusChip.setBackgroundResource(R.drawable.bg_status_cancelled);
            binding.imageStatus.setImageResource(R.drawable.ic_stat_cancelled);
            binding.imageStatus.setColorFilter(resolveColor(R.color.danger));
            binding.textStatus.setTextColor(resolveColor(R.color.danger));
            binding.textStatus.setText("\u0110\u00e3 h\u1ee7y");
            binding.layoutPaymentInfo.setBackgroundResource(R.drawable.bg_status_cancelled);
            binding.imagePayment.setImageResource(R.drawable.ic_stat_cancelled);
            binding.imagePayment.setColorFilter(resolveColor(R.color.danger));
            binding.textPaymentInfo.setTextColor(resolveColor(R.color.danger));
            return;
        }
        binding.layoutStatusChip.setBackgroundResource(R.drawable.bg_status_paid);
        binding.imageStatus.setImageResource(R.drawable.ic_stat_paid);
        binding.imageStatus.setColorFilter(resolveColor(R.color.success));
        binding.textStatus.setTextColor(resolveColor(R.color.success));
        binding.textStatus.setText("\u0110\u00e3 thanh to\u00e1n");
        binding.layoutPaymentInfo.setBackgroundResource(R.drawable.bg_success_pill);
        binding.imagePayment.setImageResource(R.drawable.ic_stat_paid);
        binding.imagePayment.setColorFilter(resolveColor(R.color.success));
        binding.textPaymentInfo.setTextColor(resolveColor(R.color.success));
    }

    private String formatDisplayDate(String isoDate) {
        if (isoDate == null) {
            return "";
        }
        String[] parts = isoDate.split("-");
        if (parts.length != 3) {
            return isoDate;
        }
        return parts[2] + "/" + parts[1] + "/" + parts[0];
    }

    private String buildDateTimeMeta(String date, String time) {
        String displayDate = formatDisplayDate(date).trim();
        String displayTime = time == null ? "" : time.trim();
        if (displayDate.isEmpty()) {
            return displayTime;
        }
        if (displayTime.isEmpty()) {
            return displayDate;
        }
        return displayDate + " " + displayTime;
    }

    private int resolveColor(int colorResId) {
        return ContextCompat.getColor(requireContext(), colorResId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        headerBinding = null;
        binding = null;
    }
}
