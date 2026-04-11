package com.example.nhom8_makafe.util;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

import com.example.nhom8_makafe.model.PaymentMethod;
import com.example.nhom8_makafe.model.Role;

import java.util.Calendar;

public final class UiUtils {
    private UiUtils() {
    }

    public static GradientDrawable roundedBackground(String colorHex, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(colorHex));
        drawable.setCornerRadius(radiusDp);
        return drawable;
    }

    public static void bindThumbnail(View container, TextView textView, String label, String colorHex) {
        textView.setText(label);
        if (container == textView) {
            textView.setBackground(roundedBackground(colorHex, 32f));
            return;
        }
        container.setBackground(roundedBackground(colorHex, 32f));
        textView.setBackground(roundedBackground(withAlpha(colorHex, 0.18f), 24f));
    }

    public static String greeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return "Chào buổi sáng";
        }
        if (hour < 18) {
            return "Chào buổi chiều";
        }
        return "Chào buổi tối";
    }

    public static String roleLabel(Role role) {
        return role == Role.ADMIN ? "Quản lý" : "Nhân viên";
    }

    public static String paymentLabel(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.CASH ? "Tiền mặt" : "Chuyển khoản QR";
    }

    public static String withAlpha(String colorHex, float factor) {
        int color = Color.parseColor(colorHex);
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return String.format("#%02X%02X%02X%02X", alpha, red, green, blue);
    }
}
