package com.lazytalk.keyboard.ui
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.lazytalk.keyboard.core.model.TypingLayout

@Composable fun InterfaceLanguageButton(onClick: (() -> Unit)? = null) {
    val context=LocalContext.current
    TextButton(onClick=onClick ?: {
        context.startActivity(Intent().setClassName(context.packageName,context.packageName+".SettingsActivity")
            .putExtra("interfaceLanguage",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    },contentPadding=PaddingValues(horizontal=6.dp)) {
        val color=MaterialTheme.colorScheme.primary
        Canvas(Modifier.size(18.dp)) {
            val stroke=Stroke(1.4.dp.toPx())
            drawCircle(color,style=stroke)
            drawOval(color,Offset(size.width*.28f,0f),Size(size.width*.44f,size.height),style=stroke)
            drawLine(color,Offset(0f,size.height/2),Offset(size.width,size.height/2),1.4.dp.toPx())
        }
        Spacer(Modifier.width(4.dp))
        Text("Language",style=MaterialTheme.typography.labelMedium)
    }
}

@Composable fun layoutDisplayName(layout: TypingLayout): String {
    val locale=LocalConfiguration.current.locales[0]
    return if(locale.language=="zh") layout.chineseName else java.util.Locale.forLanguageTag(layout.tag)
        .getDisplayLanguage(locale).ifBlank {layout.name}.replaceFirstChar {it.titlecase(locale)}
}
