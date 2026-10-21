package com.lazytalk.keyboard.core.common

enum class AppError(val message: String) {
    UNAUTHORIZED("请到 LingWing Keyboard 设置登录账号；登录失效时请重新登录。"), RATE_LIMITED("翻译请求较多，请稍后重试。"),
    EMAIL_UNVERIFIED("请先打开验证邮件，再到设置点击“我已验证”。"),
    DAILY_QUOTA("今天的免费翻译次数已用完，额度于 UTC 午夜恢复。"),
    SERVICE_QUOTA("今日服务额度已用完，请明天再试。"), TEXT_LIMIT("每次翻译最多 500 字符，请缩短内容。"),
    NETWORK("Network unavailable. Try again."), TIMEOUT("Request timed out. Try again."),
    SERVER("Translation server failed. Try again."), INVALID_RESPONSE("Invalid server response."),
    UNSUPPORTED_LANGUAGE("This language or phrase is unavailable in Mock mode. Use a sample or configure Remote."),
    SECURE_FIELD("Translation unavailable in secure fields."),
    INVALID_CONFIGURATION("Configure an HTTPS Backend URL in Settings."),
    EMPTY_INPUT("Enter a translation draft first."), UNKNOWN("Translation failed. Try again.")
}
class AppException(val error: AppError) : Exception(error.message)
