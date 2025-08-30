package com.runningcoach.v2.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.runningcoach.v2.data.local.entity.DataSource
import com.runningcoach.v2.data.local.entity.WorkoutType
import com.runningcoach.v2.data.local.entity.WorkoutStatus

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
    
    @TypeConverter
    fun fromMap(value: Map<String, Any>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toMap(value: String): Map<String, Any> {
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromDataSource(value: DataSource): String {
        return value.name
    }
    
    @TypeConverter
    fun toDataSource(value: String): DataSource {
        return DataSource.valueOf(value)
    }
    
    @TypeConverter
    fun fromWorkoutType(value: WorkoutType): String {
        return value.name
    }
    
    @TypeConverter
    fun toWorkoutType(value: String): WorkoutType {
        return WorkoutType.valueOf(value)
    }
    
    @TypeConverter
    fun fromWorkoutStatus(value: WorkoutStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toWorkoutStatus(value: String): WorkoutStatus {
        return WorkoutStatus.valueOf(value)
    }
}
