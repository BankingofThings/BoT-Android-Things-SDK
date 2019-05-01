package io.bankingofthings.iot.interactors

import io.bankingofthings.iot.network.ApiHelper
import io.bankingofthings.iot.network.pojo.ActivateDeviceParamPojo
import io.bankingofthings.iot.network.pojo.TriggerActionParamPojo
import io.bankingofthings.iot.repo.IdRepo
import io.bankingofthings.iot.repo.KeyRepo
import io.bankingofthings.iot.utils.JWTUtil
import io.jsonwebtoken.Jwts
import io.reactivex.Single
import org.json.JSONObject

/**
 * Set status ACTIVATED to CORE
 */
class CheckDeviceActivatedWorker(
    private val apiHelper: ApiHelper,
    private val keyRepo: KeyRepo,
    private val idRepo: IdRepo
) {
    fun execute(): Single<Boolean> {
        val token = JWTUtil.create(
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
            .map { JSONObject(it) }
            .map {
                it.optBoolean("status", false)
                    .apply {
                        System.out.println("CheckDevicePairedWorker:execute $this")
                    }
            }
    }
}
