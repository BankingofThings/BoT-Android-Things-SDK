package io.bankingofthings.iot.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import io.bankingofthings.iot.model.domain.DeviceModel
import java.io.File
import java.io.FileOutputStream

object QRUtil {
    fun create(deviceModel: DeviceModel): Bitmap {
        val qrBitmap = encodeAsBitmap(Gson().toJson(deviceModel))

        val sdCard = Environment.getExternalStorageDirectory()
        val file = File(sdCard, "qr_${deviceModel.deviceID}.jpg")
        val fileOutputStream = FileOutputStream(file)

        qrBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)

        fileOutputStream.flush()
        fileOutputStream.close()

        return qrBitmap
    }

    /**
     * TODO catch writer exception while encoding?
     */
    private fun encodeAsBitmap(message: String): Bitmap {
        val result: BitMatrix = MultiFormatWriter().encode(
            message, BarcodeFormat.QR_CODE, 512, 512, null
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
