package com.shubham.todojetpackcompose.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.shubham.todojetpackcompose.common.PENDING
import com.shubham.todojetpackcompose.common.TODO_STATUS
import com.shubham.todojetpackcompose.common.TODO_TABLE_NAME
import com.shubham.todojetpackcompose.common.TODO_TASK
import com.shubham.todojetpackcompose.common.Utils

@Entity(tableName = TODO_TABLE_NAME)
data class TodoItemEntity(
    @PrimaryKey
    @SerializedName("id")
    var id: Long,

    @ColumnInfo(name = TODO_TASK)
    @SerializedName("title")
    var task: String,

    @ColumnInfo(name = TODO_STATUS, defaultValue = PENDING)
    var status: String = Utils.TodoStatus.PENDING.name
)


