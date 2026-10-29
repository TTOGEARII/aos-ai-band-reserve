package com.enezzang.yatte

import com.enezzang.yatte.data.Crypto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 서버·웹의 JS 구현과 비트 단위 호환을 못 박는다.
 * 고정 벡터는 백엔드 crypto.util.ts 코드를 node로 실행해 얻은 값이다 —
 * 여기가 깨지면 로그인부터 모든 암호화 API가 서버에서 복호화 실패한다.
 */
class CryptoTest {

    private val defaultKey = "subculture-ground-encryption-key-2024"

    @Test
    fun `JS 구현과 같은 암호문 - ASCII JSON`() {
        val plain = """{"email":"test@example.com","password":"secret123"}"""
        assertEquals(
            "eyJlbWFpbCI6InRlb+PAqnhhbXBsZS5jb20iLD7n4bxzd29yZCI6InNlY3J547H9MyJ9",
            Crypto.encrypt(plain, defaultKey),
        )
    }

    @Test
    fun `JS 구현과 같은 암호문 - 한글(UTF-8 멀티바이트)`() {
        // 키 해시가 16바이트 주기로 XOR되므로, 멀티바이트 경계가 어긋나면 여기서 바로 드러난다.
        assertEquals(
            "7JWI64WV7ZWY7IS48A0U7+2VqeyjvOyLpCDsmJR7FXI=",
            Crypto.encrypt("안녕하세요 합주실 예약", defaultKey),
        )
    }

    @Test
    fun `암호화-복호화 왕복`() {
        val samples = listOf(
            """{"notionToken":"secret_abc","geminiKey":"AIza..."}""",
            "한글과 English 혼용, 이모지 🎸🥁 포함",
            "", // 빈 문자열도 깨지지 않아야 한다
        )
        for (sample in samples) {
            assertEquals(sample, Crypto.decrypt(Crypto.encrypt(sample, defaultKey), defaultKey))
        }
    }

    @Test
    fun `다른 키로 암호화하면 다른 암호문`() {
        val plain = "same plain text"
        val a = Crypto.encrypt(plain, defaultKey)
        val b = Crypto.encrypt(plain, "another-key")
        // 키가 다른데 암호문이 같다면 keyBytes 계산이 키를 무시하고 있는 것
        assert(a != b)
        assertEquals(plain, Crypto.decrypt(b, "another-key"))
    }
}
