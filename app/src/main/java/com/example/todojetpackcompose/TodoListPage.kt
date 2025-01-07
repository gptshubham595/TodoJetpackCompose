package com.example.todojetpackcompose

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat

@Composable
fun TodoListPage(viewModel: TodoViewModel) {
    val todoList = viewModel.todoList.collectAsState()
    val inputText = rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = inputText.value,
                onValueChange = {
                    inputText.value = it
                },
                modifier = Modifier.fillMaxWidth(0.7f)
            )
            Button(onClick = {
                if (inputText.value.isNotBlank()) {
                    viewModel.addTodo(inputText.value)
                    inputText.value = ""
                }
            }) {
                Text(text = "Add")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            content = {
                items(
                    todoList.value,
                    key = { it.id }
                ) { item ->
                    TodoItem(
                        item = item,
                        onCompleted = viewModel::onCompleted,
                        onDelete = viewModel::removeTodo
                    )
                }
            }
        )
    }
}

@Composable
fun TodoItem(
    item: Todo,
    onCompleted: (Int) -> Unit = {},
    onDelete: (Int) -> Unit = {}
) {
    val backgroundColor = if (item.isCompleted) {
        MaterialTheme.colorScheme.secondary // Or your desired selected color
    } else {
        MaterialTheme.colorScheme.primary
    }

    val tintColor = if (item.isCompleted) {
        Color.Red // Or your desired selected tint color
    } else {
        Color.Green
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = SimpleDateFormat(
                    "HH:mm:aa, dd/mm",
                ).format(item.createdAt),
                fontSize = 10.sp,
                color = Color.LightGray
            )
            Text(
                text = item.title,
                fontSize = 20.sp,
                color = Color.White
            )
        }
        Row {
            IconButton(onClick = {
                onCompleted(item.id)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_add_task_24),
                    contentDescription = "Selected",
                    tint = tintColor
                )
            }
            IconButton(onClick = {
                onDelete(item.id)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_delete_24),
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TodoListPagePreview() {
    TodoListPage(viewModel = TodoViewModel())
}