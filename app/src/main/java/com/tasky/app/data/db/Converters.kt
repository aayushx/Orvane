package com.tasky.app.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }
    private val stringListSerializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null || value.isEmpty()) return "[]"
        return json.encodeToString(stringListSerializer, value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(stringListSerializer, value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/** Date range types used by custom filters. */
object DateRangeType {
    const val ANY = 0
    const val TODAY = 1
    const val NEXT_7_DAYS = 2
    const val OVERDUE = 3
    const val CUSTOM = 4
}
