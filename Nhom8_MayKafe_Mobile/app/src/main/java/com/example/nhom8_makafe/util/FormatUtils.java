package com.example.nhom8_makafe.util;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class FormatUtils {
    private static final Locale VIETNAMESE = new Locale("vi", "VN");

    private FormatUtils() {
    }

    public static String formatCurrency(int amount) {
        return NumberFormat.getNumberInstance(VIETNAMESE).format(amount) + "\u0111";
    }

    public static String formatCompactCurrency(int amount) {
        if (amount >= 1_000_000) {
            return String.format(VIETNAMESE, "%.1ftr", amount / 1_000_000f);
        }
        if (amount >= 1_000) {
            return String.format(VIETNAMESE, "%.0fk", amount / 1_000f);
        }
        return String.valueOf(amount);
    }

    public static String currentTime() {
        return new SimpleDateFormat("HH:mm", VIETNAMESE).format(new Date());
    }

    public static String currentDate() {
        return new SimpleDateFormat("dd/MM/yyyy", VIETNAMESE).format(new Date());
    }
}
