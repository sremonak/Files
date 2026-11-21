package com.sremonak.files.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sremonak.files.R
import com.sremonak.files.data.model.FileCategory
import com.sremonak.files.ui.components.EmptyState
import com.sremonak.files.ui.components.FileListItem
import com.sremonak.files.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenDetails: (String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showTypes by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_search)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clear() }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    FilterChip(selected = state.category != null, onClick = { showTypes = true },
                        label = { Text(state.category?.name ?: "All types") })
                    DropdownMenu(expanded = showTypes, onDismissRequest = { showTypes = false }) {
                        DropdownMenuItem(text = { Text("All types") }, onClick = { showTypes = false; viewModel.setCategory(null) })
                        FileCategory.entries.forEach { category ->
                            DropdownMenuItem(text = { Text(category.name) }, onClick = { showTypes = false; viewModel.setCategory(category) })
                        }
                    }
                }
                FilterChip(selected = state.minimumSize > 0, onClick = {
                    viewModel.setMinimumSize(if (state.minimumSize > 0) 0 else 100L * 1024 * 1024)
                }, label = { Text("100 MB+") })
                FilterChip(selected = state.recentDays > 0, onClick = {
                    viewModel.setRecentDays(if (state.recentDays > 0) 0 else 7)
                }, label = { Text("Last 7 days") })
            }
            if (state.searching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                state.searching -> Unit
                !state.searched -> EmptyState(
                    icon = Icons.Filled.Search,
                    title = "Search your files",
                    subtitle = "Type a name to search across internal storage"
                )
                state.results.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Search,
                    title = stringResource(R.string.search_no_results)
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.results, key = { it.path }) { info ->
                        FileListItem(
                            info = info,
                            selected = false,
                            selectionMode = false,
                            onClick = { onOpenDetails(info.path) },
                            onLongClick = { onOpenDetails(info.path) }
                        )
                    }
                }
            }
        }
    }
}
