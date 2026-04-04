package com.example.nhom8_makafe.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.data.SessionManager;
import com.example.nhom8_makafe.databinding.ActivityMainBinding;
import com.example.nhom8_makafe.model.CartItem;
import com.example.nhom8_makafe.model.Role;
import com.example.nhom8_makafe.model.User;
import com.example.nhom8_makafe.ui.cart.CartBottomSheetDialogFragment;
import com.example.nhom8_makafe.ui.invoices.InvoicesFragment;
import com.example.nhom8_makafe.ui.login.LoginFragment;
import com.example.nhom8_makafe.ui.menu.MenuManagementFragment;
import com.example.nhom8_makafe.ui.reports.ReportsFragment;
import com.example.nhom8_makafe.ui.sales.SalesFragment;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.navigation.NavigationBarView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SessionManager.Observer, MainNavigator {
    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private MainDestination currentDestination = MainDestination.SALES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = SessionManager.getInstance();
        sessionManager.addObserver(this);

        setupBottomNavigation();
        renderAuthenticationState();
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setItemIconTintList(getColorStateList(R.color.bottom_nav_icon_tint));
        binding.bottomNavigation.setItemTextColor(getColorStateList(R.color.bottom_nav_text_tint));
        binding.bottomNavigation.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        binding.bottomNavigation.setOnItemSelectedListener(this::handleBottomNavigation);
    }

    private boolean handleBottomNavigation(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_account) {
            requestLogout();
            return false;
        }
        if (itemId == R.id.nav_sales) {
            navigateTo(MainDestination.SALES);
            return true;
        }
        if (itemId == R.id.nav_invoices) {
            navigateTo(MainDestination.INVOICES);
            return true;
        }
        if (itemId == R.id.nav_menu) {
            navigateTo(MainDestination.MENU);
            return true;
        }
        if (itemId == R.id.nav_reports) {
            navigateTo(MainDestination.REPORTS);
            return true;
        }
        return false;
    }

    private void renderAuthenticationState() {
        if (!sessionManager.isLoggedIn()) {
            binding.bottomNavigation.setVisibility(View.GONE);
            replaceRootFragment(LoginFragment.newInstance());
            return;
        }
        binding.bottomNavigation.setVisibility(View.VISIBLE);
        applyRoleRestrictions(sessionManager.getCurrentUser());
        binding.bottomNavigation.setSelectedItemId(destinationToMenuId(currentDestination));
        navigateTo(currentDestination);
        updateCartBadge(sessionManager.getCartCount());
    }

    private void applyRoleRestrictions(User user) {
        if (user == null) {
            return;
        }
        Menu menu = binding.bottomNavigation.getMenu();
        MenuItem menuItem = menu.findItem(R.id.nav_menu);
        MenuItem reportsItem = menu.findItem(R.id.nav_reports);
        boolean isAdmin = user.getRole() == Role.ADMIN;
        menuItem.setEnabled(isAdmin);
        reportsItem.setEnabled(isAdmin);
        if (!isAdmin && (currentDestination == MainDestination.MENU || currentDestination == MainDestination.REPORTS)) {
            currentDestination = MainDestination.SALES;
        }
    }

    private void navigateTo(MainDestination destination) {
        User user = sessionManager.getCurrentUser();
        if (user == null) {
            return;
        }
        if (user.getRole() == Role.STAFF
                && (destination == MainDestination.MENU || destination == MainDestination.REPORTS)) {
            destination = MainDestination.SALES;
        }
        currentDestination = destination;
        Fragment fragment;
        switch (destination) {
            case INVOICES:
                fragment = InvoicesFragment.newInstance();
                break;
            case MENU:
                fragment = MenuManagementFragment.newInstance();
                break;
            case REPORTS:
                fragment = ReportsFragment.newInstance();
                break;
            case SALES:
            default:
                fragment = SalesFragment.newInstance();
                break;
        }
        replaceRootFragment(fragment);
    }

    private void replaceRootFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private int destinationToMenuId(MainDestination destination) {
        switch (destination) {
            case INVOICES:
                return R.id.nav_invoices;
            case MENU:
                return R.id.nav_menu;
            case REPORTS:
                return R.id.nav_reports;
            case SALES:
            default:
                return R.id.nav_sales;
        }
    }

    private void updateCartBadge(int count) {
        if (count <= 0) {
            binding.bottomNavigation.removeBadge(R.id.nav_sales);
            return;
        }
        BadgeDrawable badgeDrawable = binding.bottomNavigation.getOrCreateBadge(R.id.nav_sales);
        badgeDrawable.setBackgroundColor(getColor(R.color.accent_gold));
        badgeDrawable.setBadgeTextColor(getColor(R.color.badge_text));
        badgeDrawable.setVisible(true);
        badgeDrawable.setNumber(count);
        badgeDrawable.setMaxCharacterCount(3);
    }

    @Override
    public void onSessionChanged(User user) {
        renderAuthenticationState();
    }

    @Override
    public void onCartChanged(List<CartItem> cartItems) {
        updateCartBadge(sessionManager.getCartCount());
    }

    @Override
    public void requestLogout() {
        User user = sessionManager.getCurrentUser();
        if (user == null) {
            return;
        }
        showLogoutDialog(user);
    }

    private void showLogoutDialog(User user) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_auth_confirm, null, false);
        AppCompatTextView textTitle = dialogView.findViewById(R.id.text_dialog_title);
        AppCompatTextView textMessage = dialogView.findViewById(R.id.text_dialog_message);
        AppCompatButton buttonCancel = dialogView.findViewById(R.id.button_cancel);
        AppCompatButton buttonConfirm = dialogView.findViewById(R.id.button_confirm);

        textTitle.setText("Đăng xuất?");
        textMessage.setText("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản " + user.getDisplayName() + "?");
        buttonCancel.setText("Hủy");
        buttonConfirm.setText("Đăng xuất");

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            currentDestination = MainDestination.SALES;
            sessionManager.logout();
        });

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    @Override
    public void openCartSheet() {
        CartBottomSheetDialogFragment.newInstance()
                .show(getSupportFragmentManager(), "cart_sheet");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sessionManager.removeObserver(this);
        binding = null;
    }
}
