package io.bankingofthings.iot.utils

import com.google.gson.Gson
import org.json.JSONObject
import java.util.*

/**
 * Created by Ercan Bozoglu on 2019-05-20
 * Copyright @ 2018 BankingOfThings.io. All Right reserved.
 *
 * Helper class for actions
 */
object ActionUtil {

    /**
     * Checks if action last time of execution is passed with the frequency time.
     */
    fun checkFrequencyTimePassed(lastTimeExecutedTime: Long, frequency: String): Boolean {
        val lastTimeExecutedCalendar =
            Calendar.getInstance().apply { timeInMillis = lastTimeExecutedTime }

        return when (frequency) {
            "always" -> true
            "minutely" -> lastTimeExecutedCalendar.apply { add(Calendar.MINUTE, 1) }.before(Calendar.getInstance())
            "hourly" -> lastTimeExecutedCalendar.apply { add(Calendar.HOUR, 1) }.before(Calendar.getInstance())
            "daily" -> lastTimeExecutedCalendar.apply { add(Calendar.DATE, 1) }.before(Calendar.getInstance())
            "weekly" -> lastTimeExecutedCalendar.apply { add(Calendar.YEAR, 1) }.before(Calendar.getInstance())
            "monthly" -> lastTimeExecutedCalendar.apply { add(Calendar.MONTH, 1) }.before(Calendar.getInstance())
            "half_yearly" -> lastTimeExecutedCalendar.apply { add(Calendar.MONTH, 6) }.before(Calendar.getInstance())
            "yearly" -> lastTimeExecutedCalendar.apply { add(Calendar.YEAR, 1) }.before(Calendar.getInstance())
            else -> false
        }
    }

    fun createToJson(actionID: String, queueID: String, alternativeID: String?): String {
        return Gson().toJson(
            JSONObject()
                .put("actionID", actionID)
                .put("queueID", queueID)
                .put("alternativeID", alternativeID)
        )
    }

    fun createFromJson(json: String): Triple<String, String, String> {
        return Gson().fromJson(json, JSONObject::class.java).let {
            Triple(
                it.optString("actionID"),
                it.optString("queueID"),
                it.optString("alternativeID")
            )
        }
    }

}
