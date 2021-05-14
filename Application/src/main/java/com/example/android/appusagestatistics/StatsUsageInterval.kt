package com.example.android.appusagestatistics

import android.app.usage.UsageStatsManager

/**
 * File Description
 * Created by: Vikesh Dass
 * Created on: 14-05-2021
 * Email : vikesh.dass@nagarro.com
 * Company : Adventovate
 */
/**
 * Enum represents the intervals for [android.app.usage.UsageStatsManager] so that
 * values for intervals can be found by a String representation.
 */
enum class StatsUsageInterval(
    private val mStringRepresentation: String,
    val mInterval: Int
) {
    DAILY("Daily", UsageStatsManager.INTERVAL_DAILY),
    WEEKLY("Weekly", UsageStatsManager.INTERVAL_WEEKLY),
    MONTHLY("Monthly", UsageStatsManager.INTERVAL_MONTHLY),
    YEARLY("Yearly", UsageStatsManager.INTERVAL_YEARLY);

    companion object {
        fun getValue(stringRepresentation: String): StatsUsageInterval? {
            for (statsUsageInterval in values()) {
                if (statsUsageInterval.mStringRepresentation == stringRepresentation) {
                    return statsUsageInterval
                }
            }
            return null
        }
    }

}