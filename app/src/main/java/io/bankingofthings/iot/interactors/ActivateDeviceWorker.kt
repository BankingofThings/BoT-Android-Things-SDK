package io.bankingofthings.iot.interactors

import io.bankingofthings.iot.error.DeviceActivationFailedError
import io.bankingofthings.iot.network.ApiHelper
import io.bankingofthings.iot.network.pojo.ActivateDeviceParamPojo
import io.bankingofthings.iot.repo.IdRepo
import io.bankingofthings.iot.repo.KeyRepo
import io.bankingofthings.iot.utils.JWTUtil
import io.jsonwebtoken.Jwts
import io.reactivex.Completable
import io.reactivex.Single
import org.json.JSONObject

/**
 * Activates device on CORE
 * 1. Create pojo with deviceID and sign with private RSA key
 * 2. Do API call
 * 3. Check response signing is valid
 * 4. Parse bot object
 * 5. Check result
 */
class ActivateDeviceWorker(private val apiHelper: ApiHelper, private val keyRepo: KeyRepo, private val idRepo: IdRepo) {
    @Throws(DeviceActivationFailedError::class)
    fun execute(): Completable {
        val token =
            JWTUtil.create(
                ActivateDeviceParamPojo(ActivateDeviceParamPojo.Bot(idRepo.deviceID)) as Any,
                keyRepo.privateKey
            )

        return apiHelper.activateDevice(idRepo.makerID, idRepo.deviceID, token)
            .map {
                Jwts
                    .parser()
                    .setSigningKey(keyRepo.serverPublicKey)
                    .parseClaimsJws(it.string())
                    .body
            }
            .map { it.get("bot", String::class.java) }
            .flatMapCompletable {
                if (it.isEmpty()) {
                    Completable.complete()
                } else {
                    Completable.error(DeviceActivationFailedError())
                }
            }
    }
}
