package com.shubham.todojetpackcompose.appfunctions.schemas

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSchemaDefinition
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionStringValueConstraint

// ─────────────────────────────────────────────────────────────────────────────
// SHARED DATA TYPES
// These are the objects that travel across the AppFunction boundary.
// @AppFunctionSerializable tells KSP how to serialize/deserialize them.
// Keep them SEPARATE from your domain models — they are the public API contract.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents a single Todo item as seen by an external AI agent or caller.
 *
 * Note: [id] is a String even though your Room DB stores it as Long.
 * AppFunctions serialization handles String cleanly across processes.
 * We convert Long → String in the mapper inside each impl.
 */
@AppFunctionSerializable(isDescribedByKdoc = true)
data class AppFunctionTodoItem(
    /** Unique identifier for the todo (epoch millis as a numeric string e.g. "1709123456789"). */
    val id: String,
    /** The task description text. */
    val task: String,
    /**
     * Current status. Always one of "PENDING" or "COMPLETED".
     * Matches [com.shubham.todojetpackcompose.common.Utils.TodoStatus].
     */
    @AppFunctionStringValueConstraint(enumValues = ["PENDING", "COMPLETED"])
    val status: String,
)

/**
 * Returned by any operation that creates, updates, or deletes a todo.
 */
@AppFunctionSerializable(isDescribedByKdoc = true)
data class TodoMutationResult(
    /** True if the operation completed successfully, false otherwise. */
    val success: Boolean,
    /** Human-readable message describing the outcome, suitable for display to the user. */
    val message: String,
)

/**
 * Aggregated statistics about the current state of the todo list.
 */
@AppFunctionSerializable(isDescribedByKdoc = true)
data class TodoStats(
    /** Total number of todos (pending + completed). */
    val total: Int,
    /** Number of todos marked as COMPLETED. */
    val completed: Int,
    /** Number of todos still marked as PENDING. */
    val pending: Int,
)

// ─────────────────────────────────────────────────────────────────────────────
// SCHEMA INTERFACES
// Each interface = one discoverable AppFunction.
// @AppFunctionSchemaDefinition registers it with the Android OS at index time.
// The KDoc on the function body becomes the LLM-readable description —
// this is what Gemini or another agent reads to decide when to call your function.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Exposes the ability to add a new todo task.
 */
@AppFunctionSchemaDefinition(name = "addTodo", version = 1, category = "todo")
interface AddTodoFunction {
    /**
     * Creates and saves a new todo item with the given task description.
     * Use this when the user wants to add, create, or remember a new task or todo item.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @param task The task description to save. Must not be blank.
     * @return A [TodoMutationResult] indicating whether the task was saved successfully.
     */
    fun addTodo(
        appFunctionContext: AppFunctionContext,
        task: String,
    ): TodoMutationResult
}

/**
 * Exposes the ability to retrieve all todo items.
 */
@AppFunctionSchemaDefinition(name = "getAllTodos", version = 1, category = "todo")
interface GetAllTodosFunction {
    /**
     * Returns every todo item currently stored in the app, both pending and completed.
     * Use this when the user wants to see, list, or review all their tasks.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @return A list of all [AppFunctionTodoItem] objects. Returns an empty list if no todos exist.
     */
    fun getAllTodos(appFunctionContext: AppFunctionContext): List<AppFunctionTodoItem>
}

/**
 * Exposes the ability to retrieve only pending (incomplete) todos.
 */
@AppFunctionSchemaDefinition(name = "getPendingTodos", version = 1, category = "todo")
interface GetPendingTodosFunction {
    /**
     * Returns all todo items that are still pending — i.e., not yet completed.
     * Use this when the user asks what tasks are remaining, outstanding, or still to do.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @return A list of pending [AppFunctionTodoItem] objects. Returns an empty list if all tasks are done.
     */
    fun getPendingTodos(appFunctionContext: AppFunctionContext): List<AppFunctionTodoItem>
}

/**
 * Exposes the ability to mark a todo as completed.
 */
@AppFunctionSchemaDefinition(name = "completeTodo", version = 1, category = "todo")
interface CompleteTodoFunction {
    /**
     * Marks the todo with the given ID as completed.
     * Use this when the user says they finished, completed, or are done with a specific task.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @param todoId The unique ID of the todo to complete. This is the numeric string from [AppFunctionTodoItem.id].
     * @return A [TodoMutationResult] indicating whether the todo was successfully marked as completed.
     */
    fun completeTodo(
        appFunctionContext: AppFunctionContext,
        todoId: String,
    ): TodoMutationResult
}

/**
 * Exposes the ability to delete a todo item.
 */
@AppFunctionSchemaDefinition(name = "deleteTodo", version = 1, category = "todo")
interface DeleteTodoFunction {
    /**
     * Permanently removes the todo with the given ID from the list.
     * Use this when the user wants to delete, remove, or discard a specific task.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @param todoId The unique ID of the todo to delete. This is the numeric string from [AppFunctionTodoItem.id].
     * @return A [TodoMutationResult] indicating whether the todo was successfully deleted.
     */
    fun deleteTodo(
        appFunctionContext: AppFunctionContext,
        todoId: String,
    ): TodoMutationResult
}

/**
 * Exposes the ability to retrieve todo list statistics.
 */
@AppFunctionSchemaDefinition(name = "getTodoStats", version = 1, category = "todo")
interface GetTodoStatsFunction {
    /**
     * Returns a summary of the current todo list: total count, completed count, and pending count.
     * Use this when the user asks how many tasks they have, how many are done, or wants a summary.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @return A [TodoStats] object with counts for total, completed, and pending todos.
     */
    fun getTodoStats(appFunctionContext: AppFunctionContext): TodoStats
}