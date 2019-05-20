package io.bankingofthings.iot.repo

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.bankingofthings.iot.storage.SpHelper
import java.security.*
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAKeyGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeyRepo(spHelper: SpHelper) {
    var publicKey: PublicKey
    var privateKey: PrivateKey
    val serverPublicKey: PublicKey

    init {
        if (!spHelper.getHasKeyPair()) {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
                .apply {
                    initialize(
                        KeyGenParameterSpec
                            .Builder(
                                "jwt",
                                KeyProperties.PURPOSE_SIGN
                            )
                            .setDigests(KeyProperties.DIGEST_SHA256)
                            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                            .setKeySize(1024)
                            .build()
                    )

                    generateKeyPair()
                }

            spHelper.setHasKeyPair(true)
        }

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        privateKey = (keyStore.getEntry("jwt", null) as KeyStore.PrivateKeyEntry).privateKey
        publicKey = (keyStore.getEntry("jwt", null) as KeyStore.PrivateKeyEntry).certificate.publicKey

        serverPublicKey = getServerRSAPublicKey()
    }

    private fun getServerRSAPublicKey(): PublicKey {
        val key: String = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA77WbE+3tVs14y0I+LeEx\n" +
                "NJ2qB0OlKBu33lfFbYMUMPi6T+3/M83A2C/alDDRO2NHPvzK6xGvYa2U/NpdNsyg\n" +
                "gA92BXK64mBhUc9SBbVAhMX5WKOs0daJ7OhBqOrHKHVy4Enhlk1uSL3zONQ0mBlh\n" +
                "ULYA7qZNy82UBa3MDtimg1TwaPVNjPENalUmyX65TpQHzwUhhPBQQ0BecfhaWBuv\n" +
                "7ZSLumd+sFG6DDEtjnpSHLYRYzlLU/iM9EZPXf3I4SpqlRVzzf8pZnowDOjMSSrY\n" +
                "tAaMAFNaKJDvGGqNIG7Fd3c2vPdYZ3NoXwGo1gRv4clbtx/F1xpEeYFE7qamTayd\n" +
                "iwRUgv2lGZxpnWU4WWcqOb+FWlR+6DzJVHsVHmgx//1FOiNssIGzGW/LBdaOycSS\n" +
                "wSM5GETtiZwTOjqqmSxXZtJBvjj4eHrDQ1m9lvSyYWrnVeclD/44AO+G96z7sbp4\n" +
                "c8BHpXCBuDuwK9Kf87SNJF4yLfpi7VxMF/YC+DBqvidLSFOOFjlMpJkF1oKvzGPu\n" +
                "HE/C4k+4yZtml3e7R15JTTgdHuTKDMfk0xxlcPkjt5PPeBiOawDXNWLLk3Kv8Rql\n" +
                "SrhGlSobfvRlu3Z4BvZxgKYg5SSqu3zNImJ6TBG+gOLQ2+vzaOEWCnvfDbSYj8yD\n" +
                "wTU89h00sASDai3lEuuzb10CAwEAAQ=="

        return KeyFactory
            .getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(Base64.decode(key, Base64.DEFAULT)))
    }
}
