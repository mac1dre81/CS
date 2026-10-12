package com.credstow.app.data.local

import androidx.room.migration.Migration

/**
 * Central registry of Room schema migrations for the CredStow database.
 *
 * Schema history (see also app/schemas/ for the JSON source-of-truth files):
 *
 *  v1 — Initial schema. Single `vault_keys` table:
 *       id, provider_id, label, notes, mask_preview, nonce,
 *       cipher_text, key_version, status, created_at, updated_at.
 *
 * ---
 *
 * When the schema changes in a future release:
 *
 *  1. Bump [KeyDatabase]'s `version` in the @Database annotation.
 *  2. Add a `Migration(FROM, TO)` instance to [ALL] here, containing the
 *     exact ALTER / CREATE / DROP SQL. Keep statements idempotent where
 *     possible (e.g. `ALTER TABLE ... ADD COLUMN` fails on re-run — guard
 *     with a try/catch on a column-existence probe, or use `ADD COLUMN IF
 *     NOT EXISTS` on SQLite 3.35+; check minSdk before relying on it).
 *  3. Build once (`./gradlew :app:compileReleaseJavaCompile`) so Room
 *     regenerates the schema JSON under app/schemas/<db>/<TO>.json.
 *  4. Commit both the migration code and the new JSON file.
 *
 * The build will FAIL if a schema JSON for the new version is missing or
 * if a column referenced by @Entity is absent — that is intentional: it
 * catches migration gaps at compile time rather than in users' pockets.
 */
object DatabaseMigrations {

    /** The highest schema version currently known to this codebase. */
    const val CURRENT_VERSION = 2

    /**
     * Ordered list of forward migrations registered with Room.
     */
    val ALL: List<Migration> = listOf(
        // --- v1 → v2: add Pro feature columns ---
        // expiry_date: optional epoch-ms after which a key is considered expired.
        // last_used_at: epoch-ms of the most recent copy/test (for last-used sort).
        // Both are nullable so existing rows get NULL (semantically "no expiry",
        // "never used") — no data transformation required.
        Migration(1, 2) { db ->
            db.execSQL("ALTER TABLE vault_keys ADD COLUMN expiry_date INTEGER")
            db.execSQL("ALTER TABLE vault_keys ADD COLUMN last_used_at INTEGER")
        },
    )
}

