package com.example.nhom8_makafe.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.EnumMap;
import java.util.Map;

public final class QrBitmapUtils {
    private QrBitmapUtils() {
    }

    @Nullable
    public static Bitmap createQrBitmap(String value, int sizePx) {
        if (value == null || value.trim().isEmpty() || sizePx <= 0) {
            return null;
        }
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);
        try {
            BitMatrix bitMatrix = new MultiFormatWriter()
                    .encode(value, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < sizePx; x++) {
                for (int y = 0; y < sizePx; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.parseColor("#6B3F2A") : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException exception) {
            return null;
        }
    }
}
