package com.example.nhom8_makafe.ui.menu;

import android.text.Editable;
import android.text.TextWatcher;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nhom8_makafe.adapter.CategoryChipAdapter;
import com.example.nhom8_makafe.adapter.MenuManagementAdapter;
import com.example.nhom8_makafe.data.SessionManager;
import com.example.nhom8_makafe.data.api.ApiCallback;
import com.example.nhom8_makafe.data.api.ApiRepository;
import com.example.nhom8_makafe.databinding.DialogMenuDeleteConfirmBinding;
import com.example.nhom8_makafe.databinding.DialogMenuFormBinding;
import com.example.nhom8_makafe.databinding.FragmentMenuManagementBinding;
import com.example.nhom8_makafe.model.Product;
import com.example.nhom8_makafe.model.User;
import com.example.nhom8_makafe.util.ImageLoader;
import com.example.nhom8_makafe.util.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuManagementFragment extends Fragment {
    private static final String CATEGORY_ALL = "T\u1ea5t c\u1ea3";

    private FragmentMenuManagementBinding binding;
    private final ApiRepository apiRepository = ApiRepository.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private CategoryChipAdapter categoryAdapter;
    private MenuManagementAdapter menuAdapter;
    private String selectedCategory = CATEGORY_ALL;
    private String searchQuery = "";
    private int baseHeaderTopPadding;
    private List<String> remoteCategories = new ArrayList<>();

    public static MenuManagementFragment newInstance() {
        return new MenuManagementFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMenuManagementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        baseHeaderTopPadding = binding.layoutHeader.getPaddingTop();
        applyInsets();

        User user = sessionManager.getCurrentUser();
        if (user != null) {
            UiUtils.bindThumbnail(binding.textAvatar, binding.textAvatar, user.getAvatarInitials(), user.getAvatarColorHex());
        }

        binding.buttonAddItem.setOnClickListener(v -> showMenuDialog(null));
        binding.editSearchMenu.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchQuery = s == null ? "" : s.toString().trim();
                loadProducts();
            }
        });

        categoryAdapter = new CategoryChipAdapter(category -> {
            selectedCategory = category;
            categoryAdapter.setSelectedCategory(category);
            loadProducts();
        });
        binding.recyclerCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerCategories.setAdapter(categoryAdapter);
        categoryAdapter.submitList(buildCategoryChips());
        categoryAdapter.setSelectedCategory(selectedCategory);

        menuAdapter = new MenuManagementAdapter(new MenuManagementAdapter.MenuActionListener() {
            @Override
            public void onEdit(Product product) {
                showMenuDialog(product);
            }

            @Override
            public void onDelete(Product product) {
                showDeleteConfirmation(product);
            }

            @Override
            public void onToggle(Product product) {
                apiRepository.toggleAvailability(product, new ApiCallback<Product>() {
                    @Override
                    public void onSuccess(Product data) {
                        if (!isAdded() || binding == null) {
                            return;
                        }
                        loadProducts();
                        refreshSummary();
                    }

                    @Override
                    public void onError(String message) {
                    }
                });
            }
        });
        binding.recyclerMenuItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerMenuItems.setAdapter(menuAdapter);

        loadCategories();
        loadProducts();
        refreshSummary();
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutHeader, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.layoutHeader.setPadding(
                    binding.layoutHeader.getPaddingLeft(),
                    baseHeaderTopPadding + systemBars.top,
                    binding.layoutHeader.getPaddingRight(),
                    binding.layoutHeader.getPaddingBottom()
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(binding.layoutHeader);
    }

    private void loadCategories() {
        apiRepository.fetchCategories(new ApiCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                remoteCategories = new ArrayList<>(data);
                List<String> chips = buildCategoryChips();
                if (!chips.contains(selectedCategory)) {
                    selectedCategory = CATEGORY_ALL;
                }
                categoryAdapter.submitList(chips);
                categoryAdapter.setSelectedCategory(selectedCategory);
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || binding == null) {
                    return;
                }
                remoteCategories = new ArrayList<>();
                categoryAdapter.submitList(buildCategoryChips());
                categoryAdapter.setSelectedCategory(selectedCategory);
            }
        });
    }

    private void loadProducts() {
        if (binding == null) {
            return;
        }
        String category = CATEGORY_ALL.equals(selectedCategory) ? null : selectedCategory;
        apiRepository.fetchProducts(searchQuery, category, null, new ApiCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                menuAdapter.submitList(data);
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || binding == null) {
                    return;
                }
                menuAdapter.submitList(new ArrayList<>());
            }
        });
    }

    private void refreshSummary() {
        apiRepository.fetchProducts("", null, null, new ApiCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                int availableCount = 0;
                for (Product product : data) {
                    if (product.isAvailable()) {
                        availableCount++;
                    }
                }
                binding.textSummary.setText(data.size() + " m\u00f3n \u2022 " + availableCount + " \u0111ang b\u00e1n");
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || binding == null) {
                    return;
                }
                binding.textSummary.setText("0 m\u00f3n \u2022 0 \u0111ang b\u00e1n");
            }
        });
    }

    private List<String> buildCategoryChips() {
        List<String> chips = new ArrayList<>();
        chips.add(CATEGORY_ALL);
        chips.addAll(remoteCategories);
        return chips;
    }

    private void showDeleteConfirmation(Product product) {
        DialogMenuDeleteConfirmBinding dialogBinding = DialogMenuDeleteConfirmBinding.inflate(getLayoutInflater());
        ImageLoader.load(dialogBinding.imageProductPreview, resolveImageUrl(product.getImageUrl(), product.getCategory()));
        dialogBinding.textDeleteTitle.setText("X\u00f3a \"" + product.getName() + "\"?");
        dialogBinding.textDeleteMessage.setText("H\u00e0nh \u0111\u1ed9ng n\u00e0y kh\u00f4ng th\u1ec3 ho\u00e0n t\u00e1c.");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .create();
        dialogBinding.buttonCancelDelete.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.buttonConfirmDelete.setOnClickListener(v ->
                apiRepository.deleteProduct(product.getId(), new ApiCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        if (!isAdded() || binding == null) {
                            return;
                        }
                        dialog.dismiss();
                        loadProducts();
                        refreshSummary();
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) {
                            return;
                        }
                        dialog.dismiss();
                    }
                }));
        dialog.show();
        styleDialogWindow(dialog);
    }

    private void showMenuDialog(@Nullable Product editingProduct) {
        DialogMenuFormBinding dialogBinding = DialogMenuFormBinding.inflate(getLayoutInflater());
        List<String> categories = new ArrayList<>(remoteCategories);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categories);
        dialogBinding.editCategory.setAdapter(arrayAdapter);

        boolean isEditing = editingProduct != null;
        dialogBinding.textDialogTitle.setText(isEditing ? "S\u1eeda m\u00f3n" : "Th\u00eam m\u00f3n m\u1edbi");
        dialogBinding.buttonSubmitMenu.setText(isEditing ? "L\u01b0u thay \u0111\u1ed5i" : "Th\u00eam m\u00f3n");

        if (isEditing) {
            dialogBinding.editName.setText(editingProduct.getName());
            dialogBinding.editPrice.setText(String.valueOf(editingProduct.getPrice()));
            dialogBinding.editCategory.setText(editingProduct.getCategory(), false);
            dialogBinding.editImageUrl.setText(editingProduct.getImageUrl());
        } else if (!categories.isEmpty()) {
            dialogBinding.editCategory.setText(categories.get(0), false);
        }

        loadPreview(dialogBinding, editingProduct);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .create();

        dialogBinding.buttonCloseDialog.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.buttonPreviewImage.setOnClickListener(v -> loadPreview(dialogBinding, editingProduct));
        dialogBinding.editCategory.setOnItemClickListener((parent, view, position, id) -> {
            if (valueOf(dialogBinding.editImageUrl.getText()).isEmpty()) {
                loadPreview(dialogBinding, editingProduct);
            }
        });
        dialogBinding.buttonSubmitMenu.setOnClickListener(v -> submitMenuForm(dialogBinding, dialog, editingProduct));

        dialog.show();
        styleDialogWindow(dialog);
    }

    private void submitMenuForm(DialogMenuFormBinding dialogBinding, AlertDialog dialog, @Nullable Product editingProduct) {
        String name = valueOf(dialogBinding.editName.getText());
        String priceValue = valueOf(dialogBinding.editPrice.getText());
        String category = valueOf(dialogBinding.editCategory.getText());
        String imageUrl = valueOf(dialogBinding.editImageUrl.getText());

        dialogBinding.inputName.setError(null);
        dialogBinding.inputPrice.setError(null);
        dialogBinding.inputCategory.setError(null);

        boolean hasError = false;
        if (name.isEmpty()) {
            dialogBinding.inputName.setError("Vui l\u00f2ng nh\u1eadp t\u00ean m\u00f3n");
            hasError = true;
        }
        if (priceValue.isEmpty()) {
            dialogBinding.inputPrice.setError("Vui l\u00f2ng nh\u1eadp gi\u00e1");
            hasError = true;
        }
        if (category.isEmpty()) {
            dialogBinding.inputCategory.setError("Vui l\u00f2ng ch\u1ecdn danh m\u1ee5c");
            hasError = true;
        }
        if (hasError) {
            return;
        }

        int price;
        try {
            price = Integer.parseInt(priceValue);
        } catch (NumberFormatException exception) {
            dialogBinding.inputPrice.setError("Gi\u00e1 ph\u1ea3i l\u00e0 s\u1ed1 h\u1ee3p l\u1ec7");
            return;
        }

        Product target = new Product(
                editingProduct == null ? 0 : editingProduct.getId(),
                name,
                price,
                category,
                editingProduct == null || editingProduct.isAvailable(),
                buildAssetLabel(name),
                editingProduct == null ? colorForCategory(category) : editingProduct.getAccentColorHex(),
                resolveImageUrl(imageUrl, category)
        );

        ApiCallback<Product> callback = new ApiCallback<Product>() {
            @Override
            public void onSuccess(Product data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                dialog.dismiss();
                loadProducts();
                refreshSummary();
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || binding == null) {
                    return;
                }
                dialogBinding.inputName.setError(message);
            }
        };

        if (editingProduct == null) {
            apiRepository.createProduct(target, callback);
        } else {
            apiRepository.updateProduct(target, callback);
        }
    }

    private void styleDialogWindow(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        int screenWidth = requireContext().getResources().getDisplayMetrics().widthPixels;
        int width = Math.round(screenWidth * 0.86f);
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setDimAmount(0.42f);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.y = getTopDialogOffset();
        window.setAttributes(attributes);
    }

    private int getTopDialogOffset() {
        int statusBarInset = 0;
        Window window = requireActivity().getWindow();
        View decorView = window.getDecorView();
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decorView);
        if (insets != null) {
            statusBarInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
        }
        return statusBarInset + dp(48);
    }

    private void loadPreview(DialogMenuFormBinding dialogBinding, @Nullable Product editingProduct) {
        String category = valueOf(dialogBinding.editCategory.getText());
        String imageUrl = valueOf(dialogBinding.editImageUrl.getText());
        if (imageUrl.isEmpty() && editingProduct != null) {
            imageUrl = editingProduct.getImageUrl();
        }
        ImageLoader.load(dialogBinding.imagePreview, resolveImageUrl(imageUrl, category));
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
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

    private String valueOf(Editable editable) {
        return editable == null ? "" : editable.toString().trim();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            loadProducts();
            refreshSummary();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
