package io.bankingofthings.iot.interactors

import io.bankingofthings.iot.storage.SpHelper
import io.bankingofthings.iot.utils.ActionUtil
import io.reactivex.Completable

/**
 * queueID is also used as identifier for storage.
 */
class StoreOfflineTriggeredActionWorker(private val spHelper: SpHelper) {
    fun execute(actionID: String, queueID: String, alternativeID: String?): Completable {
        return Completable.fromCallable {
            spHelper.storeOfflineAction(ActionUtil.createToJson(actionID, queueID, alternativeID))
        }
    }
}
