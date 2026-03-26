package com.example.safeguard

import android.content.Context
import java.util.UUID

object UserSessionManager {

    private const val PREF_NAME = "safeguard_prefs"
    private const val KEY_USER_ID = "user_id"

    fun getUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var userId = prefs.getString(KEY_USER_ID, null)

        if (userId == null) {
            userId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_USER_ID, userId).apply()
        }

        return userId
    }
}