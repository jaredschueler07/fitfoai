# Database Migration Template

## Migration: Version X to Version Y

**Date**: [YYYY-MM-DD]  
**Author**: [Developer Name]  
**JIRA/Issue**: [Ticket Number]  
**Review Status**: [ ] Pending / [ ] Approved / [ ] Rejected

---

## Overview

### Purpose
Brief description of why this migration is needed and what it accomplishes.

### Changes Summary
- [ ] New tables added
- [ ] Existing tables modified
- [ ] Tables dropped
- [ ] Indices added/removed
- [ ] Data transformation required
- [ ] Performance impact expected

---

## Technical Details

### Database Version
- **From**: Version X
- **To**: Version Y  
- **Migration Class**: `MIGRATION_X_Y`

### Schema Changes

#### New Tables
```sql
-- Example:
CREATE TABLE IF NOT EXISTS `new_table` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `name` TEXT NOT NULL,
    `created_at` INTEGER NOT NULL
);
```

#### Modified Tables
```sql
-- Example:
ALTER TABLE `existing_table` ADD COLUMN `new_column` TEXT DEFAULT '';
```

#### Dropped Tables
```sql
-- Example:
DROP TABLE IF EXISTS `obsolete_table`;
```

#### Index Changes
```sql
-- New indices
CREATE INDEX IF NOT EXISTS `index_table_column` ON `table_name` (`column_name`);

-- Dropped indices  
DROP INDEX IF EXISTS `old_index_name`;
```

### Data Transformation

#### Required Data Migrations
Describe any data that needs to be transformed, migrated, or populated:

```sql
-- Example: Populate new column with default values
UPDATE existing_table SET new_column = 'default_value' WHERE new_column IS NULL;
```

#### Data Validation Queries
Provide queries to validate data integrity after migration:

```sql
-- Check for null values where they shouldn't exist
SELECT COUNT(*) FROM table_name WHERE required_column IS NULL;

-- Validate foreign key relationships
SELECT COUNT(*) FROM child_table c 
LEFT JOIN parent_table p ON c.parent_id = p.id 
WHERE p.id IS NULL;
```

---

## Implementation

### Migration Code
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Migration SQL statements
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `new_table` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `name` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL
            )
        """)
        
        // Add indices for performance
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_new_table_name` ON `new_table` (`name`)")
    }
}
```

### Entity Changes
List all entity classes that are added, modified, or removed:

#### New Entities
- `NewEntity.kt` - [Purpose and key fields]

#### Modified Entities  
- `ExistingEntity.kt` - [What changed and why]

#### Removed Entities
- `ObsoleteEntity.kt` - [Why it's no longer needed]

---

## Testing Strategy

### Unit Tests
- [ ] Migration executes without errors
- [ ] Schema validation passes
- [ ] Data integrity maintained
- [ ] Performance within acceptable limits

### Integration Tests
- [ ] Full app functionality after migration
- [ ] All CRUD operations work correctly
- [ ] No data loss during migration

### Test Cases
1. **Migration Success Test**
   - Create database at version X with sample data
   - Run migration to version Y
   - Verify all data is preserved and accessible

2. **Data Integrity Test**  
   - Validate all foreign key relationships
   - Check for duplicate data
   - Ensure no null values in required fields

3. **Performance Test**
   - Measure migration execution time
   - Verify query performance post-migration
   - Check database size impact

---

## Risk Assessment

### Migration Risks
- **High**: [List high-risk changes that could cause data loss]
- **Medium**: [List changes that might cause performance issues]  
- **Low**: [List minor changes with minimal impact]

### Mitigation Strategies
1. **Data Backup**: Ensure full database backup before migration
2. **Rollback Plan**: Document steps to revert changes if needed
3. **Staged Deployment**: Test in development → staging → production
4. **Monitoring**: Set up alerts for migration failures

### Performance Impact
- **Estimated Migration Time**: [X seconds/minutes for typical database size]
- **Database Size Change**: [Expected increase/decrease in MB]
- **Query Performance**: [Expected impact on common queries]

---

## Deployment Plan

### Pre-Deployment Checklist
- [ ] Code review completed and approved
- [ ] All tests passing in CI/CD pipeline
- [ ] Database backup verified and accessible
- [ ] Rollback procedure documented and tested
- [ ] Monitoring and alerting configured
- [ ] Stakeholders notified of deployment window

### Deployment Steps
1. **Development Environment**
   - [ ] Deploy and test migration
   - [ ] Validate functionality
   - [ ] Performance testing

2. **Staging Environment**
   - [ ] Deploy with production-like data volume
   - [ ] Full regression testing
   - [ ] Load testing if applicable

3. **Production Environment**
   - [ ] Schedule maintenance window if needed
   - [ ] Execute migration during low-traffic period
   - [ ] Monitor application health post-deployment

### Post-Deployment Validation
- [ ] Migration completed successfully
- [ ] No errors in application logs
- [ ] All critical user flows functional
- [ ] Database performance metrics normal
- [ ] No user-reported issues

---

## Rollback Procedure

### When to Rollback
- Migration fails or times out
- Data corruption detected
- Critical application functionality broken
- Performance degradation beyond acceptable limits

### Rollback Steps
**⚠️ IMPORTANT**: Room does not support automatic database downgrades. Rollback requires manual intervention.

1. **Immediate Actions**
   ```bash
   # Stop application/service to prevent further writes
   # Restore database from pre-migration backup
   # Deploy previous application version
   ```

2. **Manual Rollback Process**
   ```sql
   -- Example rollback SQL (customize per migration)
   DROP TABLE IF EXISTS `new_table_from_migration`;
   ALTER TABLE `modified_table` DROP COLUMN `new_column`;
   ```

3. **Data Recovery**
   - Restore from backup if data transformation occurred
   - Manually recreate any lost data if backup unavailable
   - Document any data that cannot be recovered

### Rollback Testing
- [ ] Rollback procedure tested in development environment
- [ ] Backup restoration process validated
- [ ] Application functionality verified after rollback

---

## Monitoring and Alerts

### Key Metrics to Monitor
- Migration execution time
- Database connection pool usage
- Application error rates
- User-facing functionality performance
- Database query performance

### Alert Conditions
- Migration takes longer than X minutes
- Error rate exceeds normal threshold
- Database connections exhausted
- Critical queries timing out

### Logging
Ensure the following events are logged:
- Migration start/completion timestamps
- Any errors or warnings during migration
- Performance metrics (execution time, rows affected)
- Data validation results

---

## Documentation Updates

### Files to Update
- [ ] `FITFOAIDatabase.kt` - Add migration to migrations list
- [ ] `README.md` - Update database version information
- [ ] API documentation - If schema changes affect API responses
- [ ] User guides - If changes affect user-facing functionality

### Communication Plan
- [ ] Development team notified of changes
- [ ] QA team provided with testing checklist
- [ ] Product team informed of any user-facing impacts
- [ ] Support team briefed on potential user issues

---

## Approval

### Technical Review
- [ ] **Database Admin**: [Name] - [Date] - [Approved/Rejected]
- [ ] **Lead Developer**: [Name] - [Date] - [Approved/Rejected]  
- [ ] **DevOps Engineer**: [Name] - [Date] - [Approved/Rejected]

### Business Review (if applicable)
- [ ] **Product Manager**: [Name] - [Date] - [Approved/Rejected]
- [ ] **QA Lead**: [Name] - [Date] - [Approved/Rejected]

### Final Approval
- [ ] **Technical Lead**: [Name] - [Date] - [Approved/Rejected]

**Deployment Authorization**: [Name] - [Date]

---

## Post-Migration Notes

### Actual Results
*[To be filled after deployment]*

- **Migration Duration**: [X minutes/seconds]
- **Issues Encountered**: [None/List any issues]
- **Performance Impact**: [As expected/Better than expected/Worse than expected]
- **Rollback Required**: [Yes/No] - [If yes, document reason and process]

### Lessons Learned
*[Document any insights gained during this migration for future reference]*

1. [Lesson 1]
2. [Lesson 2]
3. [Additional observations]

### Follow-up Actions
- [ ] Monitor performance for 24-48 hours post-deployment
- [ ] Update documentation with actual migration duration
- [ ] Share lessons learned with team
- [ ] Schedule post-deployment review meeting

---

**Migration Template Version**: 1.0  
**Last Updated**: [Date]  
**Next Review**: [Date + 6 months]