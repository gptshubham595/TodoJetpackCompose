package com.example.todojetpackcompose

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel : ViewModel() {
    // Directly expose TodoManager's StateFlow
    private val _todoList = MutableStateFlow<List<Todo>>(emptyList())
    val todoList = _todoList.asStateFlow()

    fun getTodoList() {
        _todoList.value = TodoManager.getTodoList()
        for (todo in _todoList.value) {
            Log.d("TodoViewModel2", "${todo.title} ${todo.isCompleted}")
        }
    }

    fun addTodo(title: String) {
        TodoManager.addTodo(title)
        getTodoList()
    }

    fun removeTodo(id: Int) {
        TodoManager.removeTodo(id)
        getTodoList()
    }

    fun onCompleted(id: Int) {
        TodoManager.onCompleted(id)
        for (todo in _todoList.value) {
            Log.d("TodoViewModel", "${todo.title} ${todo.isCompleted}")
        }
        getTodoList()
    }
}
