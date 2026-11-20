package com.gerenciacao.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Quem administra a PLATAFORMA (cria os anúncios de Parcerias): e-mails em "gerenciacao.plataforma.admins" (variável PLATAFORMA_ADMINS, separados por vírgula). */
@ConfigurationProperties(prefix = "gerenciacao.plataforma")
data class PlataformaProperties(val admins: List<String> = emptyList()) {
    /** Confere se o e-mail é de um administrador da plataforma. */
    fun ehAdmin(email: String): Boolean = admins.any { it.isNotBlank() && it.trim().equals(email.trim(), ignoreCase = true) }
}
