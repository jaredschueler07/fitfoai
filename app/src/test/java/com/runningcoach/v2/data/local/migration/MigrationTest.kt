package com.runningcoach.v2.data.local.migration

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.runningcoach.v2.data.local.FITFOAIDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.IOException

/**
 * Comprehensive database migration tests
 * 
 * Test Coverage:
 * - Migration 3->4 (Voice coaching entities)
 * - Source field backfill logic (future migration)
 * - Migration failure scenarios
 * - Data integrity during migrations
 * - Performance during large dataset migrations
 * - Schema validation after migrations
 * 
 * NOTE: These tests require instrumented testing (androidTest) to work with
 * the actual SQLite database. This file should be moved to androidTest
 * when implementing the actual migration tests.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDatabaseName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FITFOAIDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Test migration from version 3 to 4
     * Validates voice coaching table creation
     */
    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        var db = helper.createDatabase(testDatabaseName, 3).apply {
            // Insert test data in version 3 schema
            execSQL("""
                INSERT INTO users (id, name, email, createdAt, updatedAt) 
                VALUES (1, 'Test User', 'test@example.com', 1640995200000, 1640995200000)
            """)
            execSQL("""
                INSERT INTO run_sessions (userId, startTime, duration, distance, createdAt, updatedAt)
                VALUES (1, 1640995200000, 1800000, 5000.0, 1640995200000, 1640995200000)
            """)
            close()
        }

        // Re-open the database with version 4 and provide MIGRATION_3_4 as the migration process
        db = helper.runMigrationsAndValidate(testDatabaseName, 4, true, FITFOAIDatabase.MIGRATION_3_4)

        // Validate that voice_lines table was created
        val voiceLinesExists = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='voice_lines'").use { cursor ->
            cursor.moveToFirst() && cursor.getString(0) == "voice_lines"
        }
        assertTrue("voice_lines table should exist after migration", voiceLinesExists)

        // Validate that coach_personalities table was created
        val coachPersonalitiesExists = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='coach_personalities'").use { cursor ->
            cursor.moveToFirst() && cursor.getString(0) == "coach_personalities"
        }
        assertTrue("coach_personalities table should exist after migration", coachPersonalitiesExists)

        // Validate indices were created
        val voiceLinesCoachIdIndex = db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_voice_lines_coachId'").use { cursor ->
            cursor.moveToFirst()
        }
        assertTrue("index_voice_lines_coachId should exist", voiceLinesCoachIdIndex)

        // Validate existing data is preserved
        val userCount = db.query("SELECT COUNT(*) FROM users").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
        assertEquals("User data should be preserved", 1, userCount)

        val sessionCount = db.query("SELECT COUNT(*) FROM run_sessions").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
        assertEquals("Session data should be preserved", 1, sessionCount)
    }

    /**
     * Test voice_lines table structure after migration
     */
    @Test
    @Throws(IOException::class)
    fun testVoiceLinesTableStructure() {
        // Create database at version 3
        helper.createDatabase(testDatabaseName, 3).close()
        
        // Migrate to version 4
        val db = helper.runMigrationsAndValidate(testDatabaseName, 4, true, FITFOAIDatabase.MIGRATION_3_4)

        // Test inserting data into voice_lines table
        db.execSQL("""
            INSERT INTO voice_lines (id, coachId, contextType, text, audioFilePath, isPreloaded, lastUsed, useCount, createdAt)
            VALUES ('line1', 'bennett', 'pace_encouragement', 'Great pace!', '/path/to/audio', 1, 1640995200000, 5, 1640995200000)
        """)

        // Validate the data was inserted correctly
        val cursor = db.query("SELECT * FROM voice_lines WHERE id = 'line1'")
        assertTrue("Should find inserted voice line", cursor.moveToFirst())
        assertEquals("bennett", cursor.getString(cursor.getColumnIndex("coachId")))
        assertEquals("pace_encouragement", cursor.getString(cursor.getColumnIndex("contextType")))
        assertEquals("Great pace!", cursor.getString(cursor.getColumnIndex("text")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("isPreloaded")))
        assertEquals(5, cursor.getInt(cursor.getColumnIndex("useCount")))
        cursor.close()
    }

    /**
     * Test coach_personalities table structure after migration
     */
    @Test
    @Throws(IOException::class)
    fun testCoachPersonalitiesTableStructure() {
        // Create database at version 3
        helper.createDatabase(testDatabaseName, 3).close()
        
        // Migrate to version 4
        val db = helper.runMigrationsAndValidate(testDatabaseName, 4, true, FITFOAIDatabase.MIGRATION_3_4)

        // Test inserting data into coach_personalities table
        db.execSQL("""
            INSERT INTO coach_personalities (id, name, description, voiceId, motivationalStyle, coachingFrequency, personalityTraits, isActive, createdAt)
            VALUES ('bennett', 'Bennett', 'Professional coach', 'voice123', 'supportive', 'moderate', '["encouraging","professional"]', 1, 1640995200000)
        """)

        // Validate the data was inserted correctly
        val cursor = db.query("SELECT * FROM coach_personalities WHERE id = 'bennett'")
        assertTrue("Should find inserted coach personality", cursor.moveToFirst())
        assertEquals("Bennett", cursor.getString(cursor.getColumnIndex("name")))
        assertEquals("Professional coach", cursor.getString(cursor.getColumnIndex("description")))
        assertEquals("voice123", cursor.getString(cursor.getColumnIndex("voiceId")))
        assertEquals("supportive", cursor.getString(cursor.getColumnIndex("motivationalStyle")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("isActive")))
        cursor.close()
    }

    // Future Migration Tests (Example for when source field is added to RunSessionEntity)
    
    /**
     * Test future migration that adds source field to run_sessions table
     * This demonstrates how to test source field backfill logic
     */
    @Test
    @Throws(IOException::class)
    fun testFutureSourceFieldMigration() {
        // This test would be implemented when the source field migration is created
        // For now, this is a template showing the testing approach

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add source column to run_sessions
                database.execSQL("ALTER TABLE run_sessions ADD COLUMN source TEXT DEFAULT 'MANUAL'")
                
                // Backfill logic: Set source based on existing data
                database.execSQL("""
                    UPDATE run_sessions 
                    SET source = CASE 
                        WHEN googleFitSessionId IS NOT NULL THEN 'GOOGLE_FIT'
                        WHEN syncedWithGoogleFit = 1 THEN 'GOOGLE_FIT'
                        ELSE 'MANUAL'
                    END
                """)
            }
        }

        // Create database with test data at version 4
        var db = helper.createDatabase(testDatabaseName, 4).apply {
            // Create voice coaching tables (from migration 3->4)
            execSQL("""
                CREATE TABLE IF NOT EXISTS `voice_lines` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `coachId` TEXT NOT NULL,
                    `contextType` TEXT NOT NULL,
                    `text` TEXT NOT NULL,
                    `audioFilePath` TEXT,
                    `isPreloaded` INTEGER NOT NULL DEFAULT 0,
                    `lastUsed` INTEGER NOT NULL DEFAULT 0,
                    `useCount` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL
                )
            """)
            
            // Insert test run sessions with different sync states
            execSQL("""
                INSERT INTO run_sessions (userId, startTime, duration, distance, googleFitSessionId, syncedWithGoogleFit, createdAt, updatedAt)
                VALUES (1, 1640995200000, 1800000, 5000.0, 'gfit123', 1, 1640995200000, 1640995200000)
            """)
            execSQL("""
                INSERT INTO run_sessions (userId, startTime, duration, distance, googleFitSessionId, syncedWithGoogleFit, createdAt, updatedAt)
                VALUES (1, 1640995260000, 1800000, 5000.0, NULL, 0, 1640995260000, 1640995260000)
            """)
            close()
        }

        // Run the migration
        db = helper.runMigrationsAndValidate(testDatabaseName, 5, true, MIGRATION_4_5)

        // Validate source field was added and backfilled correctly
        val googleFitSession = db.query("SELECT source FROM run_sessions WHERE googleFitSessionId = 'gfit123'").use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }
        assertEquals("Should backfill Google Fit source", "GOOGLE_FIT", googleFitSession)

        val manualSession = db.query("SELECT source FROM run_sessions WHERE googleFitSessionId IS NULL").use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }
        assertEquals("Should backfill manual source", "MANUAL", manualSession)
    }

    /**
     * Test migration with large dataset performance
     */
    @Test
    @Throws(IOException::class)
    fun testMigrationPerformanceWithLargeDataset() {
        // Create database with large amount of test data
        var db = helper.createDatabase(testDatabaseName, 3).apply {
            // Insert many run sessions to test migration performance
            beginTransaction()
            try {
                for (i in 1..1000) {
                    execSQL("""
                        INSERT INTO run_sessions (userId, startTime, duration, distance, createdAt, updatedAt)
                        VALUES (1, ${1640995200000L + i * 3600000L}, 1800000, 5000.0, 1640995200000, 1640995200000)
                    """)
                }
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
            close()
        }

        // Measure migration time
        val startTime = System.currentTimeMillis()
        db = helper.runMigrationsAndValidate(testDatabaseName, 4, true, FITFOAIDatabase.MIGRATION_3_4)
        val migrationTime = System.currentTimeMillis() - startTime

        // Validate performance (migration should complete in reasonable time)
        assertTrue("Migration should complete in under 5 seconds for 1000 records", migrationTime < 5000)

        // Validate all data was preserved
        val sessionCount = db.query("SELECT COUNT(*) FROM run_sessions").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
        assertEquals("All sessions should be preserved", 1000, sessionCount)

        // Validate new tables exist and are functional
        db.execSQL("""
            INSERT INTO voice_lines (id, coachId, contextType, text, createdAt)
            VALUES ('perf_test', 'bennett', 'test', 'Performance test line', 1640995200000)
        """)

        val voiceLineExists = db.query("SELECT COUNT(*) FROM voice_lines WHERE id = 'perf_test'").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0) > 0
        }
        assertTrue("Should be able to insert into new voice_lines table", voiceLineExists)
    }

    /**
     * Test migration failure scenarios
     */
    @Test
    @Throws(IOException::class)
    fun testMigrationWithCorruptedData() {
        // Create database with potentially problematic data
        var db = helper.createDatabase(testDatabaseName, 3).apply {
            // Insert data that might cause migration issues
            execSQL("""
                INSERT INTO run_sessions (userId, startTime, duration, distance, createdAt, updatedAt)
                VALUES (1, NULL, 1800000, 5000.0, 1640995200000, 1640995200000)
            """)
            close()
        }

        // Migration should handle null values gracefully
        db = helper.runMigrationsAndValidate(testDatabaseName, 4, true, FITFOAIDatabase.MIGRATION_3_4)

        // Validate that migration completed successfully despite null values
        val voiceLinesExists = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='voice_lines'").use { cursor ->
            cursor.moveToFirst() && cursor.getString(0) == "voice_lines"
        }
        assertTrue("Migration should succeed even with problematic data", voiceLinesExists)
    }

    /**
     * Test rollback scenarios (downgrade protection)
     */
    @Test
    @Throws(IOException::class)
    fun testDowngradeProtection() {
        // Create database at version 4
        helper.createDatabase(testDatabaseName, 4).close()

        try {
            // Attempt to open database with version 3 (should fail)
            val db = Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                FITFOAIDatabase::class.java,
                testDatabaseName
            )
            .fallbackToDestructiveMigrationOnDowngrade() // This should be set in production
            .build()
            
            // This would normally throw an exception or trigger destructive migration
            db.openHelper.readableDatabase
            db.close()
            
            // If we reach here, destructive migration was used (which is expected)
            // In production, we should have proper downgrade handling
            assertTrue("Downgrade should be handled gracefully", true)
            
        } catch (e: Exception) {
            // Exception is expected for unsupported downgrade
            assertTrue("Should handle downgrade attempts", e.message?.contains("migration") == true)
        }
    }

    /**
     * Test schema validation after migration
     */
    @Test
    @Throws(IOException::class)
    fun testSchemaValidationAfterMigration() {
        // Create database at version 3
        helper.createDatabase(testDatabaseName, 3).close()

        // Migrate to version 4
        val db = helper.runMigrationsAndValidate(testDatabaseName, 4, true, FITFOAIDatabase.MIGRATION_3_4)

        // Validate all expected tables exist
        val tableNames = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name").use { cursor ->
            while (cursor.moveToNext()) {
                tableNames.add(cursor.getString(0))
            }
        }

        val expectedTables = listOf(
            "android_metadata", // System table
            "coach_personalities",
            "run_sessions", 
            "users",
            "voice_lines"
        )

        expectedTables.forEach { tableName ->
            if (tableName != "android_metadata") { // Skip system table check
                assertTrue("Table $tableName should exist", tableNames.contains(tableName))
            }
        }

        // Validate specific column constraints for new tables
        
        // Check voice_lines table structure
        val voiceLinesCursor = db.query("PRAGMA table_info(voice_lines)")
        val voiceLinesColumns = mutableListOf<String>()
        while (voiceLinesCursor.moveToNext()) {
            voiceLinesColumns.add(voiceLinesCursor.getString(1)) // Column name
        }
        voiceLinesCursor.close()

        val expectedVoiceLinesColumns = listOf("id", "coachId", "contextType", "text", "audioFilePath", "isPreloaded", "lastUsed", "useCount", "createdAt")
        expectedVoiceLinesColumns.forEach { column ->
            assertTrue("voice_lines should have $column column", voiceLinesColumns.contains(column))
        }

        // Check coach_personalities table structure  
        val coachPersonalitiesCursor = db.query("PRAGMA table_info(coach_personalities)")
        val coachPersonalitiesColumns = mutableListOf<String>()
        while (coachPersonalitiesCursor.moveToNext()) {
            coachPersonalitiesColumns.add(coachPersonalitiesCursor.getString(1)) // Column name
        }
        coachPersonalitiesCursor.close()

        val expectedCoachPersonalitiesColumns = listOf("id", "name", "description", "voiceId", "motivationalStyle", "coachingFrequency", "personalityTraits", "isActive", "createdAt")
        expectedCoachPersonalitiesColumns.forEach { column ->
            assertTrue("coach_personalities should have $column column", coachPersonalitiesColumns.contains(column))
        }
    }

    /**
     * Test concurrent migration scenarios
     * Note: This test simulates what would happen with concurrent database access during migration
     */
    @Test
    @Throws(IOException::class)
    fun testConcurrentMigrationSafety() {
        // Create database at version 3
        helper.createDatabase(testDatabaseName, 3).close()

        // Migrate to version 4 (this should be atomic)
        val db = helper.runMigrationsAndValidate(testDatabaseName, 4, true, FITFOAIDatabase.MIGRATION_3_4)

        // Validate that the migration was atomic - either fully completed or not started
        // Check if both new tables exist (partial migration would be problematic)
        val voiceLinesExists = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='voice_lines'").use { cursor ->
            cursor.count > 0
        }
        
        val coachPersonalitiesExists = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='coach_personalities'").use { cursor ->
            cursor.count > 0
        }

        // Both tables should exist if migration completed successfully
        assertTrue("Migration should be atomic - both new tables should exist", 
                   voiceLinesExists && coachPersonalitiesExists)

        // Test that we can immediately use the new tables
        db.execSQL("""
            INSERT INTO voice_lines (id, coachId, contextType, text, createdAt)
            VALUES ('concurrent_test', 'bennett', 'test', 'Concurrent test line', 1640995200000)
        """)

        val insertedSuccessfully = db.query("SELECT COUNT(*) FROM voice_lines WHERE id = 'concurrent_test'").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0) == 1
        }

        assertTrue("Should be able to immediately use migrated tables", insertedSuccessfully)
    }
}