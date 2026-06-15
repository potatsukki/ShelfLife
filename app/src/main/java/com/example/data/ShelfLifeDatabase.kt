package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Ingredient::class, ShoppingItem::class, SavedRecipe::class], version = 3, exportSchema = false)
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
    }
}
