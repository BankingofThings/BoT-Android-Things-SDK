package io.bankingofthings.iot.utils

import android.util.Base64
import com.google.gson.Gson
import io.bankingofthings.iot.network.pojo.ActivateDeviceParamPojo
import io.bankingofthings.iot.network.pojo.TriggerActionParamPojo
import java.security.Signature
import java.security.interfaces.RSAPrivateKey

object JWTUtil {
    class Header(val alg: String = "RS256", val type: String = "JWT")

    fun create(pojo: Any, privateKey: RSAPrivateKey): String {
        val header = Gson().toJson(Header())
        val payload = Gson().toJson(pojo)

        val h64 = Base64.encodeToString(
            header.toByteArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_CLOSE or Base64.NO_WRAP
        )

        val p64 = Base64.encodeToString(
            payload.toByteArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_CLOSE or Base64.NO_WRAP
        )
        val signaturePart = "$h64.$p64"

        val signature = Signature
            .getInstance("SHA256withRSA")
            .let {
                it.initSign(privateKey)
                it.update(signaturePart.toByteArray())
                Base64.encodeToString(
                    it.sign(),
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_CLOSE or Base64.NO_WRAP
                )
            }

        return "$h64.$p64.$signature"
    }
}
