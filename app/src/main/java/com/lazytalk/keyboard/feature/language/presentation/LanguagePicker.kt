package com.lazytalk.keyboard.feature.language.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import com.lazytalk.keyboard.ui.uiText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.feature.keyboard.components.KeyboardRows
import com.lazytalk.keyboard.feature.keyboard.presentation.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment

@Composable fun LanguagePicker(languages: List<Language>, settings: UserSettings, source: Boolean,
    onSelect: (String) -> Unit, onFavorite: (String) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier, ime: Boolean = false,
    loading: Boolean = false, loadError: String? = null, onRetry: () -> Unit = {}) {
    var query by remember { mutableStateOf("") }
    val wideIme = ime && LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val searchKeys: @Composable () -> Unit = {
        KeyboardRows(KeyboardUiState(enterLabel = "Clear"), { event ->
            when (event) {
                is KeyboardAction.Key -> query = (query + event.text).take(60)
                KeyboardAction.Backspace -> query = query.dropLast(1)
                KeyboardAction.Enter -> query = ""
                else -> Unit
            }
        }, keyHeight = if (wideIme) 38.dp else 48.dp)
    }
    val searchField: @Composable () -> Unit = {
        OutlinedTextField(query, { query = it }, readOnly = ime, label = { Text(uiText("Search Languages")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
    Column(modifier.padding(horizontal = 8.dp)) {
        Row { Text(uiText("Choose language"), modifier = Modifier.weight(1f)); TextButton(onClick = onClose) { Text(uiText("Close")) } }
        val filtered = languages.filter { it.name.contains(query, true) || it.nativeName.contains(query, true) || it.code.contains(query, true) }
        val groups = if (query.isNotBlank()) listOf("Results" to filtered) else listOf(
            "Recent" to settings.recent.mapNotNull { code -> languages.find { it.code == code } },
            "Favorites" to languages.filter { it.code in settings.favorites },
            "Popular" to languages.take(15), "All Languages" to languages.sortedBy { it.name })
        val languageList: @Composable (Modifier) -> Unit = { listModifier -> LazyColumn(listModifier) {
            if (languages.isNotEmpty() && loadError != null) item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(loadError, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = onRetry, enabled = !loading) { Text(uiText("Retry connection")) }
                }
            }
            if (source) item { TextButton(onClick = { onSelect("auto") }) { Text(uiText("Auto Detect")) } }
            groups.forEach { (title, list) ->
                if (list.isNotEmpty()) {
                    item { Text(uiText(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                    items(list, key = { "$title-${it.code}" }) { language ->
                        Row(Modifier.fillMaxWidth()) {
                            TextButton(onClick = { onSelect(language.code) }, modifier = Modifier.weight(1f)) {
                                Text("${language.nativeName} · ${language.name}" + if (language.status != "stable") " (${language.status})" else "")
                            }
                            TextButton(onClick = { onFavorite(language.code) }, modifier = Modifier.semantics { contentDescription = "Toggle favorite ${language.name}" }) { Text(if (language.code in settings.favorites) "★" else "☆") }
                        }
                    }
                }
            }
            if (languages.isEmpty()) item {
                Text(if (loading) "Loading languages…" else loadError ?: "Language list is empty. Refresh to try again.")
                if (!loading) TextButton(onClick = onRetry) { Text(uiText("Retry language list")) }
            } else if (filtered.isEmpty()) item { Text(uiText("No matching languages")) }
        } }
        if (wideIme) Row(Modifier.weight(1f)) {
            Column(Modifier.weight(0.48f).fillMaxHeight()) {
                searchField()
                languageList(Modifier.weight(1f))
            }
            Column(Modifier.weight(0.52f).fillMaxHeight().padding(start = 8.dp), verticalArrangement = Arrangement.Center) { searchKeys() }
        } else {
            searchField()
            languageList(Modifier.weight(1f))
            if (ime) searchKeys()
        }
    }
}
