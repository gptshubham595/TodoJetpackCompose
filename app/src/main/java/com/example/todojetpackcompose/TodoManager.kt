package com.example.todojetpackcompose

import java.time.Instant
import java.util.Date

object TodoManager {
    // Using StateFlow instead of mutable list to properly handle state changes
    private val todoList = mutableListOf<Todo>()

    fun getTodoList(): List<Todo> = todoList.map {it.copy()}

    fun addTodo(title: String) {
        val newTodo = Todo(System.currentTimeMillis().toInt(), title, System.currentTimeMillis())
        todoList.add(0, newTodo)
    }

    private fun addTodo(todo: Todo) {
        todoList.add(0, todo)
    }

    fun removeTodo(id: Int) {
        todoList.removeAll { it.id == id }
    }

    fun onCompleted(id: Int) {
        val item = todoList.find { it.id == id }
        if (item == null) return
        todoList.remove(item)
        item.isCompleted = true
        addTodo(item)
    }
}
