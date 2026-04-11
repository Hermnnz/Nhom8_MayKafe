package com.example.nhom8_makafe.ui.cart;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.data.SessionManager;
import com.example.nhom8_makafe.data.api.ApiCallback;
import com.example.nhom8_makafe.data.api.ApiRepository;
import com.example.nhom8_makafe.databinding.BottomSheetCheckoutBinding;
import com.example.nhom8_makafe.model.CartItem;
import com.example.nhom8_makafe.model.Invoice;
import com.example.nhom8_makafe.model.PaymentMethod;
import com.example.nhom8_makafe.model.PaymentSession;
import com.example.nhom8_makafe.model.User;
import com.example.nhom8_makafe.util.FormatUtils;
import com.example.nhom8_makafe.util.QrBitmapUtils;
import com.example.nhom8_makafe.ui.overlay.OverlayFragment;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class CheckoutBottomSheetDialogFragment extends OverlayFragment implements SessionManager.Observer {
    private static final String ARG_DISCOUNT_PERCENT = "discount_percent";
    private static final int CASH_STEP = 5_000;
    private static final String TABLE_LABEL = "B\u00e0n m\u1edbi";

    private BottomSheetCheckoutBinding binding;
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final ApiRepository apiRepository = ApiRepository.getInstance();
    private int discountPercent = 0;
    private int lastAvailableHeight = 0;
    private boolean updatingCashInput = false;
    private boolean qrRequestInFlight = false;
    private boolean confirmInFlight = false;
    @Nullable
    private View overlayRoot;
    @Nullable
    private PaymentMethod selectedPaymentMethod;
    @Nullable
    private PaymentSession qrPaymentSession;

    public static CheckoutBottomSheetDialogFragment newInstance(int discountPercent) {
        CheckoutBottomSheetDialogFragment fragment = new CheckoutBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DISCOUNT_PERCENT, discountPercent);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCheckoutBinding.inflate(inflater, container, false);
        overlayRoot = createBottomSheetOverlay(binding.getRoot(), true);
        return overlayRoot;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        discountPercent = getArguments() == null ? 0 : getArguments().getInt(ARG_DISCOUNT_PERCENT, 0);
        if (discountPercent < 0) {
            discountPercent = 0;
        }
        if (discountPercent > 100) {
            discountPercent = 100;
        }

        binding.buttonBack.setOnClickListener(v -> dismissAllowingStateLoss());
        binding.buttonClose.setOnClickListener(v -> dismissAllowingStateLoss());
        binding.cardPaymentCash.setOnClickListener(v -> selectPaymentMethod(PaymentMethod.CASH));
        binding.cardPaymentQr.setOnClickListener(v -> selectPaymentMethod(PaymentMethod.QR));
        binding.buttonCashUp.setOnClickListener(v -> updateCashReceivedBy(CASH_STEP));
        binding.buttonCashDown.setOnClickListener(v -> updateCashReceivedBy(-CASH_STEP));
        binding.editCashReceived.addTextChangedListener(simpleWatcher(() -> {
            if (!updatingCashInput) {
                renderState();
            }
        }));
        binding.buttonConfirmPayment.setOnClickListener(v -> confirmPayment());

        sessionManager.addObserver(this);
        renderState();
        if (overlayRoot != null) {
            overlayRoot.post(this::refreshOverlayBounds);
        }
    }

    private void refreshOverlayBounds() {
        if (binding == null || overlayRoot == null) {
            return;
        }
        int overlayHeight = overlayRoot.getHeight();
        if (overlayHeight <= 0) {
            return;
        }
        int availableHeight = Math.max(dp(420), overlayHeight - dp(14));
        lastAvailableHeight = availableHeight;
        applyResponsiveLayout(availableHeight, false);
        overlayRoot.post(() -> {
            if (binding == null) {
                return;
            }
            if (binding.getRoot().getMeasuredHeight() > availableHeight) {
                applyResponsiveLayout(availableHeight, true);
                binding.getRoot().requestLayout();
            }
        });
    }

    private void applyResponsiveLayout(int availableHeight, boolean forceCompact) {
        if (binding == null) {
            return;
        }
        boolean compact = forceCompact || availableHeight <= dp(620);
        boolean ultraCompact = forceCompact || availableHeight <= dp(560);

        int headerHorizontal = compact ? dp(14) : dp(16);
        int headerVertical = ultraCompact ? dp(10) : compact ? dp(12) : dp(14);
        updatePadding(binding.layoutCheckoutHeader, headerHorizontal, headerVertical, headerHorizontal, headerVertical);
        updateSquareSize(binding.buttonBack, ultraCompact ? dp(30) : compact ? dp(32) : dp(34));
        updateSquareSize(binding.buttonClose, ultraCompact ? dp(30) : compact ? dp(32) : dp(34));
        binding.textTitle.setTextSize(ultraCompact ? 17f : 18f);

        int contentHorizontal = compact ? dp(14) : dp(16);
        int contentTop = ultraCompact ? dp(10) : compact ? dp(12) : dp(14);
        int contentBottom = ultraCompact ? dp(10) : compact ? dp(10) : dp(12);
        updatePadding(binding.layoutCheckoutContent, contentHorizontal, contentTop, contentHorizontal, contentBottom);

        int summaryPadding = ultraCompact ? dp(12) : compact ? dp(13) : dp(14);
        updatePadding(binding.cardTotalSummary, summaryPadding, summaryPadding, summaryPadding, summaryPadding);
        binding.textTotalLabel.setTextSize(ultraCompact ? 12f : 13f);
        binding.textTotalAmount.setTextSize(ultraCompact ? 22f : compact ? 23f : 24f);
        binding.textItemCount.setTextSize(ultraCompact ? 12f : 13f);

        setTopMargin(binding.textPaymentSectionTitle, ultraCompact ? dp(10) : compact ? dp(12) : dp(14));
        binding.textPaymentSectionTitle.setTextSize(ultraCompact ? 14f : 15f);
        setTopMargin(binding.layoutPaymentMethods, ultraCompact ? dp(8) : dp(10));

        int paymentCardHeight = ultraCompact ? dp(88) : compact ? dp(96) : dp(108);
        updateHeight(binding.cardPaymentCash, paymentCardHeight);
        updateHeight(binding.cardPaymentQr, paymentCardHeight);
        updateSquareSize(binding.layoutCashIconBg, ultraCompact ? dp(40) : compact ? dp(44) : dp(50));
        updateSquareSize(binding.layoutQrIconBg, ultraCompact ? dp(40) : compact ? dp(44) : dp(50));
        updateSquareSize(binding.imageCashIcon, ultraCompact ? dp(20) : dp(22));
        updateSquareSize(binding.imageQrIcon, ultraCompact ? dp(20) : dp(22));
        binding.textCashLabel.setTextSize(ultraCompact ? 13f : 14f);
        binding.textQrLabel.setTextSize(ultraCompact ? 13f : 14f);

        int sectionMarginTop = ultraCompact ? dp(8) : compact ? dp(10) : dp(12);
        setTopMargin(binding.layoutCashSection, sectionMarginTop);
        setTopMargin(binding.layoutQrSection, sectionMarginTop);
        int sectionPadding = ultraCompact ? dp(10) : compact ? dp(12) : dp(14);
        updatePadding(binding.layoutCashSection, sectionPadding, sectionPadding, sectionPadding, sectionPadding);
        updatePadding(binding.layoutQrSection, sectionPadding, sectionPadding, sectionPadding, sectionPadding);
        binding.textCashSectionTitle.setTextSize(ultraCompact ? 14f : 15f);
        binding.textQrSectionTitle.setTextSize(ultraCompact ? 14f : 15f);
        setTopMargin(binding.textCashLabelTitle, ultraCompact ? dp(10) : dp(12));
        binding.textCashLabelTitle.setTextSize(ultraCompact ? 12f : 13f);
        updateHeight(binding.layoutCashInputBox, ultraCompact ? dp(44) : compact ? dp(46) : dp(50));
        binding.editCashReceived.setTextSize(ultraCompact ? 14f : 15f);
        setTopMargin(binding.layoutCashResult, ultraCompact ? dp(8) : dp(10));
        updatePadding(binding.layoutCashResult,
                ultraCompact ? dp(10) : dp(12),
                ultraCompact ? dp(10) : dp(12),
                ultraCompact ? dp(10) : dp(12),
                ultraCompact ? dp(10) : dp(12));
        binding.textCashResultLabel.setTextSize(ultraCompact ? 12f : 13f);
        binding.textCashResultValue.setTextSize(ultraCompact ? 14f : 15f);

        updateSquareSize(binding.imageQrCode, ultraCompact ? dp(96) : compact ? dp(108) : dp(124));
        binding.textBankName.setTextSize(ultraCompact ? 12f : 13f);
        binding.textAccountNumber.setTextSize(ultraCompact ? 12f : 13f);
        binding.textAccountName.setTextSize(ultraCompact ? 12f : 13f);
        binding.textQrAmount.setTextSize(ultraCompact ? 14f : 15f);
        binding.textTransferContent.setTextSize(ultraCompact ? 12f : 13f);

        int buttonBarHorizontal = compact ? dp(14) : dp(16);
        int buttonBarVertical = ultraCompact ? dp(10) : compact ? dp(12) : dp(14);
        updatePadding(binding.layoutButtonBar, buttonBarHorizontal, buttonBarVertical, buttonBarHorizontal, buttonBarVertical);
        updateHeight(binding.buttonConfirmPayment, ultraCompact ? dp(46) : compact ? dp(48) : dp(52));
        binding.buttonConfirmPayment.setTextSize(ultraCompact ? 14f : 15f);
    }

    private void selectPaymentMethod(@NonNull PaymentMethod paymentMethod) {
        selectedPaymentMethod = paymentMethod;
        if (paymentMethod == PaymentMethod.CASH) {
            int currentValue = parseInt(binding.editCashReceived.getText() == null ? "" : binding.editCashReceived.getText().toString());
            if (currentValue <= 0) {
                setCashReceivedText(getResolvedTotal());
                return;
            }
        } else {
            ensureQrPaymentSession(false);
        }
        renderState();
    }

    private void ensureQrPaymentSession(boolean fromConfirmAction) {
        if (binding == null || qrRequestInFlight || confirmInFlight || !isAdded()) {
            return;
        }
        int expectedTotal = getLocalFinalTotal();
        if (qrPaymentSession != null
                && qrPaymentSession.getPaymentId() > 0
                && qrPaymentSession.getAmount() == expectedTotal) {
            renderState();
            return;
        }

        qrRequestInFlight = true;
        renderState();
        ApiCallback<PaymentSession> callback = new ApiCallback<PaymentSession>() {
            @Override
            public void onSuccess(PaymentSession data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                qrPaymentSession = data;
                qrRequestInFlight = false;
                renderState();
                if (fromConfirmAction && data != null && data.getPaymentId() > 0) {
                    Toast.makeText(requireContext(), "M\u00e3 QR \u0111\u00e3 s\u1eb5n s\u00e0ng.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || binding == null) {
                    return;
                }
                qrRequestInFlight = false;
                renderState();
                Toast.makeText(
                        requireContext(),
                        message == null || message.trim().isEmpty()
                                ? "Kh\u00f4ng th\u1ec3 t\u1ea1o m\u00e3 QR thanh to\u00e1n."
                                : message,
                        Toast.LENGTH_SHORT
                ).show();
            }
        };
        if (qrPaymentSession != null && qrPaymentSession.getOrderId() > 0) {
            apiRepository.refreshQrPayment(qrPaymentSession.getOrderId(), callback);
        } else {
            apiRepository.initializeQrPayment(TABLE_LABEL, discountPercent, sessionManager.getCartItems(), callback);
        }
    }

    private void renderState() {
        if (binding == null) {
            return;
        }
        if (sessionManager.getCartItems().isEmpty()) {
            dismissAllowingStateLoss();
            return;
        }

        int resolvedTotal = getResolvedTotal();
        binding.textTotalAmount.setText(FormatUtils.formatCurrency(resolvedTotal));
        binding.textItemCount.setText(sessionManager.getCartCount() + " m\u00f3n");

        boolean cashSelected = selectedPaymentMethod == PaymentMethod.CASH;
        boolean qrSelected = selectedPaymentMethod == PaymentMethod.QR;
        stylePaymentCard(binding.cardPaymentCash, binding.layoutCashIconBg, binding.imageCashIcon, binding.textCashLabel, cashSelected);
        stylePaymentCard(binding.cardPaymentQr, binding.layoutQrIconBg, binding.imageQrIcon, binding.textQrLabel, qrSelected);
        binding.layoutCashSection.setVisibility(cashSelected ? View.VISIBLE : View.GONE);
        binding.layoutQrSection.setVisibility(qrSelected ? View.VISIBLE : View.GONE);

        if (cashSelected) {
            renderCashSection(resolvedTotal);
        } else {
            binding.layoutCashResult.setVisibility(View.GONE);
        }

        if (qrSelected) {
            renderQrSection();
        }

        updateConfirmButton(resolvedTotal);
        scheduleLayoutAdjustment();
    }

    private void renderQrSection() {
        if (binding == null) {
            return;
        }
        binding.textQrSectionTitle.setText("Th\u00f4ng tin chuy\u1ec3n kho\u1ea3n");
        if (qrPaymentSession == null) {
            binding.textBankName.setText("--");
            binding.textAccountNumber.setText("--");
            binding.textAccountName.setText("--");
            binding.textQrAmount.setText(FormatUtils.formatCurrency(getLocalFinalTotal()));
            binding.textTransferContent.setText("--");
            binding.imageQrCode.setPadding(dp(18), dp(18), dp(18), dp(18));
            binding.imageQrCode.setImageResource(R.drawable.ic_payment_qr);
            return;
        }
        binding.textBankName.setText(qrPaymentSession.getBankName());
        binding.textAccountNumber.setText(qrPaymentSession.getAccountNumber());
        binding.textAccountName.setText(qrPaymentSession.getAccountName());
        binding.textQrAmount.setText(FormatUtils.formatCurrency(qrPaymentSession.getAmount()));
        binding.textTransferContent.setText(qrPaymentSession.getTransferContent());

        int qrSize = binding.imageQrCode.getLayoutParams() == null
                ? dp(140)
                : Math.max(binding.imageQrCode.getLayoutParams().width, dp(96));
        Bitmap bitmap = QrBitmapUtils.createQrBitmap(qrPaymentSession.getQrContent(), qrSize);
        if (bitmap != null) {
            binding.imageQrCode.setPadding(0, 0, 0, 0);
            binding.imageQrCode.setImageBitmap(bitmap);
        } else {
            binding.imageQrCode.setPadding(dp(18), dp(18), dp(18), dp(18));
            binding.imageQrCode.setImageResource(R.drawable.ic_payment_qr);
        }
    }

    private void renderCashSection(int finalTotal) {
        int cashReceived = parseInt(binding.editCashReceived.getText() == null ? "" : binding.editCashReceived.getText().toString());
        int delta = cashReceived - finalTotal;
        if (cashReceived <= 0 || delta == 0) {
            binding.layoutCashResult.setVisibility(View.GONE);
            return;
        }
        binding.layoutCashResult.setVisibility(View.VISIBLE);
        if (delta > 0) {
            binding.layoutCashResult.setBackgroundResource(R.drawable.bg_payment_status_success);
            binding.textCashResultLabel.setText("Ti\u1ec1n th\u1eeba tr\u1ea3 l\u1ea1i:");
            binding.textCashResultLabel.setTextColor(requireContext().getColor(R.color.success));
            binding.textCashResultValue.setTextColor(requireContext().getColor(R.color.success));
            binding.textCashResultValue.setText(FormatUtils.formatCurrency(delta));
        } else {
            binding.layoutCashResult.setBackgroundResource(R.drawable.bg_payment_status_danger);
            binding.textCashResultLabel.setText("C\u00f2n thi\u1ebfu:");
            binding.textCashResultLabel.setTextColor(requireContext().getColor(R.color.danger));
            binding.textCashResultValue.setTextColor(requireContext().getColor(R.color.danger));
            binding.textCashResultValue.setText(FormatUtils.formatCurrency(Math.abs(delta)));
        }
    }

    private void updateConfirmButton(int finalTotal) {
        boolean enabled = false;
        String text;
        if (confirmInFlight) {
            text = "\u0110ang x\u1eed l\u00fd...";
        } else if (selectedPaymentMethod == PaymentMethod.QR && qrRequestInFlight) {
            text = "\u0110ang t\u1ea1o QR...";
        } else if (selectedPaymentMethod == PaymentMethod.QR) {
            text = "\u0110\u00e3 chuy\u1ec3n kho\u1ea3n \u2713";
            enabled = qrPaymentSession != null && qrPaymentSession.getPaymentId() > 0 && !sessionManager.getCartItems().isEmpty();
        } else {
            text = "X\u00e1c nh\u1eadn thanh to\u00e1n";
            if (selectedPaymentMethod == PaymentMethod.CASH) {
                int cashReceived = parseInt(binding.editCashReceived.getText() == null ? "" : binding.editCashReceived.getText().toString());
                enabled = cashReceived >= finalTotal && !sessionManager.getCartItems().isEmpty();
            }
        }

        if (confirmInFlight || qrRequestInFlight) {
            enabled = false;
        }
        binding.buttonConfirmPayment.setText(text);
        binding.buttonConfirmPayment.setEnabled(enabled);
        binding.buttonConfirmPayment.setAlpha(enabled ? 1f : 0.78f);
        binding.buttonConfirmPayment.setBackgroundResource(enabled
                ? R.drawable.bg_primary_button
                : R.drawable.bg_payment_button_disabled);
    }

    private void scheduleLayoutAdjustment() {
        if (binding == null || overlayRoot == null || lastAvailableHeight <= 0) {
            return;
        }
        overlayRoot.post(() -> {
            if (binding == null) {
                return;
            }
            refreshOverlayBounds();
            boolean overflow = binding.getRoot().getMeasuredHeight() > lastAvailableHeight;
            applyResponsiveLayout(lastAvailableHeight, overflow);
            binding.getRoot().requestLayout();
        });
    }

    private void confirmPayment() {
        if (binding == null || selectedPaymentMethod == null || confirmInFlight) {
            return;
        }
        int finalTotal = getResolvedTotal();
        if (selectedPaymentMethod == PaymentMethod.CASH) {
            int cashReceived = parseInt(binding.editCashReceived.getText() == null ? "" : binding.editCashReceived.getText().toString());
            if (cashReceived < finalTotal) {
                Toast.makeText(requireContext(), "Ti\u1ec1n kh\u00e1ch \u0111\u01b0a ch\u01b0a \u0111\u1ee7.", Toast.LENGTH_SHORT).show();
                return;
            }
            submitCashPayment(cashReceived);
            return;
        }

        if (selectedPaymentMethod == PaymentMethod.QR) {
            if (qrPaymentSession == null || qrPaymentSession.getPaymentId() <= 0) {
                ensureQrPaymentSession(true);
                return;
            }
            submitQrPaymentConfirmation();
        }
    }

    private void submitCashPayment(int cashReceived) {
        confirmInFlight = true;
        renderState();
        ApiCallback<Invoice> callback = new ApiCallback<Invoice>() {
            @Override
            public void onSuccess(Invoice data) {
                handlePaymentSuccess();
            }

            @Override
            public void onError(String message) {
                handlePaymentError(message, "Kh\u00f4ng th\u1ec3 x\u1eed l\u00fd thanh to\u00e1n ti\u1ec1n m\u1eb7t.");
            }
        };

        if (qrPaymentSession != null && qrPaymentSession.getOrderId() > 0) {
            apiRepository.confirmCashPaymentForOrder(qrPaymentSession.getOrderId(), cashReceived, callback);
        } else {
            apiRepository.confirmCashPayment(TABLE_LABEL, discountPercent, cashReceived, sessionManager.getCartItems(), callback);
        }
    }

    private void submitQrPaymentConfirmation() {
        confirmInFlight = true;
        renderState();
        apiRepository.confirmBankTransfer(qrPaymentSession.getPaymentId(), new ApiCallback<Invoice>() {
            @Override
            public void onSuccess(Invoice data) {
                handlePaymentSuccess();
            }

            @Override
            public void onError(String message) {
                handlePaymentError(message, "Kh\u00f4ng th\u1ec3 x\u00e1c nh\u1eadn chuy\u1ec3n kho\u1ea3n.");
            }
        });
    }

    private void handlePaymentSuccess() {
        if (!isAdded()) {
            return;
        }
        androidx.fragment.app.FragmentManager fragmentManager = getParentFragmentManager();
        View rootView = requireActivity().findViewById(android.R.id.content);
        sessionManager.clearCart();
        dismissAllowingStateLoss();
        rootView.post(() -> {
            if (!fragmentManager.isStateSaved()) {
                androidx.fragment.app.Fragment existing = fragmentManager.findFragmentByTag(PaymentSuccessDialogFragment.TAG);
                if (existing instanceof OverlayFragment) {
                    ((OverlayFragment) existing).dismissAllowingStateLoss();
                }
                PaymentSuccessDialogFragment.newInstance().show(fragmentManager, PaymentSuccessDialogFragment.TAG);
            }
        });
    }

    private void handlePaymentError(@Nullable String message, @NonNull String fallbackMessage) {
        if (!isAdded() || binding == null) {
            return;
        }
        confirmInFlight = false;
        renderState();
        Toast.makeText(
                requireContext(),
                message == null || message.trim().isEmpty() ? fallbackMessage : message,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void stylePaymentCard(@NonNull MaterialCardView card,
                                  @NonNull FrameLayout iconBackground,
                                  @NonNull ImageView icon,
                                  @NonNull TextView label,
                                  boolean selected) {
        card.setCardBackgroundColor(requireContext().getColor(selected ? R.color.coffee_100 : R.color.white));
        card.setStrokeColor(requireContext().getColor(selected ? R.color.coffee_700 : R.color.coffee_200));
        card.setStrokeWidth(dp(selected ? 2 : 1));
        iconBackground.setBackgroundResource(selected ? R.drawable.bg_payment_icon_selected : R.drawable.bg_payment_icon_idle);
        icon.setColorFilter(requireContext().getColor(selected ? R.color.white : R.color.coffee_700));
        label.setTextColor(requireContext().getColor(selected ? R.color.coffee_700 : R.color.coffee_950));
    }

    private void updateCashReceivedBy(int delta) {
        if (binding == null) {
            return;
        }
        int currentValue = parseInt(binding.editCashReceived.getText() == null ? "" : binding.editCashReceived.getText().toString());
        int nextValue = Math.max(0, currentValue + delta);
        setCashReceivedText(nextValue);
    }

    private void setCashReceivedText(int value) {
        if (binding == null) {
            return;
        }
        updatingCashInput = true;
        binding.editCashReceived.setText(value <= 0 ? "" : String.valueOf(value));
        Editable editable = binding.editCashReceived.getText();
        if (editable != null) {
            binding.editCashReceived.setSelection(editable.length());
        }
        updatingCashInput = false;
        renderState();
    }

    private int getLocalFinalTotal() {
        int subtotal = sessionManager.getCartSubtotal();
        int discountAmount = Math.round(subtotal * discountPercent / 100f);
        return Math.max(0, subtotal - discountAmount);
    }

    private int getResolvedTotal() {
        if (qrPaymentSession != null && qrPaymentSession.getAmount() > 0) {
            return qrPaymentSession.getAmount();
        }
        return getLocalFinalTotal();
    }

    private int parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private TextWatcher simpleWatcher(Runnable runnable) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                runnable.run();
            }
        };
    }

    private void updateHeight(@NonNull View view, int height) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            return;
        }
        layoutParams.height = height;
        view.setLayoutParams(layoutParams);
    }

    private void updateSquareSize(@NonNull View view, int size) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            return;
        }
        layoutParams.width = size;
        layoutParams.height = size;
        view.setLayoutParams(layoutParams);
    }

    private void updatePadding(@NonNull View view, int left, int top, int right, int bottom) {
        view.setPadding(left, top, right, bottom);
    }

    private void setTopMargin(@NonNull View view, int topMargin) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        marginLayoutParams.topMargin = topMargin;
        view.setLayoutParams(marginLayoutParams);
    }

    @Override
    public void onSessionChanged(User user) {
        if (user == null) {
            dismissAllowingStateLoss();
        }
    }

    @Override
    public void onCartChanged(List<CartItem> cartItems) {
        if (binding == null) {
            return;
        }
        if (cartItems.isEmpty()) {
            dismissAllowingStateLoss();
            return;
        }
        qrPaymentSession = null;
        qrRequestInFlight = false;
        confirmInFlight = false;
        renderState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sessionManager.removeObserver(this);
        overlayRoot = null;
        binding = null;
    }
}
