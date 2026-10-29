package com.enezzang.yatte.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.chatDataStore: DataStore<Preferences> by preferencesDataStore(name = "enezzang-yatte-chat")

/**
 * 대화기록·선택 모델을 기기에 보존한다. 웹의 localStorage("notion-agent:chat" / "notion-agent:model") 대응 —
 * 앱을 껐다 켜도 대화가 이어진다. 저장이 깨져 있으면(버전 변경 등) 빈 대화로 시작하고 에러는 내지 않는다.
 */
@Singleton
class ChatStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val messagesKey = stringPreferencesKey("messages")
    private val modelKey = stringPreferencesKey("model")

    val messages: Flow<List<StoredMessage>> = context.chatDataStore.data.map { prefs ->
        val raw = prefs[messagesKey] ?: return@map emptyList()
        runCatching {
            EnezzangYatteNetwork.json.decodeFromString<List<StoredMessage>>(raw)
        }.getOrDefault(emptyList())
    }

    val model: Flow<String?> = context.chatDataStore.data.map { prefs -> prefs[modelKey] }

    suspend fun saveMessages(messages: List<StoredMessage>) {
        context.chatDataStore.edit { prefs ->
            prefs[messagesKey] = EnezzangYatteNetwork.json.encodeToString(messages)
        }
    }

    suspend fun saveModel(model: String) {
        context.chatDataStore.edit { prefs -> prefs[modelKey] = model }
    }

    suspend fun clearMessages() {
        context.chatDataStore.edit { prefs -> prefs.remove(messagesKey) }
    }
}
