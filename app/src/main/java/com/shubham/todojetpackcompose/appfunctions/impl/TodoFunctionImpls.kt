package com.shubham.todojetpackcompose.appfunctions.impl

import android.util.Log
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.service.AppFunction
import com.shubham.todojetpackcompose.appfunctions.schemas.AddTodoFunction
import com.shubham.todojetpackcompose.appfunctions.schemas.AppFunctionTodoItem
import com.shubham.todojetpackcompose.appfunctions.schemas.CompleteTodoFunction
import com.shubham.todojetpackcompose.appfunctions.schemas.DeleteTodoFunction
import com.shubham.todojetpackcompose.appfunctions.schemas.GetAllTodosFunction
import com.shubham.todojetpackcompose.appfunctions.schemas.GetPendingTodosFunction
import com.shubham.todojetpackcompose.appfunctions.schemas.GetTodoStatsFunction
import com.shubham.todojetpackcompose.appfunctions.schemas.TodoMutationResult
import com.shubham.todojetpackcompose.appfunctions.schemas.TodoStats
import com.shubham.todojetpackcompose.common.Utils
import com.shubham.todojetpackcompose.domain.models.TodoItem
import com.shubham.todojetpackcompose.domain.usecases.AddTodoItemUseCase
import com.shubham.todojetpackcompose.domain.usecases.DeleteTodoItemUseCase
import com.shubham.todojetpackcompose.domain.usecases.GetTodoListUseCase
import com.shubham.todojetpackcompose.domain.usecases.UpdateTodoItemUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val TAG = "TodoAppFunctions"

// ─────────────────────────────────────────────────────────────────────────────
// WHY runBlocking?
//
// @AppFunction methods cannot be suspend functions — the Android system
// calls them synchronously on a background thread it manages.
// Your use cases return Either<Exception, Flow<T>>, which is async.
// runBlocking bridges the two worlds safely here because:
//   1. The system already dispatched us off the main thread.
//   2. Each call is a single short-lived DB read or write.
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// PRIVATE HELPERS
// Shared logic used by multiple impls — keeps each class lean.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Fetches all todos synchronously. Wraps the Either+Flow pattern into a simple List.
 * Returns empty list on any error so callers don't need to handle Either themselves.
 */
private fun GetTodoListUseCase.fetchAllBlocking(): List<TodoItem> = runBlocking {
    when (val result = invoke()) {
        is Utils.Either.Success -> result.data.first()
        is Utils.Either.Error -> {
            Log.e(TAG, "fetchAllBlocking failed: ${result.exception.message}")
            emptyList()
        }
    }
}

/**
 * Maps your domain [TodoItem] to the AppFunction-safe [AppFunctionTodoItem].
 * id: Long → String because AppFunctions serialization works best with String across processes.
 */
private fun TodoItem.toAppFunctionItem() = AppFunctionTodoItem(
    id = id.toString(),    // Long → String. Agent passes it back as String when calling completeTodo/deleteTodo.
    task = task,
    status = status,       // Already "PENDING" or "COMPLETED" — matches @AppFunctionStringValueConstraint
)

// ─────────────────────────────────────────────────────────────────────────────
// IMPLEMENTATIONS
// Each class implements exactly one schema interface.
// Constructor takes only the use cases it actually needs — no over-injection.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Implementation of [AddTodoFunction].
 * Depends on: [AddTodoItemUseCase] to save, [GetTodoListUseCase] not needed here.
 */
class AddTodoFunctionImpl(
    private val addTodoItemUseCase: AddTodoItemUseCase,
) : AddTodoFunction {

    /**
     * Creates and saves a new todo item with the given task description.
     * Use this when the user wants to add, create, or remember a new task or todo item.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @param task The task description to save. Must not be blank.
     * @return A [TodoMutationResult] indicating whether the task was saved successfully.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun addTodo(
        appFunctionContext: AppFunctionContext,
        task: String,
    ): TodoMutationResult {
        Log.i(TAG, "addTodo called: task=$task")

        // Validate — throw AppFunctionInvalidArgumentException for bad input.
        // The system catches this and returns a structured error to the caller.
        if (task.isBlank()) {
            throw AppFunctionInvalidArgumentException("Task description must not be blank.")
        }

        // Build domain model exactly the same way TodoViewModel does.
        val newItem = TodoItem(
            id = System.currentTimeMillis(),  // epoch millis — same ID strategy as your ViewModel
            task = task.trim(),
            status = Utils.TodoStatus.PENDING.name,
        )

        return runBlocking {
            when (val result = addTodoItemUseCase(newItem)) {
                is Utils.Either.Success -> {
                    result.data.first()  // consume the Flow — triggers the DB write
                    TodoMutationResult(
                        success = true,
                        message = "Task '${newItem.task}' added successfully.",
                    )
                }
                is Utils.Either.Error -> {
                    Log.e(TAG, "addTodo failed: ${result.exception.message}")
                    TodoMutationResult(
                        success = false,
                        message = result.exception.message ?: "Failed to add task.",
                    )
                }
            }
        }
    }
}

/**
 * Implementation of [GetAllTodosFunction].
 * Depends on: [GetTodoListUseCase] only.
 */
class GetAllTodosFunctionImpl(
    private val getTodoListUseCase: GetTodoListUseCase,
) : GetAllTodosFunction {

    /**
     * Returns every todo item currently stored in the app, both pending and completed.
     * Use this when the user wants to see, list, or review all their tasks.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @return A list of all [AppFunctionTodoItem] objects. Returns an empty list if no todos exist.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun getAllTodos(appFunctionContext: AppFunctionContext): List<AppFunctionTodoItem> {
        Log.i(TAG, "getAllTodos called")
        return getTodoListUseCase
            .fetchAllBlocking()
            .map { it.toAppFunctionItem() }
    }
}

/**
 * Implementation of [GetPendingTodosFunction].
 * Depends on: [GetTodoListUseCase] only.
 */
class GetPendingTodosFunctionImpl(
    private val getTodoListUseCase: GetTodoListUseCase,
) : GetPendingTodosFunction {

    /**
     * Returns all todo items that are still pending — i.e., not yet completed.
     * Use this when the user asks what tasks are remaining, outstanding, or still to do.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @return A list of pending [AppFunctionTodoItem] objects. Returns an empty list if all tasks are done.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun getPendingTodos(appFunctionContext: AppFunctionContext): List<AppFunctionTodoItem> {
        Log.i(TAG, "getPendingTodos called")
        return getTodoListUseCase
            .fetchAllBlocking()
            .filter { it.status == Utils.TodoStatus.PENDING.name }
            .map { it.toAppFunctionItem() }
    }
}

/**
 * Implementation of [CompleteTodoFunction].
 * Depends on: [GetTodoListUseCase] to find the item, [UpdateTodoItemUseCase] to save the change.
 */
class CompleteTodoFunctionImpl(
    private val getTodoListUseCase: GetTodoListUseCase,
    private val updateTodoItemUseCase: UpdateTodoItemUseCase,
) : CompleteTodoFunction {

    /**
     * Marks the todo with the given ID as completed.
     * Use this when the user says they finished, completed, or are done with a specific task.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @param todoId The unique ID of the todo to complete. This is the numeric string from [AppFunctionTodoItem.id].
     * @return A [TodoMutationResult] indicating whether the todo was successfully marked as completed.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun completeTodo(
        appFunctionContext: AppFunctionContext,
        todoId: String,
    ): TodoMutationResult {
        Log.i(TAG, "completeTodo called: todoId=$todoId")

        // todoId comes in as a String from the agent. Parse back to Long for Room lookup.
        val id = todoId.toLongOrNull()
            ?: throw AppFunctionInvalidArgumentException(
                "Invalid todoId '$todoId'. Must be a numeric string like '1709123456789'."
            )

        // Fetch the item first — we need the full object to pass to updateTodoItem,
        // and we want a meaningful error message if it doesn't exist.
        val existing = getTodoListUseCase
            .fetchAllBlocking()
            .find { it.id == id }
            ?: return TodoMutationResult(
                success = false,
                message = "No todo found with id '$todoId'.",
            )

        // Guard: already completed — no-op with a clear message.
        if (existing.status == Utils.TodoStatus.COMPLETED.name) {
            return TodoMutationResult(
                success = false,
                message = "Todo '${existing.task}' is already completed.",
            )
        }

        val updated = existing.copy(status = Utils.TodoStatus.COMPLETED.name)

        return runBlocking {
            when (val result = updateTodoItemUseCase(updated)) {
                is Utils.Either.Success -> {
                    result.data.first()
                    TodoMutationResult(
                        success = true,
                        message = "Todo '${existing.task}' marked as completed.",
                    )
                }
                is Utils.Either.Error -> {
                    Log.e(TAG, "completeTodo failed: ${result.exception.message}")
                    TodoMutationResult(
                        success = false,
                        message = result.exception.message ?: "Failed to update todo.",
                    )
                }
            }
        }
    }
}

/**
 * Implementation of [DeleteTodoFunction].
 * Depends on: [GetTodoListUseCase] to find the item name, [DeleteTodoItemUseCase] to remove it.
 */
class DeleteTodoFunctionImpl(
    private val getTodoListUseCase: GetTodoListUseCase,
    private val deleteTodoItemUseCase: DeleteTodoItemUseCase,
) : DeleteTodoFunction {

    /**
     * Permanently removes the todo with the given ID from the list.
     * Use this when the user wants to delete, remove, or discard a specific task.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @param todoId The unique ID of the todo to delete. This is the numeric string from [AppFunctionTodoItem.id].
     * @return A [TodoMutationResult] indicating whether the todo was successfully deleted.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun deleteTodo(
        appFunctionContext: AppFunctionContext,
        todoId: String,
    ): TodoMutationResult {
        Log.i(TAG, "deleteTodo called: todoId=$todoId")

        val id = todoId.toLongOrNull()
            ?: throw AppFunctionInvalidArgumentException(
                "Invalid todoId '$todoId'. Must be a numeric string like '1709123456789'."
            )

        // Fetch name before deleting so the success message is meaningful.
        val existing = getTodoListUseCase
            .fetchAllBlocking()
            .find { it.id == id }
            ?: return TodoMutationResult(
                success = false,
                message = "No todo found with id '$todoId'.",
            )

        return runBlocking {
            when (val result = deleteTodoItemUseCase(id)) {
                is Utils.Either.Success -> {
                    result.data.first()
                    TodoMutationResult(
                        success = true,
                        message = "Todo '${existing.task}' deleted.",
                    )
                }
                is Utils.Either.Error -> {
                    Log.e(TAG, "deleteTodo failed: ${result.exception.message}")
                    TodoMutationResult(
                        success = false,
                        message = result.exception.message ?: "Failed to delete todo.",
                    )
                }
            }
        }
    }
}

/**
 * Implementation of [GetTodoStatsFunction].
 * Depends on: [GetTodoListUseCase] only.
 */
class GetTodoStatsFunctionImpl(
    private val getTodoListUseCase: GetTodoListUseCase,
) : GetTodoStatsFunction {

    /**
     * Returns a summary of the current todo list: total count, completed count, and pending count.
     * Use this when the user asks how many tasks they have, how many are done, or wants a summary.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @return A [TodoStats] object with counts for total, completed, and pending todos.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun getTodoStats(appFunctionContext: AppFunctionContext): TodoStats {
        Log.i(TAG, "getTodoStats called")
        val all = getTodoListUseCase.fetchAllBlocking()
        return TodoStats(
            total = all.size,
            completed = all.count { it.status == Utils.TodoStatus.COMPLETED.name },
            pending = all.count { it.status == Utils.TodoStatus.PENDING.name },
        )
    }
}