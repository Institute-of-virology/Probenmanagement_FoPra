# Standard Operating Procedure for Database Migrations with Flyway

This document outlines the standard operating procedure for managing database schema and data changes using Flyway in this project. Following these guidelines ensures consistent, version-controlled, and reliable database evolution.

## Key Principles

*   **Version Control:** All database changes are managed through versioned SQL scripts.
*   **Automated Migration:** Flyway automatically applies pending migrations on application startup.
*   **Schema-first Approach:** Schema changes are typically handled before data changes.
*   **Idempotency:** Migration scripts should ideally be idempotent, meaning they can be run multiple times without causing errors (though Flyway tracks applied migrations to prevent re-execution).

## Procedure

### 1. Initial Setup and Baseline Migration (First Project Setup)

When setting up the project for the first time, or integrating Flyway into an existing database, you need to create a baseline migration. This captures the current state of your database schema.

**Steps:**

1.  **Ensure Flyway is configured:**
    *   Verify `flyway-core` and `flyway-database-postgresql` dependencies are in `pom.xml`.
    *   Ensure `FlywayInitializer.java` (or similar) is configured to point to `classpath:db/migration`.
    *   Remove `spring.jpa.hibernate.ddl-auto` property from `application.properties` to prevent conflicts.
    *   Remove `spring.sql.init.data-locations` and `spring.sql.init.mode` if they point to `data.sql`, as data population will now be handled by Flyway.

2.  **Generate `V1__baseline.sql` from your existing database schema:**
    *   If your PostgreSQL database is running in a Docker container, first find its name or ID:
        ```bash
        docker ps
        ```
    *   Then, execute `pg_dump` inside the container to export the schema. Replace `<container_name_or_id>` and `sample_management` with your actual values:
        ```bash
        docker exec <container_name_or_id> pg_dump -s -U postgres sample_management > V1__baseline.sql
        ```
    *   If PostgreSQL is running directly on your host, use:
        ```bash
        pg_dump -s -h localhost -p 5432 -U postgres sample_management > V1__baseline.sql
        ```
        (Adjust host, port, and username as necessary).

3.  **Move the generated `V1__baseline.sql` file:**
    Place the generated file into the migration directory:
    ```bash
    mv V1__baseline.sql src/main/resources/db/migration/
    ```

4.  **Clean up `data.sql` (if applicable):**
    If you previously used `data.sql` for initial data, move its content to a new Flyway migration script (e.g., `V3__populate_data.sql`) and then delete the original `data.sql` file.

### 2. Creating New Migration Files (Schema Changes)

For any new schema changes (e.g., adding a table, altering a column, adding an index), you must create a new Flyway migration script.

**Steps:**

1.  **Determine the next version number:** Flyway migrations are ordered by version. Look at the existing files in `src/main/resources/db/migration/` (e.g., `V1__baseline.sql`, `V2__add_analysis_origin_to_analysis_type.sql`). If the last version is `V2`, your new migration will be `V3`.
2.  **Create a new SQL file:** In the `src/main/resources/db/migration/` directory, create a new file named `V<version>__<description>.sql`.
    *   **`V<version>`:** Use the next sequential version number (e.g., `V3`, `V4`).
    *   **`__<description>`:** A descriptive name for the change, using underscores instead of spaces (e.g., `__add_users_table`, `__alter_email_column`).
    *   **Example:** `V3__add_new_feature_table.sql`
3.  **Write your SQL statements:** Inside the new `.sql` file, write the SQL commands to perform your schema change.
    *   **Example (V3__add_new_feature_table.sql):**
        ```sql
        CREATE TABLE public.new_feature (
            id BIGINT NOT NULL,
            name VARCHAR(255) NOT NULL,
            description TEXT,
            PRIMARY KEY (id)
        );

        CREATE SEQUENCE public.new_feature_seq
            START WITH 1
            INCREMENT BY 50
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        ```
4.  **Do NOT include rollback scripts directly:** Flyway does not use explicit rollback scripts in the same way Liquibase does. Rollbacks are typically handled by reverting to a previous version in your version control system and then performing a `flyway:repair` or `flyway:baseline` if necessary (use with caution).

### 3. Creating New Migration Files (Data Changes)

For initial data population or significant data modifications that need to be version-controlled, create a new migration script.

**Steps:**

1.  **Determine the next version number:** Similar to schema changes, use the next sequential version number.
2.  **Create a new SQL file:** In the `src/main/resources/db/migration/` directory, create a new file named `V<version>__<description>.sql`.
    *   **Example:** `V4__insert_initial_users.sql`
3.  **Write your SQL `INSERT`, `UPDATE`, or `DELETE` statements:**
    *   **Example (V4__insert_initial_users.sql):**
        ```sql
        INSERT INTO public.users (id, email, enabled, username)
        VALUES (1, 'admin@example.com', TRUE, 'admin') ON CONFLICT (id) DO NOTHING;
        ```
    *   Consider using `ON CONFLICT DO NOTHING` or similar clauses for idempotent data insertions if the script might be run against a database that already contains some of the data.

### 4. Updating the Database

Flyway migrations are automatically applied when the Spring Boot application starts.

**Steps:**

1.  **Start the application:** Simply run your Spring Boot application. Flyway will detect any new migration scripts in `src/main/resources/db/migration/` and apply them in version order.
2.  **Verify:** Check your application logs for Flyway output, which will indicate if migrations were applied successfully. You can also connect to your database and inspect the `flyway_schema_history` table to see the applied migrations.

**Manual Migration (Optional):**

If you need to apply migrations without starting the full application (e.g., in a CI/CD pipeline or for development purposes), you can use the Flyway Maven plugin:

```bash
mvn flyway:migrate
```

This command will connect to the database configured in your `application.properties` (or `pom.xml` if explicitly set there, though `application.properties` is preferred for runtime configuration) and apply any pending migrations.

---

By following this SOP, you ensure that all database changes are properly versioned, documented, and applied consistently across all environments.