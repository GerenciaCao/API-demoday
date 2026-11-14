package com.gerenciacao.api.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/** Cria o "assinador" (encoder) e o "verificador" (decoder) de tokens JWT, com a mesma chave secreta (HS256). */
@Configuration
class JwtConfig(props: JwtProperties) {

    private val chave: SecretKey = SecretKeySpec(props.segredo.toByteArray(Charsets.UTF_8), "HmacSHA256")

    /** Assina os tokens emitidos no login/cadastro. */
    @Bean
    fun jwtEncoder(): JwtEncoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(chave))

    /** Verifica assinatura e validade dos tokens recebidos nas requisições. */
    @Bean
    fun jwtDecoder(): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build()
}
