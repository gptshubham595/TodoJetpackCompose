package com.example.todojetpackcompose


data class Todo(
    val id: Int,
    var title: String,
    val createdAt: Long,
    var isCompleted: Boolean = false
)

