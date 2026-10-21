package com.lazytalk.keyboard.feature.settings.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.lazytalk.keyboard.ui.uiText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lazytalk.keyboard.R
import com.lazytalk.keyboard.core.auth.AccountState

@Composable fun AccountScreen(model: SettingsViewModel, onBack: () -> Unit) {
    val account by model.account.collectAsState()
    val busy by model.authBusy.collectAsState()
    val message by model.authMessage.collectAsState()
    val context=LocalContext.current
    BackHandler(onBack=onBack)
    AccountContent(account,busy,message,model::signIn,{model.googleSignIn(context)},model::verifyEmail,
        model::refreshAccount,model::resetPassword,{model.signOut(context)},onBack)
}

@Composable fun AccountMark(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(painterResource(R.drawable.ic_lingwing), "LingWing Keyboard", modifier)
}

@Composable fun AccountContent(account: AccountState, busy: Boolean, message: String?,
    onEmail: (String,String,Boolean)->Unit, onGoogle: ()->Unit, onVerify: ()->Unit,
    onRefresh: ()->Unit, onReset: (String)->Unit, onSignOut: ()->Unit, onBack: ()->Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    LaunchedEffect(account.email) { if(account.email.isNotBlank()) password="" }
    val colors=MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(),color=colors.background) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onBack) {
                    Canvas(Modifier.size(23.dp).semantics { contentDescription="返回设置" }) {
                        val s=size.width
                        drawLine(colors.onSurface,Offset(s*.78f,s*.5f),Offset(s*.2f,s*.5f),2.dp.toPx())
                        drawLine(colors.onSurface,Offset(s*.2f,s*.5f),Offset(s*.47f,s*.23f),2.dp.toPx())
                        drawLine(colors.onSurface,Offset(s*.2f,s*.5f),Offset(s*.47f,s*.77f),2.dp.toPx())
                    }
                }
                Text(uiText("LingWing Keyboard"),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            }
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp),contentAlignment=Alignment.TopCenter) {
                Column(Modifier.widthIn(max=420.dp).fillMaxWidth().padding(top=8.dp,bottom=20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    AccountMark(Modifier.size(52.dp))
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(uiText(if(account.email.isNotBlank()) "我的账号" else if(register) "创建你的账号" else "欢迎使用 LingWing Keyboard"),
                            fontSize=26.sp,lineHeight=32.sp,fontWeight=FontWeight.SemiBold)
                        Text(uiText(if(account.email.isNotBlank()) account.email else "让每一种语言，都聊得自然。"),
                            style=MaterialTheme.typography.bodyLarge,color=colors.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
                    }
                    if(account.email.isBlank()) {
                        OutlinedButton(onClick=onGoogle,enabled=!busy,modifier=Modifier.fillMaxWidth().height(54.dp),
                            shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,colors.outlineVariant)) {
                            Icon(painterResource(R.drawable.ic_google_g),null,Modifier.size(20.dp),tint=Color.Unspecified)
                            Spacer(Modifier.width(12.dp));Text(uiText("使用 Google 账号继续"),fontWeight=FontWeight.Medium,color=colors.onSurface)
                        }
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                            HorizontalDivider(Modifier.weight(1f),color=colors.outlineVariant)
                            Text(uiText("或使用邮箱"),style=MaterialTheme.typography.labelMedium,color=colors.onSurfaceVariant)
                            HorizontalDivider(Modifier.weight(1f),color=colors.outlineVariant)
                        }
                        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(email,{email=it},label={Text(uiText("邮箱地址"))},placeholder={Text(uiText("name@example.com"))},
                                singleLine=true,shape=RoundedCornerShape(16.dp),enabled=!busy,
                                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email,imeAction=ImeAction.Next),modifier=Modifier.fillMaxWidth())
                            OutlinedTextField(password,{password=it},label={Text(uiText("密码"))},singleLine=true,
                                supportingText=if(register) {{Text(uiText("至少 8 位字符"))}} else null,
                                visualTransformation=if(showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon={ IconButton(onClick={showPassword=!showPassword}) {
                                    Canvas(Modifier.size(22.dp).semantics {contentDescription=if(showPassword) "隐藏密码" else "显示密码"}) {
                                        drawOval(colors.onSurfaceVariant,Offset(0f,size.height*.2f),Size(size.width,size.height*.6f),style=Stroke(1.7.dp.toPx()))
                                        drawCircle(colors.onSurfaceVariant,size.width*.13f)
                                        if(!showPassword) drawLine(colors.onSurfaceVariant,Offset(0f,size.height),Offset(size.width,0f),1.7.dp.toPx())
                                    }
                                } },shape=RoundedCornerShape(16.dp),enabled=!busy,
                                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=ImeAction.Done),modifier=Modifier.fillMaxWidth())
                        }
                        if(!register) TextButton(onClick={onReset(email.trim())},enabled=!busy && email.contains('@'),
                            modifier=Modifier.align(Alignment.End),contentPadding=PaddingValues(horizontal=0.dp)) {Text(uiText("忘记密码？"))}
                        Button(onClick={onEmail(email.trim(),password,register)},enabled=!busy && email.contains('@') && password.length>=8,
                            modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(16.dp)) {
                            Text(uiText(if(register) "创建账号" else "登录"),fontSize=16.sp,fontWeight=FontWeight.SemiBold)
                        }
                        TextButton(onClick={register=!register},enabled=!busy,modifier=Modifier.align(Alignment.CenterHorizontally)) {
                            Text(uiText(if(register) "已有账号？立即登录" else "还没有账号？免费注册"))
                        }
                    } else {
                        Surface(shape=RoundedCornerShape(20.dp),color=colors.primaryContainer,modifier=Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Text(uiText(if(account.unlimited) "免费 · 不限翻译次数" else "免费方案"),color=colors.onPrimaryContainer,
                                    style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
                                if(account.unlimited) Text(uiText("尽情表达，自在交流。"),color=colors.onPrimaryContainer)
                                else {
                                    Text(uiText(account.remaining?.let {"$it"} ?: "—"),fontSize=42.sp,fontWeight=FontWeight.SemiBold,color=colors.onPrimaryContainer)
                                    Text(uiText(account.remaining?.let {"今日剩余 / ${account.limit} 次"} ?: "正在获取额度"),color=colors.onPrimaryContainer)
                                }
                            }
                        }
                        Text(uiText(if(account.unlimited) "每次最多 500 字符。" else "每次最多 500 字符。额度每日早上 8 点（UTC+8）恢复。"),
                            style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
                        if(!account.verified) {
                            Text(uiText("验证邮箱，即可开启翻译"),style=MaterialTheme.typography.titleMedium)
                            Text(uiText("打开验证邮件中的链接，然后回来确认。"),color=colors.onSurfaceVariant)
                            Button(onClick=onRefresh,enabled=!busy,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp)) {Text(uiText("我已验证邮箱"))}
                            TextButton(onClick=onVerify,enabled=!busy) {Text(uiText("重新发送验证邮件"))}
                        } else OutlinedButton(onClick=onRefresh,enabled=!busy,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp)) {Text(uiText("刷新账号状态"))}
                        TextButton(onClick=onSignOut,enabled=!busy,modifier=Modifier.align(Alignment.CenterHorizontally)) {Text(uiText("退出登录"),color=colors.onSurfaceVariant)}
                    }
                    if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    message?.let { Surface(shape=RoundedCornerShape(14.dp),color=colors.secondaryContainer) {
                        Text(uiText(it),Modifier.padding(14.dp),style=MaterialTheme.typography.bodyMedium,color=colors.onSecondaryContainer)
                    } }
                    Text(uiText("普通输入无需登录。仅在使用翻译时发送翻译内容。"),style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
                }
            }
        }
    }
}
