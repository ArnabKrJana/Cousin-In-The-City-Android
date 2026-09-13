package com.oneforth.cousininthecity.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromActionDataMap(map: Map<String, String>?): String? {
        if (map == null) return null
        return gson.toJson(map)
    }

    @TypeConverter
    fun toActionDataMap(json: String?): Map<String, String>? {
        if (json == null) return null
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(json, type)
    }
}

