package com.lazytalk.keyboard.feature.copiedlanguage

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Bundled on-device model. No translation/network client and no clipboard access. */
class LocalSentenceLanguageDetector : SentenceLanguageDetector {
    private var client: LanguageIdentifier? = null
    override suspend fun identify(text: String): List<Pair<String, Float>> = suspendCancellableCoroutine { continuation ->
        val detector = client ?: LanguageIdentification.getClient().also { client = it }
        detector.identifyPossibleLanguages(text.take(200))
            .addOnSuccessListener { results ->
                if (continuation.isActive) continuation.resume(results.map { it.languageTag to it.confidence })
            }.addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    }
    override fun close() { client?.close(); client = null }
}
