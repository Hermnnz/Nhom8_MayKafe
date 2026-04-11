package com.example.nhom8_makafe.ui.main;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.data.SessionManager;
import com.example.nhom8_makafe.databinding.ActivityMainBinding;
import com.example.nhom8_makafe.databinding.ViewBottomNavAvatarItemBinding;
import com.example.nhom8_makafe.databinding.ViewBottomNavItemBinding;
import com.example.nhom8_makafe.model.CartItem;
import com.example.nhom8_makafe.model.Role;
import com.example.nhom8_makafe.model.User;
import com.example.nhom8_makafe.ui.cart.CartBottomSheetDialogFragment;
import com.example.nhom8_makafe.ui.invoices.InvoicesFragment;
import com.example.nhom8_makafe.ui.login.LoginFragment;
import com.example.nhom8_makafe.ui.menu.MenuManagementFragment;
import com.example.nhom8_makafe.ui.overlay.OverlayFragment;
import com.example.nhom8_makafe.ui.reports.ReportsFragment;
import com.example.nhom8_makafe.ui.sales.SalesFragment;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements SessionManager.Observer, MainNavigator, LogoutConfirmOverlayFragment.Listener {
    private static final int MAX_BADGE_VALUE = 99;

    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private MainDestination currentDestination = MainDestination.SALES;
    private int bottomNavBaseBottomPadding;

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
        bottomNavBaseBottomPadding = binding.bottomNavigation.getPaddingBottom();
        applyBottomNavigationInsets();

        configureStandardItem(binding.navSalesItem, R.string.nav_sales, R.drawable.ic_nav_sales_enabled,
                R.drawable.ic_nav_sales_disabled, () -> navigateTo(MainDestination.SALES));
        configureStandardItem(binding.navInvoicesItem, R.string.nav_invoices, R.drawable.ic_nav_invoices_enabled,
                R.drawable.ic_nav_invoices_disabled, () -> navigateTo(MainDestination.INVOICES));
        configureStandardItem(binding.navMenuItem, R.string.nav_menu, R.drawable.ic_nav_menu_enabled,
                R.drawable.ic_nav_menu_disabled, () -> navigateTo(MainDestination.MENU));
        configureStandardItem(binding.navReportsItem, R.string.nav_reports, R.drawable.ic_nav_reports_enabled,
                R.drawable.ic_nav_reports_disabled, () -> navigateTo(MainDestination.REPORTS));

        binding.navAccountItem.textLabel.setText(R.string.nav_account);
        binding.navAccountItem.navItemRoot.setOnClickListener(v -> requestLogout());
        binding.navAccountItem.imageBadge.setImageResource(R.drawable.ic_nav_badge_logout);

        updateAccountNavigationItem(null);
        renderBottomNavigationState(null);
        updateCartBadge(0);
    }

    private void applyBottomNavigationInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    bottomNavBaseBottomPadding + systemBars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(binding.bottomNavigation);
    }

    private void configureStandardItem(@NonNull ViewBottomNavItemBinding itemBinding,
                                       int labelRes,
                                       int enabledIconRes,
                                       int disabledIconRes,
                                       @NonNull Runnable action) {
        itemBinding.textLabel.setText(labelRes);
        itemBinding.imageIcon.setImageResource(enabledIconRes);
        itemBinding.navItemRoot.setOnClickListener(v -> action.run());
        itemBinding.navItemRoot.setTag(new int[]{enabledIconRes, disabledIconRes});
    }

    private void renderAuthenticationState() {
        clearOverlayLayer();
        if (!sessionManager.isLoggedIn()) {
            updateAccountNavigationItem(null);
            binding.bottomNavigation.setVisibility(View.GONE);
            replaceRootFragment(LoginFragment.newInstance());
            return;
        }
        binding.bottomNavigation.setVisibility(View.VISIBLE);
        User currentUser = sessionManager.getCurrentUser();
        updateAccountNavigationItem(currentUser);
        applyRoleRestrictions(currentUser);
        navigateTo(currentDestination);
        updateCartBadge(sessionManager.getCartCount());
    }

    private void applyRoleRestrictions(@Nullable User user) {
        if (user == null) {
            return;
        }
        boolean isAdmin = user.getRole() == Role.ADMIN;
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
        renderBottomNavigationState(user);
        clearOverlayLayer();

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

    private void renderBottomNavigationState(@Nullable User user) {
        boolean loggedIn = user != null;
        boolean isAdmin = loggedIn && user.getRole() == Role.ADMIN;

        setStandardItemState(binding.navSalesItem, currentDestination == MainDestination.SALES, loggedIn);
        setStandardItemState(binding.navInvoicesItem, currentDestination == MainDestination.INVOICES, loggedIn);
        setStandardItemState(binding.navMenuItem, currentDestination == MainDestination.MENU && isAdmin, isAdmin);
        setStandardItemState(binding.navReportsItem, currentDestination == MainDestination.REPORTS && isAdmin, isAdmin);
        setAvatarItemState(binding.navAccountItem, false, loggedIn, user);
    }

    private void setStandardItemState(@NonNull ViewBottomNavItemBinding itemBinding,
                                      boolean active,
                                      boolean enabled) {
        int[] iconRes = (int[]) itemBinding.navItemRoot.getTag();
        itemBinding.navItemRoot.setEnabled(enabled);
        itemBinding.imageIcon.setImageResource(enabled ? iconRes[0] : iconRes[1]);
        itemBinding.imageIcon.setAlpha(enabled ? (active ? 1f : 0.72f) : 1f);
        itemBinding.textLabel.setTextColor(getColor(enabled
                ? (active ? R.color.bottom_nav_active : R.color.bottom_nav_inactive)
                : R.color.bottom_nav_disabled));
        itemBinding.textLabel.setTypeface(Typeface.create(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL));
        itemBinding.viewIndicator.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
        itemBinding.textBadge.setAlpha(enabled ? 1f : 0.72f);
    }

    private void setAvatarItemState(@NonNull ViewBottomNavAvatarItemBinding itemBinding,
                                    boolean active,
                                    boolean enabled,
                                    @Nullable User user) {
        itemBinding.navItemRoot.setEnabled(enabled);
        itemBinding.viewIndicator.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
        itemBinding.textLabel.setTextColor(getColor(enabled
                ? (active ? R.color.bottom_nav_active : R.color.bottom_nav_inactive)
                : R.color.bottom_nav_disabled));
        itemBinding.textLabel.setTypeface(Typeface.create(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL));
        itemBinding.layoutAvatarBadge.setAlpha(enabled ? 1f : 0.6f);
        itemBinding.textAvatar.setAlpha(enabled ? 1f : 0.58f);

        int avatarColor = enabled && user != null ? resolveAvatarColor(user) : getColor(R.color.bottom_nav_disabled);
        itemBinding.textAvatar.setBackground(createAvatarBackground(avatarColor, active));
    }

    private void updateCartBadge(int count) {
        TextView badgeView = binding.navSalesItem.textBadge;
        if (count <= 0) {
            badgeView.setVisibility(View.GONE);
            return;
        }
        badgeView.setVisibility(View.VISIBLE);
        badgeView.setText(count > MAX_BADGE_VALUE ? "99+" : String.valueOf(count));
    }

    private void updateAccountNavigationItem(@Nullable User user) {
        if (user == null) {
            binding.navAccountItem.textAvatar.setText("MK");
            binding.navAccountItem.textLabel.setText(R.string.nav_account);
            binding.navAccountItem.layoutAvatarBadge.setVisibility(View.GONE);
            setAvatarItemState(binding.navAccountItem, false, false, null);
            return;
        }
        binding.navAccountItem.textAvatar.setText(resolveAccountInitials(user));
        binding.navAccountItem.textLabel.setText(resolveShortName(user.getDisplayName()));
        binding.navAccountItem.layoutAvatarBadge.setVisibility(View.VISIBLE);
        setAvatarItemState(binding.navAccountItem, false, true, user);
    }

    private GradientDrawable createAvatarBackground(int fillColor, boolean active) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fillColor);
        if (active) {
            drawable.setStroke(dp(2), getColor(R.color.bottom_nav_active));
        }
        return drawable;
    }

    private int resolveAvatarColor(@NonNull User user) {
        try {
            return Color.parseColor(user.getAvatarColorHex());
        } catch (IllegalArgumentException ignored) {
            return getColor(R.color.coffee_700);
        }
    }

    private String resolveAccountInitials(@NonNull User user) {
        String initials = user.getAvatarInitials();
        if (initials != null && !initials.trim().isEmpty()) {
            String compact = initials.trim().toUpperCase(Locale.getDefault());
            return compact.length() > 2 ? compact.substring(0, 2) : compact;
        }
        String shortName = resolveShortName(user.getDisplayName());
        return shortName.substring(0, Math.min(shortName.length(), 2)).toUpperCase(Locale.getDefault());
    }

    private String resolveShortName(@Nullable String displayName) {
        if (displayName == null) {
            return getString(R.string.nav_account);
        }
        String trimmed = displayName.trim();
        if (trimmed.isEmpty()) {
            return getString(R.string.nav_account);
        }
        String[] parts = trimmed.split("\\s+");
        String rawShortName = parts[parts.length - 1];
        if (rawShortName.isEmpty()) {
            return getString(R.string.nav_account);
        }
        if (rawShortName.length() == 1) {
            return rawShortName.toUpperCase(Locale.getDefault());
        }
        return rawShortName.substring(0, 1).toUpperCase(Locale.getDefault())
                + rawShortName.substring(1).toLowerCase(Locale.getDefault());
    }

    private void replaceRootFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
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
        if (getSupportFragmentManager().findFragmentByTag(LogoutConfirmOverlayFragment.TAG) == null) {
            LogoutConfirmOverlayFragment.newInstance(user.getDisplayName())
                    .show(getSupportFragmentManager(), LogoutConfirmOverlayFragment.TAG);
        }
    }

    @Override
    public void openCartSheet() {
        if (getSupportFragmentManager().findFragmentByTag("cart_sheet") == null) {
            CartBottomSheetDialogFragment.newInstance()
                    .show(getSupportFragmentManager(), "cart_sheet");
        }
    }

    @Override
    public void onLogoutConfirmed() {
        currentDestination = MainDestination.SALES;
        sessionManager.logout();
    }

    private void clearOverlayLayer() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        androidx.fragment.app.FragmentTransaction transaction = null;
        for (Fragment fragment : fragments) {
            if (!(fragment instanceof OverlayFragment)) {
                continue;
            }
            if (transaction == null) {
                transaction = getSupportFragmentManager().beginTransaction().setReorderingAllowed(true);
            }
            transaction.remove(fragment);
        }
        if (transaction != null) {
            transaction.commitAllowingStateLoss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sessionManager.removeObserver(this);
        binding = null;
    }
}
