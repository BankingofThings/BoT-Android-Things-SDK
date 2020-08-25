package io.bankingofthings.iot.model.domain

import android.os.Parcelable

/**
 * Defines device type
 * @param value is set by iot or CORE
 */
enum class ProductType(val value: Int) {
    OWNED(0),
    SHARED(1),
    RENTAL(2),
    PAYPERUSE(3)
}
