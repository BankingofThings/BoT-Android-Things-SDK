package io.bankingofthings.iot.interactors

import io.bankingofthings.iot.model.domain.ActionModel
import io.bankingofthings.iot.storage.SpHelper
import io.reactivex.Completable

class StoreActionsWorker(private val spHelper: SpHelper) {
    fun execute(actionModels: List<ActionModel>): Completable {
        return Completable.fromCallable {
            actionModels.forEach {
                System.out.println("StoreActionsWorker:execute ${it.actionName} ${it.frequency}")
                if (it.actionID != null && it.frequency != null) {
                    spHelper.setActionFrequency(it.actionID, it.frequency)
                } else {
                    System.out.println("StoreActionsWorker:execute corrupt action ${it.actionID} - ${it.frequency}")
                }
            }
        }
    }
}
