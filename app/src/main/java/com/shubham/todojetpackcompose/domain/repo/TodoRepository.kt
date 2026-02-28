package com.shubham.todojetpackcompose.domain.repo

import com.shubham.todojetpackcompose.common.Utils
import com.shubham.todojetpackcompose.domain.models.TodoItem
import kotlinx.coroutines.flow.Flow

interface TodoRepository {

    suspend fun getTodoList(): Utils.Either<Exception, Flow<List<TodoItem>>>

    suspend fun addTodoItem(todoItem: TodoItem): Utils.Either<Exception, Flow<Long>>

    suspend fun deleteTodoItem(todoId: Long): Utils.Either<Exception, Flow<Int>>

    suspend fun updateTodoItem(todoItem: TodoItem): Utils.Either<Exception, Flow<Int>>
}