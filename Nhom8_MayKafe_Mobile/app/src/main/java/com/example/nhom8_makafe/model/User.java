package com.example.nhom8_makafe.model;

public class User {
    private final String username;
    private final String password;
    private final String displayName;
    private final Role role;
    private final String avatarInitials;
    private final String avatarColorHex;

    public User(String username, String password, String displayName, Role role, String avatarInitials, String avatarColorHex) {
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.role = role;
        this.avatarInitials = avatarInitials;
        this.avatarColorHex = avatarColorHex;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public String getAvatarInitials() {
        return avatarInitials;
    }

    public String getAvatarColorHex() {
        return avatarColorHex;
    }
}
