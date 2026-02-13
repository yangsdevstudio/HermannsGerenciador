package com.hermanns.hermannsgerenciador.salesman.util

import android.content.Context
import android.content.SharedPreferences

object SalesmanPrefs {
    private const val PREFS_NAME = "SalesmanTrackerPrefs"
    private const val KEY_SESSION_ACTIVE = "session_active"
    private const val KEY_TRACKING_ENABLED = "tracking_enabled"
    private const val KEY_LAST_SNAPSHOT_TIME = "last_snapshot_time"
    private const val KEY_SALESMAN_ID = "salesman_id"
    private const val KEY_VEHICLE_PLATE = "vehicle_plate"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setSessionActive(context: Context, active: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SESSION_ACTIVE, active).apply()
    }

    fun isSessionActive(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SESSION_ACTIVE, false)
    }

    fun setTrackingEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TRACKING_ENABLED, enabled).apply()
    }

    fun isTrackingEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TRACKING_ENABLED, false)
    }

    fun updateLastSnapshotTime(context: Context) {
        getPrefs(context).edit().putLong(KEY_LAST_SNAPSHOT_TIME, System.currentTimeMillis()).apply()
    }

    fun getLastSnapshotTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SNAPSHOT_TIME, 0L)
    }

    fun setSalesmanId(context: Context, salesmanId: String) {
        getPrefs(context).edit().putString(KEY_SALESMAN_ID, salesmanId).apply()
    }

    fun getSalesmanId(context: Context): String? {
        return getPrefs(context).getString(KEY_SALESMAN_ID, null)
    }

    fun setVehiclePlate(context: Context, vehiclePlate: String) {
        getPrefs(context).edit().putString(KEY_VEHICLE_PLATE, vehiclePlate).apply()
    }

    fun getVehiclePlate(context: Context): String? {
        return getPrefs(context).getString(KEY_VEHICLE_PLATE, null)
    }

    fun clearSession(context: Context) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_SESSION_ACTIVE, false)
            putBoolean(KEY_TRACKING_ENABLED, false)
            remove(KEY_SALESMAN_ID)
            remove(KEY_VEHICLE_PLATE)
        }.apply()
    }
}
