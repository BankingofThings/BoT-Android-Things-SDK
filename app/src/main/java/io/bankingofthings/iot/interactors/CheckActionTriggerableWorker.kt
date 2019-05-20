package io.bankingofthings.iot.interactors

import io.bankingofthings.iot.storage.SpHelper
import io.bankingofthings.iot.utils.ActionUtil
import io.reactivex.Single

class CheckActionTriggerableWorker(private val spHelper: SpHelper) {
    /**
     * Check if actions last execution time is passed.
     */
    fun execute(actionID: String): Single<Boolean> {
        System.out.println("CheckActionTriggerableWorker:execute actionID = ${actionID}")

        return Single.create {
            val time = spHelper.getActionLastExecutionTime(actionID)
            val frequency = spHelper.getActionFrequency(actionID)

            System.out.println("CheckActionTriggerableWorker:execute time = ${time}")
            System.out.println("CheckActionTriggerableWorker:execute frequency = ${frequency}")

            if (time == -1L || frequency == null) {
                it.onSuccess(true)
            } else if (ActionUtil.checkFrequencyTime(time, frequency)) {
                it.onSuccess(true)
            } else {
                it.onSuccess(false)
            }
        }
    }
}
