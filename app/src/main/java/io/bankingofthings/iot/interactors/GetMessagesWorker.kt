package io.bankingofthings.iot.interactors

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.bankingofthings.iot.error.ActionTriggerFailedError
import io.bankingofthings.iot.network.ApiHelper
import io.bankingofthings.iot.network.pojo.MessagePojo
import io.bankingofthings.iot.repo.IdRepo
import io.bankingofthings.iot.repo.KeyRepo
import io.jsonwebtoken.Jwts
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.lang.Exception

/**
 * Trigger action at CORE
 * 1. Create pojo and sign with private RSA key
 * 2. Do API call
 * 3. Check resopnse signing is valid
 * 4. Parse object
 * 5. Check result
 *
 * "{
 * deviceID:f7ef26d1-611f-4004-bef0-546b72cb77cd,
 * payload:{actionID:57EF5243-436E-42BA-BE5B-9616BAC8E4B6,
 *          customerID:B5124E90-A9A3-4CC5-9DA0-D12D91B802EA,
 *          deviceID:f7ef26d1-611f-4004-bef0-546b72cb77cd},
 * messageID:C4443ED3-49DB-43AE-940C-5F5FAF3AB5AE,
 * event:Action Deactivated,
 * deliverd:0}"
 */
class GetMessagesWorker(
    private val apiHelper: ApiHelper,
    private val keyRepo: KeyRepo,
    private val idRepo: IdRepo
) {
    /**
     * alternativeID is only necessary if in BuildConfig.MULTI_PAIR = true
     */
    @Throws(ActionTriggerFailedError::class)
    fun execute(): Single<List<MessagePojo>> {
        return apiHelper
            .getMessages(idRepo.makerID!!, idRepo.deviceID!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                Jwts
                    .parser()
                    .setSigningKey(keyRepo.serverPublicKey)
                    .parseClaimsJws(it.string())
                    .body
            }
            .map { it.get("bot", String::class.java) }
            .map {
                try {
                    val list = Gson().fromJson<List<MessagePojo>>(
                        it, object : TypeToken<List<MessagePojo>>() {}.type
                    )

                    // MessagePojo constructor init not called when creating from Gson. So do it explicit here
                    list.forEach { messagePojo ->
                        messagePojo.payloadModel = Gson().fromJson(
                            messagePojo.payload,
                            MessagePojo.PayloadModel::class.java
                        )
                    }

                    list
                } catch (e: Exception) {
                    try {
                        val messagePojo = Gson().fromJson(it, MessagePojo::class.java)
                        // MessagePojo constructor init not called when creating from Gson. So do it explicit here
                        messagePojo.payloadModel = Gson().fromJson(
                            messagePojo.payload,
                            MessagePojo.PayloadModel::class.java
                        )
                        listOf(messagePojo)
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                        listOf<MessagePojo>()
                    }

                }
            }
    }
}
