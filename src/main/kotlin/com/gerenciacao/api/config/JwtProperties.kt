package com.gerenciacao.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configurações do token de login, lidas de "gerenciacao.jwt.*" no application.yml.
 * O segredo vem da variável de ambiente JWT_SEGREDO.
 */
@ConfigurationProperties(prefix = "gerenciacao.jwt")
data class JwtProperties(
    val segredo: String,
    val expiracaoHoras: Long = 12,
) {
    init {
        // HS256 exige chave com pelo menos 256 bits (32 bytes)
        require(segredo.toByteArray(Charsets.UTF_8).size >= 32) {
            "JWT_SEGREDO precisa ter pelo menos 32 caracteres."
        }
    }
}
