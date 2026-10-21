package com.lazytalk.keyboard.core.data
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.lazytalk.keyboard.core.data.settings.DataStoreSettingsRepository
import com.lazytalk.keyboard.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
class SettingsPersistenceTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun survivesReopeningActualDataStore() = runTest {
        val file = temp.root.resolve("settings.preferences_pb")
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
        val repo = DataStoreSettingsRepository(store)
        val expected = UserSettings("ms", Tone.POLITE, setOf("iban", "ms"), listOf("ms", "iban"), TranslationMode.REMOTE, "https://example.com", "th", "lt_test_persisted_access_code", keyVibration=false,keyVibrationStrength=KeyVibrationStrength.STRONG)
        assertTrue(repo.settings.first().keyVibration)
        repo.update { expected }
        assertEquals(expected, repo.settings.first())
        job.cancelAndJoin()
        val secondJob = SupervisorJob()
        try {
            val reopened = DataStoreSettingsRepository(PreferenceDataStoreFactory.create(scope = CoroutineScope(secondJob + Dispatchers.IO)) { file })
            assertEquals(expected, reopened.settings.first())
        } finally { secondJob.cancelAndJoin() }
    }
}
