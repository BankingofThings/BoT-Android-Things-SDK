package io.bankingofthings.iot.interactors

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.bankingofthings.iot.model.domain.ActionModel
import io.bankingofthings.iot.network.ApiHelper
import io.bankingofthings.iot.repo.IdRepo
import io.bankingofthings.iot.repo.KeyRepo
import io.jsonwebtoken.Jwts
import io.reactivex.Single

/**
 * Fetches list of all actions from CORE
 * 1. Do API call
 * 2. Check response signing is valid
 * 3. Parse bot object
 * 4. Convert pojo to Domain Model (list of action models)
 */
class GetActionsWorker(private val apiHelper: ApiHelper, private val keyRepo: KeyRepo, private val idRepo: IdRepo) {
    /**
     * @return list of action models
     */
    fun execute(): Single<List<ActionModel>> {
        return apiHelper.getActions(idRepo.makerID, idRepo.deviceID!!)
            .map {
                Jwts
                    .parser()
                    .setSigningKey(keyRepo.serverPublicKey)
                    .parseClaimsJws(it.string())
                    .body
            }
            .map { it.get("bot", String::class.java) }
            .map {
                if (it.isNotEmpty()) {
                    Gson().fromJson<List<ActionModel>>(it, object : TypeToken<List<ActionModel>>() {}.type)
                } else {
                    listOf()
                }
            }
            .doAfterSuccess {
                it.forEach { System.out.println("GetActionsWorker:execute $it") }
            }
    }

}
