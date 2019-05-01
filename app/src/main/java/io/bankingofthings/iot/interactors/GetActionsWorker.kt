package io.bankingofthings.iot.interactors

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.bankingofthings.iot.model.domain.ActionModel
import io.bankingofthings.iot.network.ApiHelper
import io.bankingofthings.iot.repo.IdRepo
import io.bankingofthings.iot.repo.KeyRepo
import io.jsonwebtoken.Jwts
import io.reactivex.Single

class GetActionsWorker(private val apiHelper: ApiHelper, private val keyRepo: KeyRepo, private val idRepo: IdRepo) {
    fun execute(): Single<List<ActionModel>> {
        return apiHelper.getActions(idRepo.makerID, idRepo.deviceID)
            .map {
                Jwts
                    .parser()
                    .setSigningKey(keyRepo.serverPublicKey)
                    .parseClaimsJws(it.string())
                    .body
            }
            .map { it.get("bot", String::class.java) }
            .map { Gson().fromJson<List<ActionModel>>(it, object : TypeToken<List<ActionModel>>() {}.type) }
    }

}
