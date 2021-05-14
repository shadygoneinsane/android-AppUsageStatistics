/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.appusagestatistics

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.android.appusagestatistics.databinding.FragmentAppUsageStatisticsBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.android.synthetic.main.fragment_app_usage_statistics.*
import java.util.*

/**
 * Fragment that demonstrates how to use App Usage Statistics API.
 */
class AppUsageStatisticsFragment : Fragment() {
    private lateinit var viewBinding: FragmentAppUsageStatisticsBinding

    //VisibleForTesting for variables below
    private var mUsageStatsManager: UsageStatsManager? = null
    private var mUsageListAdapter: UsageListAdapter? = null
    private val FLAGS = PackageManager.GET_META_DATA or
            PackageManager.GET_SHARED_LIBRARY_FILES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mUsageStatsManager =
            activity?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_app_usage_statistics, container, false)
        viewBinding = DataBindingUtil.bind(view)!!
        viewBinding.lifecycleOwner = this

        return view
    }

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(rootView, savedInstanceState)
        mUsageListAdapter = UsageListAdapter()
        viewBinding.recyclerviewAppUsage.scrollToPosition(0)
        viewBinding.recyclerviewAppUsage.adapter = mUsageListAdapter
        viewBinding.loading.visibility = View.VISIBLE

        val mSpinner = rootView.findViewById<Spinner>(R.id.spinner_time_span)
        val spinnerAdapter: SpinnerAdapter = ArrayAdapter.createFromResource(
            requireActivity(),
            R.array.action_list, android.R.layout.simple_spinner_dropdown_item
        )
        mSpinner.adapter = spinnerAdapter
        mSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            var strings = resources.getStringArray(R.array.action_list)
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View,
                position: Int, id: Long
            ) {
                StatsUsageInterval.getValue(strings[position])?.let { usageInterval ->
                    getUsageStatistics(usageInterval.mInterval)?.let { usageStatsList ->
                        Collections.sort(usageStatsList, LastTimeLaunchedComparatorDesc())
                        updateAppsList(usageStatsList)
                        chartDetails(viewBinding.pieChart, usageStatsList)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        viewBinding.pieChart.setUsePercentValues(true)
    }

    /**
     * Returns the [.mRecyclerView] including the time span specified by the
     * intervalType argument.
     *
     * @param intervalType The time interval by which the stats are aggregated.
     * Corresponding to the value of [UsageStatsManager].
     * E.g. [UsageStatsManager.INTERVAL_DAILY], [UsageStatsManager.INTERVAL_WEEKLY],
     * @return A list of [android.app.usage.UsageStats].
     */
    private fun getUsageStatistics(intervalType: Int): MutableList<UsageStats>? {
        // Get the app statistics since one year ago from the current time.
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -1)
        val queryUsageStats = mUsageStatsManager?.queryUsageStats(
            intervalType, cal.timeInMillis,
            System.currentTimeMillis()
        )
        if (queryUsageStats?.size == 0) {
            Log.i(TAG, "The user may not allow the access to apps usage. ")
            Toast.makeText(
                activity,
                getString(R.string.explanation_access_to_appusage_is_not_enabled),
                Toast.LENGTH_LONG
            ).show()
            button_open_usage_setting.visibility = View.VISIBLE
            button_open_usage_setting.setOnClickListener { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        }
        return queryUsageStats
    }

    /**
     * Updates the [.mRecyclerView] with the list of [UsageStats] passed as an argument.
     *
     * @param usageStatsList A list of [UsageStats] from which update the
     * [.mRecyclerView].
     */
    //VisibleForTesting
    private fun updateAppsList(usageStatsList: List<UsageStats>) {
        val customUsageStatsList: MutableList<CustomUsageStats> = ArrayList()

        for (i in usageStatsList.indices) {
            val customUsageStats = CustomUsageStats()
            customUsageStats.usageStats = usageStatsList[i]
            try {
                customUsageStats.appIcon =
                    activity?.packageManager?.getApplicationIcon(customUsageStats.usageStats?.packageName!!)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(
                    TAG, String.format(
                        "App Icon is not found for %s",
                        customUsageStats.usageStats?.packageName
                    )
                )
                customUsageStats.appIcon = activity?.getDrawable(R.drawable.ic_default_app_launcher)
            }

            try {
                activity?.packageManager?.getApplicationInfo(
                    customUsageStats.usageStats?.packageName!!,
                    FLAGS
                )?.let { mApp ->
                    if (mApp.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                        // 1. Applications downloaded from Google Play Store
                        customUsageStatsList.add(customUsageStats)
                    }
                    if (mApp.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) {
                        // 2. Applications preloaded in device by manufacturer
                        customUsageStatsList.add(customUsageStats)
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(
                    TAG, String.format(
                        "App Info is not found for %s",
                        customUsageStats.usageStats?.packageName
                    )
                )
                customUsageStatsList.add(customUsageStats)
            }
        }
        mUsageListAdapter?.setCustomUsageStatsList(customUsageStatsList)
        mUsageListAdapter?.notifyDataSetChanged()
        viewBinding.recyclerviewAppUsage.scrollToPosition(0)
        loading.visibility = View.GONE
    }

    /**
     * The [Comparator] to sort a collection of [UsageStats] sorted by the timestamp
     * last time the app was used in the descendant order.
     */
    private class LastTimeLaunchedComparatorDesc : Comparator<UsageStats> {
        override fun compare(left: UsageStats, right: UsageStats): Int {
            return right.lastTimeUsed.compareTo(left.lastTimeUsed)
        }
    }

    private class MostTimeUsedComparatorDesc : Comparator<UsageStats> {
        override fun compare(left: UsageStats, right: UsageStats): Int {
            return right.totalTimeInForeground.compareTo(left.totalTimeInForeground)
        }
    }

    private fun chartDetails(mChart: PieChart, usageStatsList: List<UsageStats>) {
        Collections.sort(usageStatsList, MostTimeUsedComparatorDesc())

        val pieEntries: ArrayList<PieEntry> = ArrayList()
        val label = "type"
        //initializing data
        val appUsageMap: MutableMap<String, Int> = HashMap()

        val packageManager = context?.applicationContext?.packageManager!!
        var appName = Utils.getAppName(packageManager, usageStatsList[0].packageName)
        appUsageMap[appName] = 200

        appName = Utils.getAppName(packageManager, usageStatsList[1].packageName)
        appUsageMap[appName] = 230

        appName = Utils.getAppName(packageManager, usageStatsList[2].packageName)
        appUsageMap[appName] = 100

        appName = Utils.getAppName(packageManager, usageStatsList[3].packageName)
        appUsageMap[appName] = 500

        //initializing colors for the entries
        val colors: ArrayList<Int> = ArrayList()
        colors.add(Color.parseColor("#304567"))
        colors.add(Color.parseColor("#309967"))
        colors.add(Color.parseColor("#476567"))
        colors.add(Color.parseColor("#890567"))
        colors.add(Color.parseColor("#a35567"))
        colors.add(Color.parseColor("#ff5f67"))
        colors.add(Color.parseColor("#3ca567"))

        //input data and fit data into pie chart entry
        for (type in appUsageMap.keys) {
            pieEntries.add(PieEntry(appUsageMap[type]!!.toFloat(), type))
        }

        //collecting the entries with label name
        val pieDataSet = PieDataSet(pieEntries, label)
        //setting text size of the value
        pieDataSet.valueTextSize = 12f
        //providing color list for coloring different entries
        pieDataSet.colors = colors
        //grouping the data set from entry to chart
        val pieData = PieData(pieDataSet)
        //showing the value of the entries, default true if not set
        pieData.setDrawValues(true)

        mChart.holeRadius = 85f
        mChart.setHoleColor(Color.TRANSPARENT)
        pieData.setValueTextColor(Color.WHITE)
        pieDataSet.valueLinePart1OffsetPercentage = 90f
        pieDataSet.valueLinePart1Length = 0.2f
        pieDataSet.valueLinePart2Length = 0.1f
        pieDataSet.valueTextColor = Color.WHITE
        pieDataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        mChart.setTransparentCircleAlpha(0)
        mChart.setEntryLabelColor(Color.WHITE)

        mChart.data = pieData
        mChart.invalidate()
    }

    companion object {
        private val TAG = AppUsageStatisticsFragment::class.java.simpleName

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment [AppUsageStatisticsFragment].
         */
        @JvmStatic
        fun newInstance(): AppUsageStatisticsFragment {
            return AppUsageStatisticsFragment()
        }
    }
}