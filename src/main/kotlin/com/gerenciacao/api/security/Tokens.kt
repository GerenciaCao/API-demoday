package com.gerenciacao.api.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Utilidades para tokens de uso único (convites, e futuramente recuperação de senha e validação de e-mail). */
object Tokens {
    private val aleatorio = SecureRandom()

    /** Gera um token aleatório de 256 bits, seguro para ir em links (Base64 URL, sem "="). */
    fun gerarAleatorio(): String {
        val bytes = ByteArray(32)
        aleatorio.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /** Hash SHA-256 (hexadecimal) do token. O banco guarda só o hash, nunca o token. */
    fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
