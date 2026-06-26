package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Ingredient::class, ShoppingItem::class, SavedRecipe::class, RecommendedRecipe::class],
    version = 6,
    exportSchema = false
)
abstract class ShelfLifeDatabase : RoomDatabase() {
    abstract val dao: ShelfLifeDao

    companion object {
        @Volatile
        private var INSTANCE: ShelfLifeDatabase? = null

        fun getDatabase(context: Context): ShelfLifeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShelfLifeDatabase::class.java,
                    "shelflife_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ingredients ADD COLUMN userId TEXT NOT NULL DEFAULT 'legacy_local_user'")
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN userId TEXT NOT NULL DEFAULT 'legacy_local_user'")
                db.execSQL(
                    """
                    CREATE TABLE saved_recipes_new (
                        id TEXT NOT NULL,
                        userId TEXT NOT NULL DEFAULT 'legacy_local_user',
                        name TEXT NOT NULL,
                        prepTime TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        imageResUrl TEXT NOT NULL,
                        whySuggested TEXT NOT NULL,
                        ingredientsCsv TEXT NOT NULL,
                        stepsCsv TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        PRIMARY KEY(id, userId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO saved_recipes_new (
                        id, userId, name, prepTime, difficulty, imageResUrl,
                        whySuggested, ingredientsCsv, stepsCsv, timestamp
                    )
                    SELECT id, 'legacy_local_user', name, prepTime, difficulty, imageResUrl,
                        whySuggested, ingredientsCsv, stepsCsv, timestamp
                    FROM saved_recipes
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE saved_recipes")
                db.execSQL("ALTER TABLE saved_recipes_new RENAME TO saved_recipes")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_recipes ADD COLUMN imageProvider TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saved_recipes ADD COLUMN photographerName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saved_recipes ADD COLUMN photographerUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saved_recipes ADD COLUMN photoPageUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saved_recipes ADD COLUMN ingredientsJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saved_recipes ADD COLUMN stepsJson TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ingredients ADD COLUMN dateType TEXT NOT NULL DEFAULT 'Expiration Date'")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN itemStatus TEXT NOT NULL DEFAULT 'Sealed'")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN openedDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN storageCondition TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN brand TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN barcode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN packageSize TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN store TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN price REAL DEFAULT NULL")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recommended_recipes (
                        id TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        prepTime TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        imageResUrl TEXT NOT NULL,
                        whySuggested TEXT NOT NULL,
                        ingredientsCsv TEXT NOT NULL,
                        stepsCsv TEXT NOT NULL,
                        imageProvider TEXT NOT NULL,
                        photographerName TEXT NOT NULL,
                        photographerUrl TEXT NOT NULL,
                        photoPageUrl TEXT NOT NULL,
                        ingredientsJson TEXT NOT NULL,
                        stepsJson TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        PRIMARY KEY(id, userId)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN sourceRecipeNamesJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saved_recipes ADD COLUMN origin TEXT NOT NULL DEFAULT 'AI'")
                db.execSQL("ALTER TABLE saved_recipes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE saved_recipes ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saved_recipes ADD COLUMN baseServings INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE saved_recipes ADD COLUMN cookTime TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saved_recipes ADD COLUMN localImageUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saved_recipes ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE saved_recipes ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE saved_recipes SET createdAt = timestamp, updatedAt = timestamp")
            }
        }
    }
}
