package com.example.nhom8_makafe.data;

import com.example.nhom8_makafe.model.CartItem;
import com.example.nhom8_makafe.model.Product;
import com.example.nhom8_makafe.model.User;

import java.util.ArrayList;
import java.util.List;

public class SessionManager {
    public interface Observer {
        void onSessionChanged(User user);

        void onCartChanged(List<CartItem> cartItems);
    }

    private static SessionManager instance;

    private final List<Observer> observers = new ArrayList<>();
    private final List<CartItem> cartItems = new ArrayList<>();
    private String authToken;
    private User currentUser;

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(User user) {
        login(null, user);
    }

    public void login(String token, User user) {
        authToken = token;
        currentUser = user;
        notifySessionChanged();
    }

    public void logout() {
        authToken = null;
        currentUser = null;
        cartItems.clear();
        notifySessionChanged();
        notifyCartChanged();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getAuthHeader() {
        if (authToken == null || authToken.trim().isEmpty()) {
            return null;
        }
        return "Token " + authToken.trim();
    }

    public List<CartItem> getCartItems() {
        return new ArrayList<>(cartItems);
    }

    public int getCartCount() {
        int count = 0;
        for (CartItem item : cartItems) {
            count += item.getQuantity();
        }
        return count;
    }

    public int getCartSubtotal() {
        int subtotal = 0;
        for (CartItem item : cartItems) {
            subtotal += item.getPrice() * item.getQuantity();
        }
        return subtotal;
    }

    public void addProduct(Product product) {
        for (CartItem item : cartItems) {
            if (item.getProductId() == product.getId()) {
                item.setQuantity(item.getQuantity() + 1);
                notifyCartChanged();
                return;
            }
        }
        cartItems.add(new CartItem(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getCategory(),
                product.getAssetLabel(),
                product.getAccentColorHex(),
                1
        ));
        notifyCartChanged();
    }

    public void increaseItem(long productId) {
        for (CartItem item : cartItems) {
            if (item.getProductId() == productId) {
                item.setQuantity(item.getQuantity() + 1);
                break;
            }
        }
        notifyCartChanged();
    }

    public void decreaseItem(long productId) {
        CartItem target = null;
        for (CartItem item : cartItems) {
            if (item.getProductId() == productId) {
                if (item.getQuantity() <= 1) {
                    target = item;
                } else {
                    item.setQuantity(item.getQuantity() - 1);
                }
                break;
            }
        }
        if (target != null) {
            cartItems.remove(target);
        }
        notifyCartChanged();
    }

    public void updateNote(long productId, String note) {
        for (CartItem item : cartItems) {
            if (item.getProductId() == productId) {
                item.setNote(note == null ? "" : note);
                break;
            }
        }
        notifyCartChanged();
    }

    public void clearCart() {
        cartItems.clear();
        notifyCartChanged();
    }

    public void addObserver(Observer observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    private void notifySessionChanged() {
        for (Observer observer : new ArrayList<>(observers)) {
            observer.onSessionChanged(currentUser);
        }
    }

    private void notifyCartChanged() {
        List<CartItem> snapshot = getCartItems();
        for (Observer observer : new ArrayList<>(observers)) {
            observer.onCartChanged(snapshot);
        }
    }
}
