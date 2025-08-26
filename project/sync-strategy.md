# 🔄 RunningCoach App - Sync Strategy & Cloud Integration

## 📋 Overview

This document outlines the comprehensive data synchronization strategy, cloud integration approach, and backup systems for the RunningCoach app. It covers local-first architecture with optional cloud sync, data consistency, and user privacy protection.

## 🏗️ Sync Architecture Overview

### Local-First Design
```
┌─────────────────────────────────────────────────────────────┐
│                    Local Device                             │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │   Room DB   │ │ SharedPrefs │ │ File System │          │
│  │ (Primary)   │ │ (Settings)  │ │ (Assets)    │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Sync (Optional)
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Cloud Storage                            │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │   Firebase  │ │ Google Drive│ │ iCloud      │          │
│  │   Firestore │ │ (Backup)    │ │ (Backup)    │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

### Sync Principles
- **Local-First**: All data stored locally by default
- **Optional Sync**: Cloud sync is opt-in only
- **Privacy-First**: No data collection without explicit consent
- **Offline-First**: App works completely offline
- **Conflict Resolution**: Smart conflict detection and resolution

## 💾 Data Storage Strategy

### Local Storage Layers

#### Primary Database (Room)
```kotlin
@Database(
    entities = [
        User::class,
        TrainingPlan::class,
        Run::class,
        Coach::class,
        CoachingEvent::class
    ],
    version = 1
)
abstract class RunningDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun planDao(): PlanDao
    abstract fun runDao(): RunDao
    abstract fun coachDao(): CoachDao
    abstract fun coachingEventDao(): CoachingEventDao
}
```

#### Settings Storage (SharedPreferences)
```kotlin
object UserPreferences {
    private const val PREF_NAME = "running_coach_prefs"
    
    // User Settings
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_NAME = "user_name"
    const val KEY_PREFERRED_UNITS = "preferred_units"
    const val KEY_COACH_PERSONALITY = "coach_personality"
    
    // Sync Settings
    const val KEY_SYNC_ENABLED = "sync_enabled"
    const val KEY_LAST_SYNC_TIME = "last_sync_time"
    const val KEY_SYNC_PROVIDER = "sync_provider"
    
    // App Settings
    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val KEY_AUDIO_ENABLED = "audio_enabled"
}
```

#### File System Storage
```kotlin
object FileStorage {
    private const val APP_DIRECTORY = "RunningCoach"
    
    // Asset Storage
    const val ASSETS_DIR = "assets"
    const val COACH_AVATARS_DIR = "coaches/avatars"
    const val BACKGROUNDS_DIR = "backgrounds"
    const val ILLUSTRATIONS_DIR = "illustrations"
    
    // Export Storage
    const val EXPORTS_DIR = "exports"
    const val BACKUPS_DIR = "backups"
    const val LOGS_DIR = "logs"
}
```

### Cloud Storage Options

#### Firebase Firestore (Primary)
```kotlin
interface FirebaseSyncService {
    suspend fun syncUserData(userId: String): SyncResult
    suspend fun syncTrainingPlans(userId: String): SyncResult
    suspend fun syncRuns(userId: String): SyncResult
    suspend fun syncSettings(userId: String): SyncResult
    suspend fun backupData(userId: String): BackupResult
    suspend fun restoreData(userId: String, backupId: String): RestoreResult
}
```

#### Google Drive (Backup)
```kotlin
interface GoogleDriveSyncService {
    suspend fun createBackup(userId: String): BackupResult
    suspend fun restoreFromBackup(userId: String, backupId: String): RestoreResult
    suspend fun listBackups(userId: String): List<BackupInfo>
    suspend fun deleteBackup(userId: String, backupId: String): Boolean
}
```

#### iCloud (iOS Backup)
```kotlin
interface ICloudSyncService {
    suspend fun syncToICloud(userId: String): SyncResult
    suspend fun syncFromICloud(userId: String): SyncResult
    suspend fun isICloudAvailable(): Boolean
    suspend fun getICloudUsage(): StorageUsage
}
```

## 🔄 Sync Implementation

### Sync Manager
```kotlin
class SyncManager(
    private val localDatabase: RunningDatabase,
    private val firebaseService: FirebaseSyncService,
    private val googleDriveService: GoogleDriveSyncService,
    private val iCloudService: ICloudSyncService,
    private val preferences: SharedPreferences
) {
    
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    suspend fun performFullSync(userId: String): SyncResult {
        return try {
            _syncState.value = SyncState.Syncing
            
            // Sync user data
            val userResult = firebaseService.syncUserData(userId)
            if (userResult is SyncResult.Error) return userResult
            
            // Sync training plans
            val plansResult = firebaseService.syncTrainingPlans(userId)
            if (plansResult is SyncResult.Error) return plansResult
            
            // Sync runs
            val runsResult = firebaseService.syncRuns(userId)
            if (runsResult is SyncResult.Error) return runsResult
            
            // Sync settings
            val settingsResult = firebaseService.syncSettings(userId)
            if (settingsResult is SyncResult.Error) return settingsResult
            
            // Update last sync time
            preferences.edit().putLong(UserPreferences.KEY_LAST_SYNC_TIME, System.currentTimeMillis()).apply()
            
            _syncState.value = SyncState.Synced
            SyncResult.Success
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            SyncResult.Error(e)
        }
    }
    
    suspend fun performIncrementalSync(userId: String): SyncResult {
        val lastSyncTime = preferences.getLong(UserPreferences.KEY_LAST_SYNC_TIME, 0L)
        return performSyncSince(userId, lastSyncTime)
    }
    
    private suspend fun performSyncSince(userId: String, since: Long): SyncResult {
        // Implementation for incremental sync
        return SyncResult.Success
    }
}
```

### Sync States
```kotlin
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Synced : SyncState()
    data class Error(val message: String) : SyncState()
}

sealed class SyncResult {
    object Success : SyncResult()
    data class Error(val exception: Exception) : SyncResult()
    data class PartialSuccess(val syncedItems: Int, val failedItems: Int) : SyncResult()
}
```

### Data Models for Sync

#### Sync Metadata
```kotlin
@Entity(tableName = "sync_metadata")
data class SyncMetadata(
    @PrimaryKey val id: String,
    val userId: String,
    val entityType: String,
    val entityId: String,
    val lastModified: Long,
    val syncStatus: SyncStatus,
    val cloudVersion: String?,
    val localVersion: String,
    val conflictResolution: ConflictResolution?
)

enum class SyncStatus {
    SYNCED, PENDING_SYNC, SYNCING, CONFLICT, ERROR
}

enum class ConflictResolution {
    LOCAL_WINS, CLOUD_WINS, MANUAL_RESOLUTION, MERGE
}
```

#### Backup Information
```kotlin
data class BackupInfo(
    val id: String,
    val userId: String,
    val timestamp: Long,
    val size: Long,
    val description: String,
    val provider: BackupProvider,
    val status: BackupStatus,
    val metadata: Map<String, Any>
)

enum class BackupProvider {
    FIREBASE, GOOGLE_DRIVE, ICLOUD, LOCAL
}

enum class BackupStatus {
    CREATING, COMPLETED, FAILED, RESTORING
}
```

## 🔐 Security & Privacy

### Data Encryption
```kotlin
object DataEncryption {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    
    suspend fun encryptData(data: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = generateIV()
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        
        val encryptedData = cipher.doFinal(data)
        return iv + encryptedData
    }
    
    suspend fun decryptData(encryptedData: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = encryptedData.sliceArray(0..11)
        val data = encryptedData.sliceArray(12 until encryptedData.size)
        
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(data)
    }
    
    private fun generateIV(): ByteArray {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        return iv
    }
}
```

### Key Management
```kotlin
class SecureKeyManager(private val context: Context) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore")
    private val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    
    init {
        keyStore.load(null)
    }
    
    fun generateKey(alias: String): SecretKey {
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    fun getKey(alias: String): SecretKey? {
        return try {
            keyStore.getKey(alias, null) as? SecretKey
        } catch (e: Exception) {
            null
        }
    }
}
```

### Privacy Controls
```kotlin
object PrivacyManager {
    private const val PRIVACY_PREFS = "privacy_preferences"
    
    // Privacy Settings
    const val KEY_DATA_COLLECTION = "data_collection_enabled"
    const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
    const val KEY_CRASH_REPORTING = "crash_reporting_enabled"
    const val KEY_CLOUD_SYNC = "cloud_sync_enabled"
    const val KEY_DATA_SHARING = "data_sharing_enabled"
    
    fun isDataCollectionEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PRIVACY_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DATA_COLLECTION, false)
    }
    
    fun setDataCollectionEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PRIVACY_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DATA_COLLECTION, enabled)
            .apply()
    }
    
    fun isCloudSyncEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PRIVACY_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_CLOUD_SYNC, false)
    }
    
    fun setCloudSyncEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PRIVACY_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CLOUD_SYNC, enabled)
            .apply()
    }
}
```

## 🔄 Conflict Resolution

### Conflict Detection
```kotlin
class ConflictDetector {
    fun detectConflicts(localData: SyncableData, cloudData: SyncableData): List<Conflict> {
        val conflicts = mutableListOf<Conflict>()
        
        if (localData.lastModified > cloudData.lastModified) {
            conflicts.add(Conflict(
                entityId = localData.id,
                entityType = localData.type,
                localVersion = localData.version,
                cloudVersion = cloudData.version,
                conflictType = ConflictType.LOCAL_NEWER
            ))
        } else if (cloudData.lastModified > localData.lastModified) {
            conflicts.add(Conflict(
                entityId = localData.id,
                entityType = localData.type,
                localVersion = localData.version,
                cloudVersion = cloudData.version,
                conflictType = ConflictType.CLOUD_NEWER
            ))
        }
        
        return conflicts
    }
}

data class Conflict(
    val entityId: String,
    val entityType: String,
    val localVersion: String,
    val cloudVersion: String,
    val conflictType: ConflictType
)

enum class ConflictType {
    LOCAL_NEWER, CLOUD_NEWER, CONCURRENT_MODIFICATION, DATA_DIVERGENCE
}
```

### Conflict Resolution Strategies
```kotlin
class ConflictResolver {
    suspend fun resolveConflict(conflict: Conflict): ConflictResolution {
        return when (conflict.conflictType) {
            ConflictType.LOCAL_NEWER -> ConflictResolution.LOCAL_WINS
            ConflictType.CLOUD_NEWER -> ConflictResolution.CLOUD_WINS
            ConflictType.CONCURRENT_MODIFICATION -> resolveConcurrentModification(conflict)
            ConflictType.DATA_DIVERGENCE -> ConflictResolution.MANUAL_RESOLUTION
        }
    }
    
    private suspend fun resolveConcurrentModification(conflict: Conflict): ConflictResolution {
        // Implement smart conflict resolution based on data type
        return when (conflict.entityType) {
            "Run" -> resolveRunConflict(conflict)
            "TrainingPlan" -> resolvePlanConflict(conflict)
            "User" -> ConflictResolution.MANUAL_RESOLUTION
            else -> ConflictResolution.LOCAL_WINS
        }
    }
    
    private suspend fun resolveRunConflict(conflict: Conflict): ConflictResolution {
        // For runs, prefer the one with more complete data
        return ConflictResolution.MERGE
    }
    
    private suspend fun resolvePlanConflict(conflict: Conflict): ConflictResolution {
        // For training plans, prefer the most recent version
        return ConflictResolution.CLOUD_WINS
    }
}
```

## 📊 Backup & Restore

### Backup Manager
```kotlin
class BackupManager(
    private val database: RunningDatabase,
    private val fileStorage: FileStorage,
    private val encryption: DataEncryption,
    private val keyManager: SecureKeyManager
) {
    
    suspend fun createBackup(userId: String, description: String): BackupResult {
        return try {
            val backupId = generateBackupId()
            val timestamp = System.currentTimeMillis()
            
            // Export database
            val databaseBackup = exportDatabase(userId)
            
            // Export settings
            val settingsBackup = exportSettings(userId)
            
            // Export assets
            val assetsBackup = exportAssets(userId)
            
            // Create backup package
            val backupPackage = createBackupPackage(
                backupId, userId, timestamp, description,
                databaseBackup, settingsBackup, assetsBackup
            )
            
            // Encrypt backup
            val key = keyManager.generateKey("backup_$backupId")
            val encryptedBackup = encryption.encryptData(backupPackage, key)
            
            // Save backup
            val backupFile = saveBackup(backupId, encryptedBackup)
            
            // Create backup metadata
            val backupInfo = BackupInfo(
                id = backupId,
                userId = userId,
                timestamp = timestamp,
                size = backupFile.length(),
                description = description,
                provider = BackupProvider.LOCAL,
                status = BackupStatus.COMPLETED,
                metadata = mapOf(
                    "databaseVersion" to database.openHelper.readableDatabase.version,
                    "appVersion" to BuildConfig.VERSION_NAME,
                    "deviceModel" to Build.MODEL
                )
            )
            
            saveBackupMetadata(backupInfo)
            
            BackupResult.Success(backupInfo)
        } catch (e: Exception) {
            BackupResult.Error(e)
        }
    }
    
    suspend fun restoreBackup(backupId: String): RestoreResult {
        return try {
            // Load backup metadata
            val backupInfo = loadBackupMetadata(backupId)
            
            // Load encrypted backup
            val encryptedBackup = loadBackup(backupId)
            
            // Decrypt backup
            val key = keyManager.getKey("backup_$backupId")
            val decryptedBackup = encryption.decryptData(encryptedBackup, key)
            
            // Extract backup package
            val backupPackage = extractBackupPackage(decryptedBackup)
            
            // Validate backup
            validateBackup(backupPackage)
            
            // Restore data
            restoreDatabase(backupPackage.databaseBackup)
            restoreSettings(backupPackage.settingsBackup)
            restoreAssets(backupPackage.assetsBackup)
            
            RestoreResult.Success(backupInfo)
        } catch (e: Exception) {
            RestoreResult.Error(e)
        }
    }
    
    private suspend fun exportDatabase(userId: String): ByteArray {
        // Export Room database to SQL dump
        return database.openHelper.readableDatabase.path?.let { dbPath ->
            File(dbPath).readBytes()
        } ?: ByteArray(0)
    }
    
    private suspend fun exportSettings(userId: String): ByteArray {
        // Export SharedPreferences
        return Json.encodeToString(getAllPreferences()).toByteArray()
    }
    
    private suspend fun exportAssets(userId: String): ByteArray {
        // Export user-specific assets
        return ByteArray(0) // Placeholder
    }
}

sealed class BackupResult {
    data class Success(val backupInfo: BackupInfo) : BackupResult()
    data class Error(val exception: Exception) : BackupResult()
}

sealed class RestoreResult {
    data class Success(val backupInfo: BackupInfo) : RestoreResult()
    data class Error(val exception: Exception) : RestoreResult()
}
```

## 🔄 Sync Scheduling

### Sync Scheduler
```kotlin
class SyncScheduler(
    private val syncManager: SyncManager,
    private val workManager: WorkManager
) {
    
    fun schedulePeriodicSync(userId: String, interval: Duration) {
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            interval.toMillis(), TimeUnit.MILLISECONDS
        )
            .setInputData(workDataOf("userId" to userId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "sync_$userId",
            ExistingPeriodicWorkPolicy.REPLACE,
            syncWorkRequest
        )
    }
    
    fun scheduleImmediateSync(userId: String) {
        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf("userId" to userId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        workManager.enqueue(syncWorkRequest)
    }
    
    fun cancelSync(userId: String) {
        workManager.cancelUniqueWork("sync_$userId")
    }
}

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()
        
        return try {
            val syncManager = SyncManager(/* dependencies */)
            val result = syncManager.performIncrementalSync(userId)
            
            when (result) {
                is SyncResult.Success -> Result.success()
                is SyncResult.Error -> Result.retry()
                is SyncResult.PartialSuccess -> Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

## 📈 Sync Analytics

### Sync Metrics
```kotlin
class SyncAnalytics {
    fun trackSyncEvent(event: SyncEvent) {
        // Track sync events for analytics
        Analytics.track("sync_event", mapOf(
            "event_type" to event.type,
            "user_id" to event.userId,
            "sync_provider" to event.provider,
            "data_size" to event.dataSize,
            "duration" to event.duration,
            "success" to event.success
        ))
    }
    
    fun trackConflictResolution(resolution: ConflictResolution) {
        Analytics.track("conflict_resolution", mapOf(
            "resolution_type" to resolution.name,
            "timestamp" to System.currentTimeMillis()
        ))
    }
    
    fun trackBackupEvent(event: BackupEvent) {
        Analytics.track("backup_event", mapOf(
            "event_type" to event.type,
            "backup_id" to event.backupId,
            "backup_size" to event.size,
            "provider" to event.provider,
            "success" to event.success
        ))
    }
}

data class SyncEvent(
    val type: String,
    val userId: String,
    val provider: String,
    val dataSize: Long,
    val duration: Long,
    val success: Boolean
)

data class BackupEvent(
    val type: String,
    val backupId: String,
    val size: Long,
    val provider: String,
    val success: Boolean
)
```

## 🔧 Configuration

### Sync Configuration
```kotlin
object SyncConfig {
    // Sync intervals
    const val MIN_SYNC_INTERVAL = 15 * 60 * 1000L // 15 minutes
    const val DEFAULT_SYNC_INTERVAL = 60 * 60 * 1000L // 1 hour
    const val MAX_SYNC_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
    
    // Retry configuration
    const val MAX_SYNC_RETRIES = 3
    const val SYNC_RETRY_DELAY = 5 * 60 * 1000L // 5 minutes
    
    // Data limits
    const val MAX_SYNC_DATA_SIZE = 10 * 1024 * 1024L // 10MB
    const val MAX_BACKUP_SIZE = 100 * 1024 * 1024L // 100MB
    
    // Conflict resolution
    const val CONFLICT_RESOLUTION_TIMEOUT = 30 * 1000L // 30 seconds
    const val MAX_CONFLICT_ATTEMPTS = 5
}
```

### Network Configuration
```kotlin
object NetworkConfig {
    // Timeouts
    const val CONNECT_TIMEOUT = 30 * 1000L // 30 seconds
    const val READ_TIMEOUT = 60 * 1000L // 60 seconds
    const val WRITE_TIMEOUT = 60 * 1000L // 60 seconds
    
    // Retry configuration
    const val MAX_RETRIES = 3
    const val RETRY_DELAY = 1000L // 1 second
    
    // Compression
    const val ENABLE_COMPRESSION = true
    const val COMPRESSION_THRESHOLD = 1024L // 1KB
}
```

---

**Document Version**: 1.0  
**Last Updated**: January 2025  
**Next Review**: February 2025
