package com.example.nhom8_makafe.ui.menu;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.example.nhom8_makafe.databinding.FragmentMenuManagementBinding;
import com.example.nhom8_makafe.model.Product;
import com.example.nhom8_makafe.model.User;
import com.example.nhom8_makafe.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

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
        registerOverlayResults();

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

    private void registerOverlayResults() {
        getParentFragmentManager().setFragmentResultListener(
                MenuOverlayContract.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    if (!result.getBoolean(MenuOverlayContract.RESULT_REFRESH, false)) {
                        return;
                    }
                    loadProducts();
                    refreshSummary();
                }
        );
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

    private void showDeleteConfirmation(@NonNull Product product) {
        if (getParentFragmentManager().findFragmentByTag(MenuDeleteConfirmOverlayFragment.TAG) != null) {
            return;
        }
        MenuDeleteConfirmOverlayFragment.newInstance(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getImageUrl()
        ).show(getParentFragmentManager(), MenuDeleteConfirmOverlayFragment.TAG);
    }

    private void showMenuDialog(@Nullable Product editingProduct) {
        if (getParentFragmentManager().findFragmentByTag(MenuFormOverlayFragment.TAG) != null) {
            return;
        }
        ArrayList<String> categories = new ArrayList<>(remoteCategories);
        if (editingProduct == null) {
            MenuFormOverlayFragment.newCreateInstance(categories)
                    .show(getParentFragmentManager(), MenuFormOverlayFragment.TAG);
            return;
        }
        MenuFormOverlayFragment.newEditInstance(editingProduct, categories)
                .show(getParentFragmentManager(), MenuFormOverlayFragment.TAG);
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
