package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Ingredient::class, ShoppingItem::class, SavedRecipe::class], version = 1, exportSchema = false)
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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
