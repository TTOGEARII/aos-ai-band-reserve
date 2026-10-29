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

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "enezzang-yatte-session")

/**
 * 로그인 결과(JWT + 표시용 이름/이메일)를 기기에 남긴다. 웹의 access_token 쿠키(7일)와 같은 역할.
 * 토큰 만료 판단은 앱이 하지 않는다 — 아무 호출에서 401이 오면 그때 지우고 로그인으로 보낸다.
 */
@Singleton
class SessionStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val tokenKey = stringPreferencesKey("accessToken")
    private val emailKey = stringPreferencesKey("email")
    private val nameKey = stringPreferencesKey("name")

    val session: Flow<AuthSession?> = context.sessionDataStore.data.map { prefs ->
        val token = prefs[tokenKey]
        if (token.isNullOrBlank()) {
            null
        } else {
            AuthSession(token, prefs[emailKey] ?: "", prefs[nameKey] ?: "")
        }
    }

    suspend fun save(session: AuthSession) {
        context.sessionDataStore.edit { prefs ->
            prefs[tokenKey] = session.accessToken
            prefs[emailKey] = session.email
            prefs[nameKey] = session.name
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
