package io.bankingofthings.iot.repo

import android.graphics.Bitmap
import android.os.Environment
import io.bankingofthings.iot.storage.SpHelper
import io.bankingofthings.iot.utils.QRUtil
import java.io.File
import java.io.FileOutputStream

class QrRepo(private val spHelper:SpHelper, private val deviceRepo: DeviceRepo) {
    val qrBitmap: Bitmap = QRUtil.createBitmap(deviceRepo.deviceModel)

    init {
        // Do only for every new device
        if (!spHelper.getHasStoredQrImage()) {
            storeBitmapToSdCard()
            spHelper.setHasStoredQrImage(true)
        }
    }

    private fun storeBitmapToSdCard() {
        val sdCard = Environment.getExternalStorageDirectory()
        val file = File(sdCard, "qr_${deviceRepo.deviceModel.deviceID}.jpg")
        val fileOutputStream = FileOutputStream(file)

        qrBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)

        fileOutputStream.flush()
        fileOutputStream.close()
    }

    fun destroyBitmap() {
        if (!qrBitmap.isRecycled) {
            qrBitmap.recycle()
        }
    }
}

