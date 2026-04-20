package com.example.nhom8_makafe.ui.menu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nhom8_makafe.data.api.ApiCallback;
import com.example.nhom8_makafe.data.api.ApiRepository;
import com.example.nhom8_makafe.databinding.DialogMenuDeleteConfirmBinding;
import com.example.nhom8_makafe.util.ImageLoader;
import com.example.nhom8_makafe.ui.overlay.OverlayFragment;

public class MenuDeleteConfirmOverlayFragment extends OverlayFragment {
    public static final String TAG = "menu_delete_overlay";
    private static final String ARG_PRODUCT_ID = "product_id";
    private static final String ARG_NAME = "name";
    private static final String ARG_CATEGORY = "category";
    private static final String ARG_IMAGE_URL = "image_url";

    private final ApiRepository apiRepository = ApiRepository.getInstance();
    @Nullable
    private DialogMenuDeleteConfirmBinding binding;

    public static MenuDeleteConfirmOverlayFragment newInstance(long productId,
                                                               @NonNull String name,
                                                               @NonNull String category,
                                                               @Nullable String imageUrl) {
        MenuDeleteConfirmOverlayFragment fragment = new MenuDeleteConfirmOverlayFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PRODUCT_ID, productId);
        args.putString(ARG_NAME, name);
        args.putString(ARG_CATEGORY, category);
        args.putString(ARG_IMAGE_URL, imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogMenuDeleteConfirmBinding.inflate(inflater, container, false);
        return createCenteredDialogOverlay(binding.getRoot(), 0.88f, 420, true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding == null) {
            return;
        }
        Bundle args = getArguments();
        long productId = args == null ? 0L : args.getLong(ARG_PRODUCT_ID, 0L);
        String productName = args == null ? "" : args.getString(ARG_NAME, "");
        String category = args == null ? "" : args.getString(ARG_CATEGORY, "");
        String imageUrl = args == null ? "" : args.getString(ARG_IMAGE_URL, "");

        ImageLoader.load(binding.imageProductPreview, resolveImageUrl(imageUrl, category));
        binding.textDeleteTitle.setText("X\u00f3a \"" + productName + "\"?");
        binding.textDeleteMessage.setText("H\u00e0nh \u0111\u1ed9ng n\u00e0y kh\u00f4ng th\u1ec3 ho\u00e0n t\u00e1c.");
        binding.buttonCancelDelete.setOnClickListener(v -> dismissAllowingStateLoss());
        binding.buttonConfirmDelete.setOnClickListener(v ->
                apiRepository.deleteProduct(productId, new ApiCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        if (!isAdded()) {
                            return;
                        }
                        notifyMenuChanged();
                        dismissAllowingStateLoss();
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(
                                requireContext(),
                                message == null || message.trim().isEmpty()
                                        ? "Kh\u00f4ng th\u1ec3 x\u00f3a m\u00f3n n\u00e0y."
                                        : message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }));
    }

    private void notifyMenuChanged() {
        Bundle result = new Bundle();
        result.putBoolean(MenuOverlayContract.RESULT_REFRESH, true);
        getParentFragmentManager().setFragmentResult(MenuOverlayContract.REQUEST_KEY, result);
    }

    private String resolveImageUrl(String imageUrl, String category) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return imageUrl.trim();
        }
        return "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
