package com.shubham.todojetpackcompose.data.repo

import android.util.Log
import com.shubham.todojetpackcompose.common.Utils.Either
import com.shubham.todojetpackcompose.data.mapper.toData
import com.shubham.todojetpackcompose.data.mapper.toDomain
import com.shubham.todojetpackcompose.domain.models.TodoItem
import com.shubham.todojetpackcompose.domain.repo.TodoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import javax.inject.Inject

class TodoRepositoryImpl @Inject constructor(
    private val todoDao: TodoDataSource, // IDatabase
) : TodoRepository {
    override suspend fun getTodoList(): Either<Exception, Flow<List<TodoItem>>> {
        return try {
            val dataFlow = flow {
                Log.d("ResponseDB", "${todoDao.fetchAllTodoItems().map { it.toDomain() }}")
                emit(todoDao.fetchAllTodoItems().map { it.toDomain() })
            }.catch { e ->
            }.flowOn(Dispatchers.IO).onCompletion {
                Log.d("ResponseDB", "onCompletion")
            }

            Either.Success(dataFlow)
        } catch (e: Exception) {
            Either.Error(e)
        }
    }

    private suspend fun saveToDB(apiResponse: List<TodoItem>) {
        apiResponse.forEach {
            todoDao.addTodoItem(it.toData())
        }
    }

    override suspend fun addTodoItem(todoItem: TodoItem): Either<Exception, Flow<Long>> {
        return try {
            Either.Success(
                flow {
                    emit(todoDao.addTodoItem(todoItem.toData()))
                }.flowOn(Dispatchers.IO)
            )
        } catch (e: Exception) {
            Either.Error(e)
        }
    }

    override suspend fun deleteTodoItem(todoId: Long): Either<Exception, Flow<Int>> {
        return try {
            Either.Success(flow { emit(todoDao.deleteTodoItem(todoId)) }.flowOn(Dispatchers.IO))
        } catch (e: Exception) {
            Either.Error(e)
        }
    }

    override suspend fun updateTodoItem(todoItem: TodoItem): Either<Exception, Flow<Int>> {
        return try {
            Either.Success(
                flow { emit(todoDao.updateTodoItem(todoItem.toData())) }.flowOn(
                    Dispatchers.IO
                )
            )
        } catch (e: Exception) {
            Either.Error(e)
        }
    }
}
