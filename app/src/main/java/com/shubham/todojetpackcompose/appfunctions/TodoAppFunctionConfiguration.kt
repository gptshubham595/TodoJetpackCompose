package com.shubham.todojetpackcompose.appfunctions

import androidx.appfunctions.service.AppFunctionConfiguration
import com.shubham.todojetpackcompose.appfunctions.impl.AddTodoFunctionImpl
import com.shubham.todojetpackcompose.appfunctions.impl.CompleteTodoFunctionImpl
import com.shubham.todojetpackcompose.appfunctions.impl.DeleteTodoFunctionImpl
import com.shubham.todojetpackcompose.appfunctions.impl.GetAllTodosFunctionImpl
import com.shubham.todojetpackcompose.appfunctions.impl.GetPendingTodosFunctionImpl
import com.shubham.todojetpackcompose.appfunctions.impl.GetTodoStatsFunctionImpl
import com.shubham.todojetpackcompose.domain.usecases.AddTodoItemUseCase
import com.shubham.todojetpackcompose.domain.usecases.DeleteTodoItemUseCase
import com.shubham.todojetpackcompose.domain.usecases.GetTodoListUseCase
import com.shubham.todojetpackcompose.domain.usecases.UpdateTodoItemUseCase
import javax.inject.Inject

/**
 * Builds the [AppFunctionConfiguration] that tells the Android system how to
 * instantiate each AppFunction implementation class.
 *
 * WHY THIS CLASS EXISTS:
 * AppFunction impl classes are created by the Android system via reflection,
 * not by Hilt. So you can't annotate them with @Inject. Instead, you register
 * a factory lambda for each impl. When the system needs to create one, it calls
 * your lambda — and inside that lambda, your Hilt-injected use cases are in scope.
 *
 * This class itself IS Hilt-injectable (it's just a plain class with @Inject),
 * so Hilt will inject the use cases into it, which then get captured by the lambdas.
 *
 * FLOW:
 *   Hilt injects use cases into TodoAppFunctionConfiguration
 *       → TodoApp.appFunctionConfiguration calls .build() on this
 *           → Android system asks for AddTodoFunctionImpl
 *               → factory lambda runs → new AddTodoFunctionImpl(addTodoItemUseCase) ✅
 */
class TodoAppFunctionConfiguration @Inject constructor(
    // Only inject what the impls actually need.
    // GetTodoListUseCase is shared by 5 of the 6 impls — inject once, reuse.
    private val getTodoListUseCase: GetTodoListUseCase,
    private val addTodoItemUseCase: AddTodoItemUseCase,
    private val deleteTodoItemUseCase: DeleteTodoItemUseCase,
    private val updateTodoItemUseCase: UpdateTodoItemUseCase,
) {
    /**
     * Builds and returns the configuration.
     * Called from [TodoApp.appFunctionConfiguration] every time the system needs it.
     * The Builder registers a factory lambda for each impl class.
     */
    fun build(): AppFunctionConfiguration =
        AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(AddTodoFunctionImpl::class.java) {
                AddTodoFunctionImpl(
                    addTodoItemUseCase = addTodoItemUseCase,
                )
            }
            .addEnclosingClassFactory(GetAllTodosFunctionImpl::class.java) {
                GetAllTodosFunctionImpl(
                    getTodoListUseCase = getTodoListUseCase,
                )
            }
            .addEnclosingClassFactory(GetPendingTodosFunctionImpl::class.java) {
                GetPendingTodosFunctionImpl(
                    getTodoListUseCase = getTodoListUseCase,
                )
            }
            .addEnclosingClassFactory(CompleteTodoFunctionImpl::class.java) {
                CompleteTodoFunctionImpl(
                    getTodoListUseCase = getTodoListUseCase,
                    updateTodoItemUseCase = updateTodoItemUseCase,
                )
            }
            .addEnclosingClassFactory(DeleteTodoFunctionImpl::class.java) {
                DeleteTodoFunctionImpl(
                    getTodoListUseCase = getTodoListUseCase,
                    deleteTodoItemUseCase = deleteTodoItemUseCase,
                )
            }
            .addEnclosingClassFactory(GetTodoStatsFunctionImpl::class.java) {
                GetTodoStatsFunctionImpl(
                    getTodoListUseCase = getTodoListUseCase,
                )
            }
            .build()
}
