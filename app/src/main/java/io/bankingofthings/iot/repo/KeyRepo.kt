package io.bankingofthings.iot.repo

import android.util.Base64
import io.bankingofthings.iot.storage.SpHelper
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

class KeyRepo(
    private val spHelper: SpHelper
) {
    var publicKey: RSAPublicKey
    var privateKey: RSAPrivateKey
    val serverPublicKey: RSAPublicKey

    init {
        if (!spHelper.getHasKeyPair()) {
            System.out.println("KeyRepo: create new keys")

            val gen = KeyPairGenerator.getInstance("RSA")
            gen.initialize(1024, SecureRandom())
            val keyPair = gen.genKeyPair()

            publicKey = keyPair.public as RSAPublicKey
            privateKey = keyPair.private as RSAPrivateKey

            spHelper.storePubicKey(Base64.encodeToString(publicKey.encoded, Base64.DEFAULT))
            spHelper.storePrivateKey(Base64.encodeToString(privateKey.encoded, Base64.DEFAULT))
            spHelper.setHasKeyPair(true)
        } else {
            publicKey = KeyFactory
                .getInstance("RSA")
                .generatePublic(
                    X509EncodedKeySpec(
                        Base64.decode(
                            spHelper.getPublicKey(),
                            Base64.DEFAULT
                        )
                    )
                ) as RSAPublicKey

            privateKey = KeyFactory
                .getInstance("RSA")
                .generatePrivate(
                    PKCS8EncodedKeySpec(
                        Base64.decode(
                            spHelper.getPrivateKey(),
                            Base64.DEFAULT
                        )
                    )
                ) as RSAPrivateKey

            val public = Base64.encodeToString(publicKey.encoded, Base64.DEFAULT)
            val private = Base64.encodeToString(privateKey.encoded, Base64.DEFAULT)

            System.out.println("KeyRepo: public = ${public}")
            System.out.println("KeyRepo: private = ${private}")
        }

        serverPublicKey = getServerRSAPublicKey()
    }

    private fun getServerRSAPublicKey(): RSAPublicKey {
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
            .generatePublic(X509EncodedKeySpec(Base64.decode(key, Base64.DEFAULT))) as RSAPublicKey
    }
}
