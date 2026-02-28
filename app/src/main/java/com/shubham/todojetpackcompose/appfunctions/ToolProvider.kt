package com.shubham.todojetpackcompose.appfunctions

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import androidx.core.net.toUri
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicReference

/**
 * Provides AppFunction metadata to an external agent via ContentResolver.call().
 *
 * This is a fallback approach: metadata ideally should be queried by the agent directly, but
 * some devices/builds may require fetching it inside the tool app process [6].
 */
class ToolProvider : ContentProvider() {

    companion object {
        private const val TAG = "ComposeTodoToolProvider"

        // Must match the manifest authorities value exactly.
        const val AUTHORITY = "com.shubham.todojetpackcompose.appfunctions.provider"
        val CONTENT_URI: Uri = "content://$AUTHORITY".toUri()

        // ContentResolver.call() method names
        const val METHOD_START = "start"
        const val METHOD_GET_METADATA = "get_metadata"

        // Bundle key
        const val KEY_METADATA_JSON = "metadata_json"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val metadataItems =
        AtomicReference<List<AppFunctionPackageMetadata>>(emptyList())

    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(
                AppFunctionDataTypeMetadata::class.java,
                AppFunctionDataTypeMetadataAdapter(),
            )
            .create()
    }

    private val appFunctionManager by lazy {
        val ctx = context ?: throw IllegalStateException("Context not available")
        AppFunctionManagerCompat.getInstance(ctx)
            ?: throw IllegalStateException("AppFunctions not supported on this device")
    }

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        Log.i(TAG, "call(method=$method)")

        return when (method) {
            METHOD_START -> {
                observeOwnAppFunctions()
                null
            }

            METHOD_GET_METADATA -> {
                Bundle().apply {
                    putString(KEY_METADATA_JSON, gson.toJson(metadataItems.get()))
                }
            }

            else -> throw UnsupportedOperationException("Unknown method: $method")
        }
    }

    private fun observeOwnAppFunctions() {
        val pkg = context?.packageName ?: return
        val searchSpec = AppFunctionSearchSpec(packageNames = setOf(pkg))

        appFunctionManager
            .observeAppFunctions(searchSpec)
            .onEach { packages ->
                metadataItems.set(packages)
                notifyChange()
                Log.i(TAG, "Updated AppFunction metadata: packages=${packages.size}")
            }
            .launchIn(scope)
    }

    private fun notifyChange() {
        context?.contentResolver?.notifyChange(CONTENT_URI, null)
    }

    // Not used; this provider only supports call()
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor = throw UnsupportedOperationException()

    override fun insert(uri: Uri, values: ContentValues?): Uri =
        throw UnsupportedOperationException()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException()

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException()

    override fun getType(uri: Uri): String =
        throw UnsupportedOperationException()
}

/**
 * Enables Gson to deserialize AppFunctionDataTypeMetadata polymorphically by storing the class name.
 */
class AppFunctionDataTypeMetadataAdapter :
    JsonSerializer<AppFunctionDataTypeMetadata>,
    JsonDeserializer<AppFunctionDataTypeMetadata> {

    companion object {
        private const val CLASSNAME = "DATATYPE_CLASS"
    }

    override fun serialize(
        src: AppFunctionDataTypeMetadata,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement {
        val json = context.serialize(src)
        if (json.isJsonObject) {
            json.asJsonObject.addProperty(CLASSNAME, src.javaClass.name)
        }
        return json
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): AppFunctionDataTypeMetadata {
        val obj = json.asJsonObject
        val className = obj.get(CLASSNAME).asString
        obj.remove(CLASSNAME)

        return try {
            val clazz = Class.forName(className)
            context.deserialize(obj, clazz)
        } catch (e: ClassNotFoundException) {
            throw JsonParseException(e)
        }
    }
}