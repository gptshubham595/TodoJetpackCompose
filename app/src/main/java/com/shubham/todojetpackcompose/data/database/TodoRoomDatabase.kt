package com.shubham.todojetpackcompose.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shubham.todojetpackcompose.data.models.TodoItemEntity


@Database(entities = [TodoItemEntity::class], version = 2, exportSchema = false)
abstract class TodoRoomDatabase : RoomDatabase() {
    abstract fun getTodoDao(): TodoRoomDao
}
