package io.bankingofthings.iot.interactors

import io.bankingofthings.iot.error.ActionFrequencyNotFoundError
import io.bankingofthings.iot.error.ActionFrequencyTimeNotPassedError
import io.bankingofthings.iot.storage.SpHelper
import io.bankingofthings.iot.utils.ActionUtil
import io.reactivex.Completable
import java.util.*

/**
 * Check if actions last execution time is passed.
 */
class CheckActionTriggerableWorker(private val spHelper: SpHelper) {

    @Throws(ActionFrequencyNotFoundError::class, ActionFrequencyTimeNotPassedError::class)
    fun execute(actionID: String): Completable {
        return Completable.fromCallable {
            val frequency = spHelper.getActionFrequency(actionID)

            // Frequency not found
            if (frequency != null) {
                val time = spHelper.getActionLastExecutionTime(actionID)

                System.out.println("CheckActionTriggerableWorker:execute time = ${time}")
                // Time not passed
                if (time != -1L && !ActionUtil.checkFrequencyTimePassed(time, frequency)) {
                    throw ActionFrequencyTimeNotPassedError("$frequency ${(Calendar.getInstance().timeInMillis - time) / 1000} seconds elapsed")
                }

                // No errors, completed
            }
        }
    }
}
