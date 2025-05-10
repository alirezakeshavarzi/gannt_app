package com.example.ganttapp

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("tasks_prefs")

object DataStoreManager {
    private val TASKS_KEY = stringPreferencesKey("tasks_key")
    private val gson = Gson()

    suspend fun saveTasks(context: Context, tasks: List<Task>) {
        val json = gson.toJson(tasks)
        context.dataStore.edit { prefs ->
            prefs[TASKS_KEY] = json
        }
    }

    fun getTasksFlow(context: Context): Flow<List<Task>> =
        context.dataStore.data
            .map { prefs ->
                prefs[TASKS_KEY]?.let { json ->
                    val type = object : TypeToken<List<Task>>() {}.type
                    gson.fromJson<List<Task>>(json, type)
                } ?: emptyList()
            }
}
