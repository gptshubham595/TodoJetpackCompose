package com.shubham.todojetpackcompose.common

class Utils {
    enum class TodoStatus {
        PENDING,
        COMPLETED
    }

    sealed class Either<out L, out R> {
        data class Error<out L>(val exception: L) : Either<L, Nothing>()
        data class Success<out R>(val data: R) : Either<Nothing, R>()
    }
}
