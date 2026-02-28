package com.shubham.todoAgent

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AgentViewModel"
private const val TOOL_PACKAGE = "com.shubham.todojetpackcompose"

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val manager: AppFunctionManagerCompat,
    private val gson: Gson,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        // Flip this on to use ToolProvider fallback
        private const val USE_CONTENT_PROVIDER = true

        private const val AUTHORITY = "com.shubham.todojetpackcompose.appfunctions.provider"
        private val CONTENT_URI: Uri = "content://$AUTHORITY".toUri()

        private const val METHOD_START = "start"
        private const val METHOD_GET_METADATA = "get_metadata"
        private const val KEY_METADATA_JSON = "metadata_json"
    }

    private val contentResolver = context.contentResolver

    private val executor = GenericFunctionExecutor(manager, gson)

    private val functionMetadataMap =
        MutableStateFlow<Map<FunctionDeclaration, AppFunctionMetadata>>(emptyMap())

    private val _functions = MutableStateFlow<List<FunctionDeclaration>>(emptyList())
    val functions: StateFlow<List<FunctionDeclaration>> = _functions.asStateFlow()

    private val _result = MutableStateFlow("Tap a function to execute")
    val result: StateFlow<String> = _result.asStateFlow()

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            fetchMetadataFromProvider()
        }
    }

    init {
        if (USE_CONTENT_PROVIDER) {
            startObservingWithContentProvider()
        } else {
            observeToolFunctionsWithManager()
        }
    }

    private fun startObservingWithContentProvider() {
        _result.value = "Using ToolProvider fallback for metadata"
        contentResolver.registerContentObserver(CONTENT_URI, true, observer)

        // tell ToolProvider to start observing appfunctions
        contentResolver.call(AUTHORITY, METHOD_START, null, null)
        // fetch initial snapshot
        fetchMetadataFromProvider()
    }

    private fun fetchMetadataFromProvider() {
        try {
            val metadataJson = contentResolver
                .call(AUTHORITY, METHOD_GET_METADATA, null, null)
                ?.getString(KEY_METADATA_JSON)

            if (metadataJson.isNullOrBlank()) {
                _result.value = "ToolProvider returned empty metadata"
                _functions.value = emptyList()
                return
            }

            val type = object : TypeToken<List<AppFunctionPackageMetadata>>() {}.type
            val packageList: List<AppFunctionPackageMetadata> = gson.fromJson(metadataJson, type)

            val toolPkg = packageList.firstOrNull { it.packageName == TOOL_PACKAGE }
                ?: packageList.firstOrNull()

            if (toolPkg == null) {
                _result.value = "No package metadata in provider response"
                _functions.value = emptyList()
                return
            }

            val map = toolPkg.appFunctions.toFunctionDeclarations()
            functionMetadataMap.value = map
            _functions.value = map.keys.toList().sortedBy { it.shortName }
            _result.value = "Discovered ${map.size} functions (via ToolProvider)"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch metadata from ToolProvider", e)
            _result.value = "Provider metadata error: ${e.message}"
            _functions.value = emptyList()
        }
    }

    private fun observeToolFunctionsWithManager() {
        val spec = AppFunctionSearchSpec(packageNames = setOf(TOOL_PACKAGE))
        manager.observeAppFunctions(spec)
            .catch { e ->
                Log.e(TAG, "observeAppFunctions failed", e)
                _result.value = "Discovery failed: ${e.message}"
                _functions.value = emptyList()
            }
            .onEach { packages ->
                val pkg = packages.firstOrNull()
                if (pkg == null) {
                    _result.value = "No AppFunctions discovered for $TOOL_PACKAGE"
                    _functions.value = emptyList()
                    return@onEach
                }
                val map = pkg.appFunctions.toFunctionDeclarations()
                functionMetadataMap.value = map
                _functions.value = map.keys.toList().sortedBy { it.shortName }
                _result.value = "Discovered ${map.size} functions (via manager)"
            }
            .launchIn(viewModelScope)
    }

    fun execute(function: FunctionDeclaration) {
        val metadata = functionMetadataMap.value[function]
            ?: run {
                _result.value = "Missing metadata for ${function.shortName}"
                return
            }

        // NOTE: you MUST pass required args for functions like addTodo(...)
        val args: Map<String, JsonElement> = emptyMap()

        viewModelScope.launch {
            val res = executor.executeAppFunction(
                targetPackageName = TOOL_PACKAGE,
                appFunctionMetadata = metadata,
                functionDeclaration = function,
                arguments = args,
            )
            _result.value = res.fold(
                onSuccess = { it.toString() },
                onFailure = { "Error: ${it.message ?: it.toString()}" },
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (USE_CONTENT_PROVIDER) {
            contentResolver.unregisterContentObserver(observer)
        }
    }
}