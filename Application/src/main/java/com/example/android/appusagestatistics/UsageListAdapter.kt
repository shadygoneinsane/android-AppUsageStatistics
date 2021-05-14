/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.appusagestatistics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Provide views to RecyclerView with the directory entries.
 */
class UsageListAdapter internal constructor() :
    RecyclerView.Adapter<UsageListAdapter.ViewHolder>() {
    private var mCustomUsageStatsList: List<CustomUsageStats> = ArrayList()
    private val mDateFormat: DateFormat = SimpleDateFormat()

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val appName: TextView = v.findViewById(R.id.textview_app_name)
        val packageName: TextView = v.findViewById(R.id.textview_package_name)
        val lastTimeUsed: TextView = v.findViewById(R.id.textview_last_time_used)
        val totalTimeUsed: TextView = v.findViewById(R.id.tv_total_time_used)
        val totalTimeUsedLabel: TextView = v.findViewById(R.id.tv_total_time_used_label)
        val appIcon: ImageView = v.findViewById(R.id.app_icon)

    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.usage_row, viewGroup, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val packageManager = viewHolder.itemView.context.applicationContext.packageManager
        mCustomUsageStatsList[position].usageStats?.packageName?.let { packageName ->
            val appName = Utils.getAppName(packageManager, packageName)
            viewHolder.appName.text = appName
            viewHolder.packageName.text = packageName
        }

        mCustomUsageStatsList[position].usageStats?.lastTimeUsed?.let { lastTimeUsed ->
            viewHolder.lastTimeUsed.text = mDateFormat.format(Date(lastTimeUsed))
        }

        mCustomUsageStatsList[position].usageStats?.totalTimeInForeground?.let { totalUsageMillis ->
            when {
                totalUsageMillis > (60L * 1000L) -> {
                    viewHolder.totalTimeUsedLabel.visibility = View.VISIBLE
                    viewHolder.totalTimeUsed.text = "${totalUsageMillis / (1000L * 60L)}  minutes"
                }
                totalUsageMillis > 100L -> {
                    viewHolder.totalTimeUsedLabel.visibility = View.VISIBLE
                    viewHolder.totalTimeUsed.text = "${totalUsageMillis / 1000L}  secs"
                }
                else -> {
                    viewHolder.totalTimeUsedLabel.visibility = View.GONE
                    viewHolder.totalTimeUsed.visibility = View.VISIBLE
                }
            }
        }
        viewHolder.appIcon.setImageDrawable(mCustomUsageStatsList[position].appIcon)
    }

    override fun getItemCount(): Int {
        return mCustomUsageStatsList.size
    }

    fun setCustomUsageStatsList(customUsageStats: List<CustomUsageStats>) {
        mCustomUsageStatsList = customUsageStats
    }
}