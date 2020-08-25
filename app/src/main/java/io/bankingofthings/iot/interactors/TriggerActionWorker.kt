package io.bankingofthings.iot.interactors

import com.google.gson.Gson
import io.bankingofthings.iot.BuildConfig
import io.bankingofthings.iot.error.ActionTriggerFailedError
import io.bankingofthings.iot.network.ApiHelper
import io.bankingofthings.iot.network.pojo.ResponseStatus
import io.bankingofthings.iot.network.pojo.TriggerActionAlternativeIDParamPojo
import io.bankingofthings.iot.network.pojo.TriggerActionParamPojo
import io.bankingofthings.iot.repo.IdRepo
import io.bankingofthings.iot.repo.KeyRepo
import io.bankingofthings.iot.utils.JWTUtil
import io.jsonwebtoken.Jwts
import io.reactivex.Completable

/**
 * Trigger action at CORE
 * 1. Create pojo and sign with private RSA key
 * 2. Do API call
 * 3. Check resopnse signing is valid
 * 4. Parse object
 * 5. Check result
 */
class TriggerActionWorker(
    private val apiHelper: ApiHelper,
    private val keyRepo: KeyRepo,
    private val idRepo: IdRepo
) {
    /**
     * alternativeID is only necessary if in BuildConfig.MULTI_PAIR = true
     */
    @Throws(ActionTriggerFailedError::class)
    fun execute(actionID: String, queueID: String, alternativeID: String? = null): Completable {
        System.out.println("TriggerActionWorker:execute actionID = ${actionID} ${alternativeID}")
        val token = JWTUtil.create(
            // With multi pair alternativeID should not be null
            if (alternativeID != null) {
                TriggerActionAlternativeIDParamPojo(
                    TriggerActionAlternativeIDParamPojo.Bot(
                        idRepo.deviceID!!,
                        actionID,
                        alternativeID,
                        queueID
                    )
                )
            } else {
                TriggerActionParamPojo(
                    TriggerActionParamPojo.Bot(
                        idRepo.deviceID!!,
                        actionID,
                        queueID
                    )
                )
            },
            keyRepo.privateKey
        )

        return apiHelper
            .triggerAction(idRepo.makerID, idRepo.deviceID!!, token)
            .map {
                Jwts
                    .parser()
                    .setSigningKey(keyRepo.serverPublicKey)
                    .parseClaimsJws(it.string())
                    .body
            }
            .map { it.get("bot", String::class.java) }
            .map { Gson().fromJson(it, ResponseStatus::class.java) }
            .flatMapCompletable {
                if (it.isOK()) {
                    Completable.complete()
                } else {
                    Completable.error(ActionTriggerFailedError())
                }
            }
    }
}
