package com.shubham.todojetpackcompose.data.mapper

import com.shubham.todojetpackcompose.data.models.TodoItemEntity
import com.shubham.todojetpackcompose.domain.models.TodoItem

fun TodoItemEntity?.toDomain(): TodoItem {
    return TodoItem(
        id = this?.id ?: 0,
        task = this?.task ?: "",
        status = this?.status ?: ""
    )
}

fun TodoItem.toData(): TodoItemEntity {
    return TodoItemEntity(
        id = this.id,
        task = this.task,
        status = this.status
    )
}