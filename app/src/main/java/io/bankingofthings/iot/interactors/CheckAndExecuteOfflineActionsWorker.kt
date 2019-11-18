package io.bankingofthings.iot.interactors

import io.bankingofthings.iot.storage.SpHelper
import io.bankingofthings.iot.utils.ActionUtil
import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Single

class CheckAndExecuteOfflineActionsWorker(
    private val spHelper: SpHelper,
    private val triggerActionWorker: TriggerActionWorker
) {
    /**
     * Recursive, trigger one offline action at CORE and then check unitl none is left.
     */
    fun execute(): Completable {
        System.out.println("CheckAndExecuteOfflineActionsWorker:execute")

        return Single.just(spHelper.getActions())
            .flatMapCompletable {
                System.out.println("CheckAndExecuteOfflineActionsWorker:execute it.size = ${it.size}")

                Completable
                    .concat(it.map {
                        ActionUtil.createFromJson(it).let { (actionID, queueID, alternativeID) ->
                            triggerActionWorker.execute(actionID, queueID, alternativeID)
                                .onErrorResumeNext {
                                    it.printStackTrace()
                                    Completable.complete()
                                }
                        }
                    })
                    .andThen { spHelper.removeActions() }
            }
    }
}
