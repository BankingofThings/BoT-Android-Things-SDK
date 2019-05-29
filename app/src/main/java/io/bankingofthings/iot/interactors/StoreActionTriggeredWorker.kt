package io.bankingofthings.iot.interactors

import io.bankingofthings.iot.storage.SpHelper
import io.reactivex.Completable
import java.util.*

/**
 * Store the action trigger time.
 */
class StoreActionTriggeredWorker(private val spHelper: SpHelper) {
    fun execute(actionID: String): Completable {
        return Completable.fromCallable {
            spHelper.storeActionLastExecutionTime(actionID, Calendar.getInstance().timeInMillis)
        }
    }
}
