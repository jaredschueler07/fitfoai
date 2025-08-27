package com.runningcoach.v2.data.repository

import android.util.Log
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.entity.UserEntity
import com.runningcoach.v2.domain.model.FitnessLevel
import com.runningcoach.v2.domain.model.RunningGoal
import com.runningcoach.v2.presentation.screen.profile.ProfileData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class UserRepository(
    private val database: FITFOAIDatabase
) {
    companion object {
        private const val TAG = "UserRepository"
    }
    
    private val userDao = database.userDao()
    
    suspend fun saveUserProfile(profileData: ProfileData): Result<Long> {
        return try {
            // Check if user already exists
            val existingUser = userDao.getCurrentUser().first()
            
            val userEntity = if (existingUser != null) {
                // Update existing user
                existingUser.copy(
                    name = profileData.name,
                    age = profileData.age,
                    height = parseHeight(profileData.height),
                    weight = parseWeight(profileData.weight),
                    experienceLevel = profileData.fitnessLevel.name,
                    runningGoals = profileData.runningGoals.map { it.name },
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                // Create new user
                UserEntity(
                    name = profileData.name,
                    age = profileData.age,
                    height = parseHeight(profileData.height),
                    weight = parseWeight(profileData.weight),
                    experienceLevel = profileData.fitnessLevel.name,
                    runningGoals = profileData.runningGoals.map { it.name },
                    selectedCoach = "bennett" // Default coach
                )
            }
            
            val userId = if (existingUser != null) {
                userDao.updateUser(userEntity)
                userEntity.id
            } else {
                userDao.insertUser(userEntity)
            }
            
            Log.i(TAG, "Successfully saved user profile for user ID: $userId")
            Result.success(userId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user profile", e)
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentUser(): UserEntity? {
        return try {
            userDao.getCurrentUser().first()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current user", e)
            null
        }
    }
    
    fun getCurrentUserFlow(): Flow<UserEntity?> {
        return userDao.getCurrentUser()
    }
    
    suspend fun updateUserCoach(userId: Long, coachId: String): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(
                    selectedCoach = coachId,
                    updatedAt = System.currentTimeMillis()
                )
                userDao.updateUser(updatedUser)
                Log.i(TAG, "Updated user coach to: $coachId")
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user coach", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateUserFromGoogleFit(userId: Long, weight: Float?, height: Float?): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                var shouldUpdate = false
                var updatedUser: UserEntity = user
                
                weight?.let { newWeight ->
                    if (updatedUser.weight != newWeight) {
                        updatedUser = updatedUser.copy(weight = newWeight)
                        shouldUpdate = true
                    }
                }
                
                height?.let { newHeightMeters ->
                    val newHeightCm = (newHeightMeters * 100).toInt()
                    if (updatedUser.height != newHeightCm) {
                        updatedUser = updatedUser.copy(height = newHeightCm)
                        shouldUpdate = true
                    }
                }
                
                if (shouldUpdate) {
                    val finalUser = updatedUser.copy(updatedAt = System.currentTimeMillis())
                    userDao.updateUser(finalUser)
                    Log.i(TAG, "Updated user profile from Google Fit data")
                }
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user from Google Fit", e)
            Result.failure(e)
        }
    }
    
    private fun parseHeight(heightString: String): Int {
        return try {
            // Assume height is in cm, parse as integer
            val cleanHeight = heightString.replace(Regex("[^0-9.]"), "")
            cleanHeight.toFloat().toInt()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse height: $heightString, using default", e)
            170 // Default height in cm
        }
    }
    
    private fun parseWeight(weightString: String): Float {
        return try {
            // Assume weight is in kg, parse as float
            val cleanWeight = weightString.replace(Regex("[^0-9.]"), "")
            cleanWeight.toFloat()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse weight: $weightString, using default", e)
            70.0f // Default weight in kg
        }
    }
    
    suspend fun deleteUser(userId: Long): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                userDao.deleteUser(user)
                Log.i(TAG, "Deleted user: $userId")
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete user", e)
            Result.failure(e)
        }
    }
}
