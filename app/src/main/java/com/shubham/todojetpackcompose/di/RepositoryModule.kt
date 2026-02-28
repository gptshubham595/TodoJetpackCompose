package com.shubham.todojetpackcompose.di

import com.shubham.todojetpackcompose.data.repo.TodoRepositoryImpl
import com.shubham.todojetpackcompose.domain.repo.TodoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTodoRepository(impl: TodoRepositoryImpl): TodoRepository
}