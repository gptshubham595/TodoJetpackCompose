package com.shubham.todojetpackcompose.domain.usecases

import com.shubham.todojetpackcompose.common.Utils.Either
import com.shubham.todojetpackcompose.domain.models.TodoItem
import com.shubham.todojetpackcompose.domain.repo.TodoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTodoListUseCase @Inject constructor(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(): Either<Exception, Flow<List<TodoItem>>> =
        repository.getTodoList()
}

class AddTodoItemUseCase @Inject constructor(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(todoItem: TodoItem): Either<Exception, Flow<Long>> =
        repository.addTodoItem(todoItem)
}

class DeleteTodoItemUseCase @Inject constructor(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(todoId: Long): Either<Exception, Flow<Int>> =
        repository.deleteTodoItem(todoId)
}

class UpdateTodoItemUseCase @Inject constructor(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(todoItem: TodoItem): Either<Exception, Flow<Int>> =
        repository.updateTodoItem(todoItem)
}