package com.sremonak.files.ui.screens.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sremonak.files.R
import com.sremonak.files.ui.components.ConfirmDialog
import com.sremonak.files.ui.components.EmptyState
import com.sremonak.files.ui.theme.MonoFontFamily
import com.sremonak.files.util.IntentUtils
import com.sremonak.files.viewmodel.EditorViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    path: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }

    var showFind by remember { mutableStateOf(false) }
    var showReplace by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var fontSize by remember { mutableStateOf(14.sp) }
    var showMenu by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    BackHandler(enabled = state.isDirty) { showDiscardConfirm = true }

    LaunchedEffect(path) { viewModel.load(path) }
    LaunchedEffect(state.saveError) {
        state.saveError?.let { snackbarHost.showSnackbar(it); viewModel.clearSaveError() }
    }
    LaunchedEffect(state.savedTick) {
        if (state.savedTick > 0) snackbarHost.showSnackbar("Saved")
    }

    val matchCount = remember(state.content, findQuery) {
        if (findQuery.isBlank()) 0
        else state.content.lowercase().split(findQuery.lowercase()).size - 1
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.fileName + if (state.isDirty) " •" else "",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isDirty) showDiscardConfirm = true else onBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = state.isDirty
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = stringResource(R.string.action_save))
                    }
                    IconButton(onClick = { showFind = !showFind }) {
                        Icon(Icons.Filled.Search, contentDescription = "Find")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Increase font size") },
                                onClick = { showMenu = false; fontSize = (fontSize.value + 2f).coerceAtMost(28f).sp }
                            )
                            DropdownMenuItem(
                                text = { Text("Decrease font size") },
                                onClick = { showMenu = false; fontSize = (fontSize.value - 2f).coerceAtLeast(10f).sp }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_share)) },
                                leadingIcon = { Icon(Icons.Filled.Share, null) },
                                onClick = {
                                    showMenu = false
                                    IntentUtils.shareFiles(context, listOf(File(path)))
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showFind) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = findQuery,
                        onValueChange = { findQuery = it },
                        placeholder = { Text("Find") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (findQuery.isBlank()) "" else "$matchCount found",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = { showReplace = !showReplace }) {
                        Icon(Icons.Filled.Close, contentDescription = "Toggle replace")
                    }
                }
                if (showReplace) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replaceQuery,
                            onValueChange = { replaceQuery = it },
                            placeholder = { Text("Replace with") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        TextButton(
                            onClick = {
                                if (findQuery.isNotBlank()) {
                                    viewModel.update(state.content.replace(findQuery, replaceQuery))
                                }
                            },
                            enabled = findQuery.isNotBlank()
                        ) {
                            Text("Replace all")
                        }
                    }
                }
            }

            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                state.error != null -> EmptyState(
                    icon = Icons.Filled.Close,
                    title = state.error ?: "Error"
                )
                else -> BasicTextField(
                    value = state.content,
                    onValueChange = viewModel::update,
                    textStyle = TextStyle(
                        fontFamily = MonoFontFamily,
                        fontSize = fontSize,
                        lineHeight = fontSize * 1.4,
                        color = if (findQuery.isBlank()) MaterialTheme.colorScheme.onSurface else Color.Transparent
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(12.dp)
                        ) {
                            if (findQuery.isNotBlank()) {
                                Text(
                                    text = highlightMatches(state.content, findQuery),
                                    fontFamily = MonoFontFamily,
                                    fontSize = fontSize,
                                    lineHeight = fontSize * 1.4,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        if (showDiscardConfirm) {
            ConfirmDialog(
                title = "Discard changes?",
                message = "You have unsaved changes.",
                confirmText = stringResource(R.string.action_delete),
                onConfirm = { showDiscardConfirm = false; onBack() },
                onDismiss = { showDiscardConfirm = false }
            )
        }
    }
}

private fun highlightMatches(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val builder = AnnotatedString.Builder()
    val lower = text.lowercase()
    val needle = query.lowercase()
    var index = 0
    while (true) {
        val found = lower.indexOf(needle, index)
        if (found == -1) {
            builder.append(text.substring(index))
            break
        }
        builder.append(text.substring(index, found))
        builder.append(
            AnnotatedString(
                text.substring(found, found + needle.length),
                spanStyle = SpanStyle(background = Color(0xFFFFEB3B))
            )
        )
        index = found + needle.length
    }
    return builder.toAnnotatedString()
}
