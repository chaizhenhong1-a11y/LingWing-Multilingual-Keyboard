package com.lazytalk.keyboard.feature.keyboard.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.lazytalk.keyboard.feature.keyboard.presentation.*

@Composable internal fun TypingCandidateBar(s: KeyboardUiState, action: (KeyboardAction) -> Unit) {
    val spellingScroll=rememberScrollState()
    val candidateScroll=rememberScrollState()
    LaunchedEffect(s.composition,spellingScroll.maxValue) { spellingScroll.scrollTo(spellingScroll.maxValue) }
    LaunchedEffect(s.composition) { candidateScroll.scrollTo(0) }
    Column(Modifier.fillMaxWidth().fillMaxHeight()) {
        Text(s.composition, Modifier.fillMaxWidth().height(16.dp).padding(horizontal=8.dp).horizontalScroll(spellingScroll),
            fontSize=12.sp, lineHeight=14.sp, maxLines=1, color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().weight(1f).horizontalScroll(candidateScroll), verticalAlignment=Alignment.CenterVertically) {
            if(s.typingLoading) CircularProgressIndicator(Modifier.padding(horizontal=8.dp).size(16.dp),strokeWidth=1.5.dp)
            s.typingCandidates.forEach { candidate ->
                TextButton({action(KeyboardAction.TypingCandidate(candidate))}, contentPadding=PaddingValues(horizontal=10.dp,vertical=0.dp)) {
                    Text(candidate, maxLines=1, style=MaterialTheme.typography.titleMedium)
                }
            }
            s.typingError?.let { Text(it, Modifier.padding(horizontal=8.dp), style=MaterialTheme.typography.labelSmall,
                maxLines=1,overflow=TextOverflow.Ellipsis) }
        }
    }
}
