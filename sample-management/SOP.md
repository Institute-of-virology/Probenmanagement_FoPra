# Standard Operating Procedure for Database Schema Changes

This document provides a standard operating procedure for making database schema changes with Liquibase in this project. Following this procedure will ensure that your database remains in a consistent state and that changes can be tracked and rolled back if necessary.

## Procedure

1.  **Create a New Changelog File:**
    *   For each new database change, create a new YAML file in the `src/main/resources/db/changelog/changes/` directory.
    *   Use a descriptive name for the file (e.g., `003_add_new_column.yaml`).

2.  **Write the Changeset:**
    *   In the new file, define your change using Liquibase's YAML syntax.
    *   Each changeset should have a unique `id` and `author`.
    *   **Crucially, always include a `<rollback>` block** so you can undo the change if needed.

3.  **Include the New Changelog:**
    *   Open the master changelog file: `src/main/resources/db/changelog/db.changelog-master.yaml`.
    *   Add an `include` statement for your new file at the end of the file. The order is important.

4.  **Apply the Changes:**
    *   The changes will be applied automatically the next time you start your application.
    *   Alternatively, you can apply the changes manually by running the following command:
        `mvn -P liquibase-migration liquibase:update`

## Changeset Examples

Here are some examples of common database changes:

### Create Table

```yaml
databaseChangeLog:
  - changeSet:
      id: 4
      author: your_name
      changes:
        - createTable:
            tableName: new_table
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: name
                  type: VARCHAR(255)
                  constraints:
                    nullable: false
      rollback:
        - dropTable:
            tableName: new_table
```

### Add Column

```yaml
databaseChangeLog:
  - changeSet:
      id: 5
      author: your_name
      changes:
        - addColumn:
            tableName: existing_table
            columns:
              - column:
                  name: new_column
                  type: VARCHAR(255)
      rollback:
        - dropColumn:
            tableName: existing_table
            columnName: new_column
```

### Drop Table

**Note:** Dropping a table is a destructive operation. Make sure you have a backup of your data before running this changeset.

```yaml
databaseChangeLog:
  - changeSet:
      id: 6
      author: your_name
      changes:
        - dropTable:
            tableName: table_to_drop
      rollback:
        - createTable:
            tableName: table_to_drop
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
```

### Drop Column

```yaml
databaseChangeLog:
  - changeSet:
      id: 7
      author: your_name
      changes:
        - dropColumn:
            tableName: existing_table
            columnName: column_to_drop
      rollback:
        - addColumn:
            tableName: existing_table
            columns:
              - column:
                  name: column_to_drop
                  type: VARCHAR(255)
```

### Rename Table

```yaml
databaseChangeLog:
  - changeSet:
      id: 8
      author: your_name
      changes:
        - renameTable:
            oldTableName: old_table_name
            newTableName: new_table_name
      rollback:
        - renameTable:
            oldTableName: new_table_name
            newTableName: old_table_name
```

### Rename Column

```yaml
databaseChangeLog:
  - changeSet:
      id: 9
      author: your_name
      changes:
        - renameColumn:
            tableName: existing_table
            oldColumnName: old_column_name
            newColumnName: new_column_name
            columnDataType: VARCHAR(255)
      rollback:
        - renameColumn:
            tableName: existing_table
            oldColumnName: new_column_name
            newColumnName: old_column_name
            columnDataType: VARCHAR(255)
```

### Add Foreign Key Constraint

```yaml
databaseChangeLog:
  - changeSet:
      id: 10
      author: your_name
      changes:
        - addForeignKeyConstraint:
            baseTableName: child_table
            baseColumnNames: parent_id
            constraintName: fk_child_table_parent_table
            referencedTableName: parent_table
            referencedColumnNames: id
      rollback:
        - dropForeignKeyConstraint:
            baseTableName: child_table
            constraintName: fk_child_table_parent_table
```

### Create Index

```yaml
databaseChangeLog:
  - changeSet:
      id: 11
      author: your_name
      changes:
        - createIndex:
            indexName: idx_name
            tableName: existing_table
            columns:
              - column:
                  name: name
      rollback:
        - dropIndex:
            indexName: idx_name
            tableName: existing_table
```
