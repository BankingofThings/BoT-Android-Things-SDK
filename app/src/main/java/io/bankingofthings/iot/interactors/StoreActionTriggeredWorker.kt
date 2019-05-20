package io.bankingofthings.iot.interactors

import io.bankingofthings.iot.storage.SpHelper
import io.reactivex.Completable
import java.util.*

class StoreActionTriggeredWorker(private val spHelper: SpHelper) {
    fun execute(actionID: String): Completable {
        return Completable.fromCallable {
            spHelper.setActionLastExecutionTime(actionID, Calendar.getInstance().timeInMillis)
        }
    }
}
