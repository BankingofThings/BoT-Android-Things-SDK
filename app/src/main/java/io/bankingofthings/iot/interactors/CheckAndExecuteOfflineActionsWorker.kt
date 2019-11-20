package io.bankingofthings.iot.interactors

import io.bankingofthings.iot.error.NoActionsFoundError
import io.bankingofthings.iot.storage.SpHelper
import io.bankingofthings.iot.utils.ActionUtil
import io.reactivex.Completable
import io.reactivex.Single

class CheckAndExecuteOfflineActionsWorker(
    private val spHelper: SpHelper,
    private val triggerActionWorker: TriggerActionWorker
) {
    /**
     * Recursive, trigger one offline action at CORE and then check until none is left.
     */
    fun execute(): Completable {
        return spHelper.getOfflineActions()?.let { offlineActions ->
            Single.just(offlineActions)
                .flatMapCompletable { actions ->
                    Completable
                        .concat(
                            actions.map { action ->
                                ActionUtil.createFromJson(action).let { (actionID, queueID, alternativeID) ->
                                    triggerActionWorker.execute(actionID, queueID, alternativeID)
                                        .onErrorResumeNext {
                                            it.printStackTrace()
                                            Completable.complete()
                                        }
                                }

                            }
                        )
                        .andThen { spHelper.removeOfflineActions() }
                }
        } ?: throw NoActionsFoundError()
    }
}
