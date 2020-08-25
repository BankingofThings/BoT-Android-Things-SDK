package io.bankingofthings.iot.network

import io.bankingofthings.iot.BuildConfig
import okhttp3.CertificatePinner
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import javax.net.ssl.*

/**
 * Manages the TrustManager with the certificates
 */
class TLSManager {
    private val hostNames = listOf(BuildConfig.BOT_HOST)

    /**
     * Initialize after certificate is supplied
     */
    fun setCertificateInputStream(botCertificateInputStream: InputStream) {
        val trustManagerFactory: TrustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())

        // Use the new keystore, with our certificate
        trustManagerFactory.init(
            KeyStore.getInstance(KeyStore.getDefaultType())
                .apply {
                    load(null, null)

                    // Add certificate
                    val cf = CertificateFactory.getInstance("X.509")
                    setCertificateEntry("bot", cf.generateCertificate(botCertificateInputStream))
                }
        )
    }

    fun verifyHostName(hostName: String?, sslSession: SSLSession?): Boolean {
        hostNames.forEach {
            if (hostName == it) {
                return true
            }
        }

        return false
    }

    /**
     * Create pinner with sha256 public key fingerprint
     */
    fun getCertificatePinner(): CertificatePinner {
        return CertificatePinner
            .Builder()
            .add(
                BuildConfig.BOT_HOST,
                "sha256/" + BuildConfig.BOT_FINGERPRINT
            )
            .build()
    }
}
