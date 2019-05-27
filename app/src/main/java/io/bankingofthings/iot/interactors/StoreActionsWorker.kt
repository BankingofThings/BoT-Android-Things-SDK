package io.bankingofthings.iot.interactors

import io.bankingofthings.iot.model.domain.ActionModel
import io.bankingofthings.iot.storage.SpHelper
import io.reactivex.Completable

/**
 * Store the action frequency on shared preferences. It's used for checking action trigger interval time.
 */
class StoreActionsWorker(private val spHelper: SpHelper) {
    fun execute(actionModels: List<ActionModel>): Completable {
        return Completable.fromCallable {
            actionModels.forEach {
                if (it.actionID != null && it.frequency != null) {
                    spHelper.setActionFrequency(it.actionID, it.frequency)
                } else {
                    System.out.println("StoreActionsWorker:execute corrupt action ${it.actionID} - ${it.frequency}")
                }
            }
        }
    }
}
