package com.kanenashitogeari.bandroom.data

import java.util.Base64
import kotlin.math.abs

/**
 * 서버·웹과 동일한 커스텀 XOR + Base64 암호화.
 * 백엔드 `src/common/utils/crypto.util.ts` ↔ 프론트 `utils/crypto.ts`의 포팅이라
 * **비트 단위로 같은 결과**를 내야 한다 — 다르면 로그인부터 전부 깨진다.
 * 호환성은 [CryptoTest]가 JS로 만든 고정 벡터로 못 박는다.
 */
object Crypto {

    fun encrypt(plain: String, key: String): String {
        val keyBytes = keyBytes(key)
        val data = plain.toByteArray(Charsets.UTF_8)
        val out = ByteArray(data.size) { i ->
            (data[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return Base64.getEncoder().encodeToString(out)
    }

    fun decrypt(encoded: String, key: String): String {
        val keyBytes = keyBytes(key)
        val data = Base64.getDecoder().decode(encoded)
        val out = ByteArray(data.size) { i ->
            (data[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return String(out, Charsets.UTF_8)
    }

    /**
     * JS `simpleHash`의 재현. 주의할 점 두 가지:
     * - JS는 `hash & hash`로 32비트 정수로 접는다 → Kotlin Int 연산이 똑같이 2^32 모듈러라 식 그대로 옮기면 된다.
     * - `Math.abs(-2147483648)`는 JS에선 2147483648(double)이지만 Kotlin `abs(Int)`는 오버플로우라 Long으로 올려서 받는다.
     * 결과는 32자 hex(= 16바이트 키)다.
     */
    private fun keyBytes(key: String): ByteArray {
        var hash = 0
        for (ch in key) hash = (hash shl 5) - hash + ch.code
        val hex = abs(hash.toLong()).toString(16).padStart(32, '0')
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
