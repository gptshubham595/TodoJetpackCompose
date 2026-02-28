package com.shubham.todoAgent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vm: AgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val functions by vm.functions.collectAsStateWithLifecycle()
            val result by vm.result.collectAsStateWithLifecycle()

            MaterialTheme {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text("ComposeTodo Agent", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(result)
                    Spacer(Modifier.height(16.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(functions) { f ->
                            Button(
                                onClick = { vm.execute(f) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(f.shortName) }
                        }
                    }
                }
            }
        }
    }
}