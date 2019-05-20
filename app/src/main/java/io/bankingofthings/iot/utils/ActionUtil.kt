package io.bankingofthings.iot.utils

import java.util.*

/**
 * Created by Ercan Bozoglu on 2019-05-20
 * Copyright @ 2018 BankingOfThings.io. All Right reserved.
 */
object ActionUtil {
    fun checkFrequencyTime(lastTimeExecutedTime: Long, frequency: String): Boolean {
        val lastTimeExecutedCalendar = Calendar.getInstance().apply { timeInMillis = lastTimeExecutedTime }

        return when (frequency) {
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

}
