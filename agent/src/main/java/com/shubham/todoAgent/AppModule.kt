package com.shubham.todoAgent

import android.content.Context
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun providesGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(
                AppFunctionDataTypeMetadata::class.java,
                AppFunctionDataTypeMetadataAdapter()
            )
            .setPrettyPrinting()
            .create()
    }

    @Provides
    @Singleton
    fun provideAppFunctionManager(@ApplicationContext context: Context): AppFunctionManagerCompat =
        AppFunctionManagerCompat.getInstance(context)
            ?: throw UnsupportedOperationException("AppFunctions not supported on this device.")

}
