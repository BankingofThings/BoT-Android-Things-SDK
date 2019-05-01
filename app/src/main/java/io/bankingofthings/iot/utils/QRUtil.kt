package io.bankingofthings.iot.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QRUtil {
    /**
     * TODO catch writer exception while encoding?
     */
    fun encodeAsBitmap(str: String): Bitmap {
        val result: BitMatrix = MultiFormatWriter().encode(
            str, BarcodeFormat.QR_CODE, 512, 512, null
        )

        val w = result.width
        val h = result.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (result.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, 512, 0, 0, w, h)
        return bitmap
    }
}
