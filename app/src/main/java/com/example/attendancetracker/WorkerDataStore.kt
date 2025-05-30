package com.example.attendancetracker

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("worker_prefs")

class WorkerDataStore(private val context: Context) {
    private val gson = Gson()
    private val WORKERS_KEY = stringPreferencesKey("workers_json")

    suspend fun saveWorkers(workers: List<Worker>) {
        val json = gson.toJson(workers)
        context.dataStore.edit { prefs ->
            prefs[WORKERS_KEY] = json
        }
    }

    suspend fun loadWorkers(): List<Worker> {
        val prefs = context.dataStore.data
            .map { it[WORKERS_KEY] ?: "[]" }
            .first()

        return gson.fromJson(prefs, Array<Worker>::class.java).toList()
    }
}
