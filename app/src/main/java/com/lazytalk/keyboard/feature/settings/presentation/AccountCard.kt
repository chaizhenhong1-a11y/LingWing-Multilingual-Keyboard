package com.lazytalk.keyboard.feature.settings.presentation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.lazytalk.keyboard.ui.uiText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable fun AccountCard(model: SettingsViewModel, onOpen: () -> Unit) {
    val account by model.account.collectAsState()
    Card(onClick=onOpen,modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(18.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            AccountMark(Modifier.size(44.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(uiText(if(account.email.isBlank()) "登录 LingWing Keyboard" else "我的账号"),style=MaterialTheme.typography.titleMedium)
                Text(uiText(if(account.email.isBlank()) "登录后开启 AI 翻译" else account.email),style=MaterialTheme.typography.bodyMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                if(account.unlimited) Text(uiText("免费 · 不限次数"),color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium)
            }
            Text(uiText("›"),style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
