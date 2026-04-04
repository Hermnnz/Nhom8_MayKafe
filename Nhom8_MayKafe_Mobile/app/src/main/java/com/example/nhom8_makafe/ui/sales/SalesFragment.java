package com.example.nhom8_makafe.ui.sales;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nhom8_makafe.adapter.CategoryChipAdapter;
import com.example.nhom8_makafe.adapter.ProductAdapter;
import com.example.nhom8_makafe.data.SessionManager;
import com.example.nhom8_makafe.data.api.ApiCallback;
import com.example.nhom8_makafe.data.api.ApiRepository;
import com.example.nhom8_makafe.databinding.FragmentSalesBinding;
import com.example.nhom8_makafe.model.CartItem;
import com.example.nhom8_makafe.model.Product;
import com.example.nhom8_makafe.model.User;
import com.example.nhom8_makafe.ui.main.MainNavigator;
import com.example.nhom8_makafe.util.FormatUtils;
import com.example.nhom8_makafe.util.UiUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SalesFragment extends Fragment implements SessionManager.Observer {
    private static final String CATEGORY_ALL = "T\u1ea5t c\u1ea3";

    private FragmentSalesBinding binding;
    private CategoryChipAdapter categoryAdapter;
    private ProductAdapter productAdapter;
    private final ApiRepository apiRepository = ApiRepository.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private String selectedCategory = CATEGORY_ALL;
    private String searchQuery = "";

    public static SalesFragment newInstance() {
        return new SalesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSalesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupHeader();
        setupRecyclerViews();
        setupSearch();
        sessionManager.addObserver(this);
        loadCategories();
        loadProducts();
        renderCartSummary();
    }

    private void setupHeader() {
        User user = sessionManager.getCurrentUser();
        if (user == null) {
            return;
        }
        binding.textGreeting.setText(UiUtils.greeting() + ",");
        binding.textDisplayName.setText(user.getDisplayName());
        binding.textRole.setText(UiUtils.roleLabel(user.getRole()));
        UiUtils.bindThumbnail(binding.textAvatar, binding.textAvatar, user.getAvatarInitials(), user.getAvatarColorHex());
        binding.buttonOpenCart.setOnClickListener(v -> {
            if (getActivity() instanceof MainNavigator) {
                ((MainNavigator) getActivity()).openCartSheet();
            }
        });
    }

    private void setupRecyclerViews() {
        categoryAdapter = new CategoryChipAdapter(category -> {
            selectedCategory = category;
            categoryAdapter.setSelectedCategory(category);
            loadProducts();
        });
        binding.recyclerCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerCategories.setAdapter(categoryAdapter);
        categoryAdapter.submitList(buildCategoryChips(new ArrayList<>()));
        categoryAdapter.setSelectedCategory(selectedCategory);

        productAdapter = new ProductAdapter(new ProductAdapter.ProductActionListener() {
            @Override
            public void onAdd(Product product) {
                sessionManager.addProduct(product);
            }

            @Override
            public void onIncrease(Product product) {
                sessionManager.increaseItem(product.getId());
            }

            @Override
            public void onDecrease(Product product) {
                sessionManager.decreaseItem(product.getId());
            }
        });
        binding.recyclerProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerProducts.setAdapter(productAdapter);
    }

    private void setupSearch() {
        binding.editSearch.addTextChangedListener(new TextWatcher() {
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
    }

    private void loadCategories() {
        apiRepository.fetchCategories(new ApiCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                List<String> categories = buildCategoryChips(data);
                categoryAdapter.submitList(categories);
                if (!categories.contains(selectedCategory)) {
                    selectedCategory = CATEGORY_ALL;
                }
                categoryAdapter.setSelectedCategory(selectedCategory);
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || binding == null) {
                    return;
                }
                categoryAdapter.submitList(buildCategoryChips(new ArrayList<>()));
                categoryAdapter.setSelectedCategory(selectedCategory);
            }
        });
    }

    private void loadProducts() {
        if (binding == null) {
            return;
        }
        binding.textEmptyProducts.setVisibility(View.GONE);
        String category = CATEGORY_ALL.equals(selectedCategory) ? null : selectedCategory;
        apiRepository.fetchProducts(searchQuery, category, true, new ApiCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                productAdapter.submitList(data);
                productAdapter.setQuantityMap(buildQuantityMap(sessionManager.getCartItems()));
                binding.textEmptyProducts.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || binding == null) {
                    return;
                }
                productAdapter.submitList(new ArrayList<>());
                productAdapter.setQuantityMap(buildQuantityMap(sessionManager.getCartItems()));
                binding.textEmptyProducts.setVisibility(View.VISIBLE);
                binding.textEmptyProducts.setText(message == null || message.trim().isEmpty()
                        ? "Kh\u00f4ng t\u1ea3i \u0111\u01b0\u1ee3c danh s\u00e1ch m\u00f3n."
                        : message);
            }
        });
    }

    private List<String> buildCategoryChips(List<String> remoteCategories) {
        List<String> chips = new ArrayList<>();
        chips.add(CATEGORY_ALL);
        chips.addAll(remoteCategories);
        return chips;
    }

    private Map<Long, Integer> buildQuantityMap(List<CartItem> cartItems) {
        Map<Long, Integer> quantities = new HashMap<>();
        for (CartItem cartItem : cartItems) {
            quantities.put(cartItem.getProductId(), cartItem.getQuantity());
        }
        return quantities;
    }

    private void renderCartSummary() {
        int cartCount = sessionManager.getCartCount();
        int subtotal = sessionManager.getCartSubtotal();
        binding.buttonOpenCart.setVisibility(cartCount > 0 ? View.VISIBLE : View.GONE);
        if (cartCount > 0) {
            binding.buttonOpenCart.setText(cartCount + " m\u00f3n \u2022 " + FormatUtils.formatCurrency(subtotal));
        }
        if (productAdapter != null) {
            productAdapter.setQuantityMap(buildQuantityMap(sessionManager.getCartItems()));
        }
    }

    @Override
    public void onSessionChanged(User user) {
        if (user != null && binding != null) {
            setupHeader();
            loadCategories();
            loadProducts();
        }
    }

    @Override
    public void onCartChanged(List<CartItem> cartItems) {
        if (binding != null) {
            renderCartSummary();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            loadProducts();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sessionManager.removeObserver(this);
        binding = null;
    }
}
