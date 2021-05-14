package com.example.android.appusagestatistics

import android.content.pm.PackageManager

/**
 * File Description
 * Created by: Vikesh Dass
 * Created on: 14-05-2021
 * Email : vikesh.dass@nagarro.com
 */
object Utils {

    fun getAppName(packageManager: PackageManager, packageName: String): String {
        return try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.GET_META_DATA
                )
            ) as String
        } catch (ex: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}