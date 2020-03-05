package io.bankingofthings.iot.interactors

import io.bankingofthings.iot.network.ApiHelper
import io.bankingofthings.iot.repo.IdRepo
import io.bankingofthings.iot.repo.KeyRepo
import io.jsonwebtoken.Jwts
import io.reactivex.Single
import org.json.JSONObject

/**
 * Gets status from CORE and checks if device is paired
 * 1. Do API call
 * 2. Check response signing is valid
 * 3. Parse bot object
 * 4. Check result
 */
class CheckDevicePairedWorker(
    private val apiHelper: ApiHelper,
    private val keyRepo: KeyRepo,
    private val idRepo: IdRepo
) {
    fun execute(): Single<Boolean> {
        return apiHelper.checkDeviceIsPaired(idRepo.makerID, idRepo.deviceID!!)
            .map {
                Jwts
                    .parser()
                    .setSigningKey(keyRepo.serverPublicKey)
                    .parseClaimsJws(it.string())
                    .body
            }
            .map { it.get("bot", String::class.java) }
            .map { JSONObject(it) }
            .map { it.optBoolean("status", false) }
    }
}
