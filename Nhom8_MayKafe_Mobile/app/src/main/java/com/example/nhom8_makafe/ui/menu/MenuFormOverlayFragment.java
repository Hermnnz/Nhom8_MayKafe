package com.example.nhom8_makafe.ui.menu;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.data.api.ApiCallback;
import com.example.nhom8_makafe.data.api.ApiRepository;
import com.example.nhom8_makafe.databinding.BottomSheetMenuFormBinding;
import com.example.nhom8_makafe.databinding.IncludeBottomSheetHeaderBinding;
import com.example.nhom8_makafe.model.Product;
import com.example.nhom8_makafe.ui.overlay.OverlayFragment;
import com.example.nhom8_makafe.util.ImageLoader;

import java.util.ArrayList;
import java.util.Locale;

public class MenuFormOverlayFragment extends OverlayFragment {
    public static final String TAG = "menu_form_overlay";

    private static final String ARG_PRODUCT_ID = "product_id";
    private static final String ARG_NAME = "name";
    private static final String ARG_PRICE = "price";
    private static final String ARG_CATEGORY = "category";
    private static final String ARG_IMAGE_URL = "image_url";
    private static final String ARG_AVAILABLE = "available";
    private static final String ARG_ACCENT_COLOR = "accent_color";
    private static final String ARG_CATEGORIES = "categories";

    private final ApiRepository apiRepository = ApiRepository.getInstance();
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handlePickedImage);
    @Nullable
    private BottomSheetMenuFormBinding binding;
    @Nullable
    private IncludeBottomSheetHeaderBinding headerBinding;
    @Nullable
    private Uri selectedImageUri;
    @Nullable
    private String selectedImageName;

    public static MenuFormOverlayFragment newCreateInstance(@NonNull ArrayList<String> categories) {
        MenuFormOverlayFragment fragment = new MenuFormOverlayFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_CATEGORIES, categories);
        fragment.setArguments(args);
        return fragment;
    }

    public static MenuFormOverlayFragment newEditInstance(@NonNull Product product, @NonNull ArrayList<String> categories) {
        MenuFormOverlayFragment fragment = new MenuFormOverlayFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PRODUCT_ID, product.getId());
        args.putString(ARG_NAME, product.getName());
        args.putInt(ARG_PRICE, product.getPrice());
        args.putString(ARG_CATEGORY, product.getCategory());
        args.putString(ARG_IMAGE_URL, product.getImageUrl());
        args.putBoolean(ARG_AVAILABLE, product.isAvailable());
        args.putString(ARG_ACCENT_COLOR, product.getAccentColorHex());
        args.putStringArrayList(ARG_CATEGORIES, categories);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetMenuFormBinding.inflate(inflater, container, false);
        headerBinding = IncludeBottomSheetHeaderBinding.bind(binding.getRoot());
        return createBottomSheetOverlay(binding.getRoot(), true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding == null) {
            return;
        }
        ArrayList<String> categories = new ArrayList<>();
        if (getArguments() != null) {
            ArrayList<String> values = getArguments().getStringArrayList(ARG_CATEGORIES);
            if (values != null) {
                categories.addAll(values);
            }
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_menu_dropdown_option,
                categories
        );
        arrayAdapter.setDropDownViewResource(R.layout.item_menu_dropdown_option);
        binding.editCategory.setAdapter(arrayAdapter);

        boolean isEditing = isEditing();
        if (headerBinding != null) {
            headerBinding.textSheetMeta.setVisibility(View.GONE);
            headerBinding.textSheetTitle.setText(isEditing ? "S\u1eeda m\u00f3n" : "Th\u00eam m\u00f3n m\u1edbi");
        }
        binding.buttonSubmitMenu.setText(isEditing ? "L\u01b0u thay \u0111\u1ed5i" : "Th\u00eam m\u00f3n");

        if (isEditing) {
            binding.editName.setText(argString(ARG_NAME));
            binding.editPrice.setText(String.valueOf(argInt(ARG_PRICE)));
            binding.editCategory.setText(argString(ARG_CATEGORY), false);
            binding.editImageUrl.setText(argString(ARG_IMAGE_URL));
        } else if (!categories.isEmpty()) {
            binding.editCategory.setText(categories.get(0), false);
        }

        loadPreview();
        renderSelectedImageState();

        if (headerBinding != null) {
            headerBinding.buttonCloseSheet.setOnClickListener(v -> dismissAllowingStateLoss());
        }
        binding.buttonPreviewImage.setOnClickListener(v -> loadPreview());
        binding.inputImageUrl.setEndIconOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.editCategory.setOnItemClickListener((parent, itemView, position, id) -> {
            if (valueOf(binding.editImageUrl.getText()).isEmpty()) {
                loadPreview();
            }
        });
        binding.editImageUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (selectedImageUri == null) {
                    return;
                }
                clearSelectedImage();
            }
        });
        binding.buttonSubmitMenu.setOnClickListener(v -> submitMenuForm());
    }

    private void submitMenuForm() {
        if (binding == null) {
            return;
        }
        String name = valueOf(binding.editName.getText());
        String priceValue = valueOf(binding.editPrice.getText());
        String category = valueOf(binding.editCategory.getText());
        String imageUrl = valueOf(binding.editImageUrl.getText());

        binding.inputName.setError(null);
        binding.inputPrice.setError(null);
        binding.inputCategory.setError(null);
        binding.inputImageUrl.setError(null);

        boolean hasError = false;
        if (name.isEmpty()) {
            binding.inputName.setError("Vui l\u00f2ng nh\u1eadp t\u00ean m\u00f3n");
            hasError = true;
        }
        if (priceValue.isEmpty()) {
            binding.inputPrice.setError("Vui l\u00f2ng nh\u1eadp gi\u00e1");
            hasError = true;
        }
        if (category.isEmpty()) {
            binding.inputCategory.setError("Vui l\u00f2ng ch\u1ecdn danh m\u1ee5c");
            hasError = true;
        }
        if (hasError) {
            return;
        }

        int price;
        try {
            price = Integer.parseInt(priceValue);
        } catch (NumberFormatException exception) {
            binding.inputPrice.setError("Gi\u00e1 ph\u1ea3i l\u00e0 s\u1ed1 h\u1ee3p l\u1ec7");
            return;
        }

        long productId = argLong(ARG_PRODUCT_ID);
        boolean available = getArguments() == null || getArguments().getBoolean(ARG_AVAILABLE, true);
        String accentColor = argString(ARG_ACCENT_COLOR);
        if (accentColor.isEmpty()) {
            accentColor = colorForCategory(category);
        }

        Product target = new Product(
                productId,
                name,
                price,
                category,
                available,
                buildAssetLabel(name),
                accentColor,
                resolveImageUrl(imageUrl, category)
        );

        submitProduct(target);
    }

    private void submitProduct(@NonNull Product target) {
        ApiCallback<Product> callback = new ApiCallback<Product>() {
            @Override
            public void onSuccess(Product data) {
                if (!isAdded()) {
                    return;
                }
                notifyMenuChanged();
                dismissAllowingStateLoss();
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || binding == null) {
                    return;
                }
                binding.inputName.setError(message);
            }
        };

        if (selectedImageUri != null) {
            apiRepository.uploadProductImage(requireContext(), selectedImageUri, new ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    target.setImageUrl(data);
                    saveProduct(target, callback);
                }

                @Override
                public void onError(String message) {
                    if (!isAdded() || binding == null) {
                        return;
                    }
                    binding.inputImageUrl.setError(message);
                }
            });
            return;
        }

        saveProduct(target, callback);
    }

    private void saveProduct(@NonNull Product target, @NonNull ApiCallback<Product> callback) {
        if (isEditing()) {
            apiRepository.updateProduct(target, callback);
        } else {
            apiRepository.createProduct(target, callback);
        }
    }

    private void notifyMenuChanged() {
        Bundle result = new Bundle();
        result.putBoolean(MenuOverlayContract.RESULT_REFRESH, true);
        getParentFragmentManager().setFragmentResult(MenuOverlayContract.REQUEST_KEY, result);
    }

    private boolean isEditing() {
        return argLong(ARG_PRODUCT_ID) > 0L;
    }

    private void loadPreview() {
        if (binding == null) {
            return;
        }
        if (selectedImageUri != null) {
            ImageLoader.load(
                    binding.imagePreview,
                    selectedImageUri,
                    com.example.nhom8_makafe.R.drawable.bg_menu_preview_placeholder
            );
            return;
        }
        String category = valueOf(binding.editCategory.getText());
        String imageUrl = valueOf(binding.editImageUrl.getText());
        if (imageUrl.isEmpty()) {
            imageUrl = argString(ARG_IMAGE_URL);
        }
        ImageLoader.load(
                binding.imagePreview,
                resolveImageUrl(imageUrl, category),
                com.example.nhom8_makafe.R.drawable.bg_menu_preview_placeholder
        );
    }

    private void handlePickedImage(@Nullable Uri imageUri) {
        if (imageUri == null || binding == null) {
            return;
        }
        selectedImageUri = imageUri;
        selectedImageName = resolveDisplayName(imageUri);
        binding.inputImageUrl.setError(null);
        renderSelectedImageState();
        loadPreview();
    }

    private void clearSelectedImage() {
        selectedImageUri = null;
        selectedImageName = null;
        renderSelectedImageState();
        loadPreview();
    }

    private void renderSelectedImageState() {
        if (binding == null) {
            return;
        }
        boolean hasSelectedImage = selectedImageUri != null;
        binding.textSelectedImageFile.setVisibility(hasSelectedImage ? View.VISIBLE : View.GONE);
        if (hasSelectedImage) {
            binding.textSelectedImageFile.setText("\u0110\u00e3 ch\u1ecdn t\u1ec7p \u1ea3nh: " + selectedImageName);
        } else {
            binding.textSelectedImageFile.setText("");
        }
    }

    private String resolveDisplayName(@NonNull Uri imageUri) {
        try (Cursor cursor = requireContext().getContentResolver().query(imageUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String value = cursor.getString(index);
                    if (value != null && !value.trim().isEmpty()) {
                        return value.trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "anh-mon-moi.jpg";
    }

    private String resolveImageUrl(String imageUrl, String category) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return imageUrl.trim();
        }
        return "";
    }

    private String buildAssetLabel(String name) {
        String[] parts = name.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            }
            if (builder.length() == 2) {
                break;
            }
        }
        if (builder.length() == 0) {
            return "MK";
        }
        if (builder.length() == 1 && name.length() > 1) {
            builder.append(name.substring(1, 2).toUpperCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private String colorForCategory(String category) {
        if ("Tr\u00e0".equals(category)) {
            return "#C8956C";
        }
        if ("Sinh t\u1ed1".equals(category)) {
            return "#F5A623";
        }
        if ("N\u01b0\u1edbc \u00e9p".equals(category)) {
            return "#F08B3A";
        }
        if ("B\u00e1nh".equals(category)) {
            return "#CA9C63";
        }
        return "#6B3F2A";
    }

    private String valueOf(@Nullable Editable editable) {
        return editable == null ? "" : editable.toString().trim();
    }

    private String argString(String key) {
        return getArguments() == null ? "" : getArguments().getString(key, "");
    }

    private int argInt(String key) {
        return getArguments() == null ? 0 : getArguments().getInt(key, 0);
    }

    private long argLong(String key) {
        return getArguments() == null ? 0L : getArguments().getLong(key, 0L);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        selectedImageUri = null;
        selectedImageName = null;
        headerBinding = null;
        binding = null;
    }
}
