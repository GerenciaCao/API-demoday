package com.gerenciacao.api.security

import com.gerenciacao.api.config.JwtProperties
import com.gerenciacao.api.domain.Usuario
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Token pronto para enviar ao app e o instante em que ele expira. */
data class TokenGerado(val valor: String, val expiraEm: Instant)

/** Emite os tokens JWT de login. O token guarda só o id do usuário; o resto é lido do banco a cada requisição. */
@Service
class TokenService(private val encoder: JwtEncoder, private val props: JwtProperties) {

    /** Gera um JWT assinado (HS256) para o usuário, válido por "expiracao-horas". */
    fun gerar(usuario: Usuario): TokenGerado {
        val agora = Instant.now()
        val expira = agora.plus(props.expiracaoHoras, ChronoUnit.HOURS)
        val claims = JwtClaimsSet.builder()
            .issuer("gerenciacao-api")
            .issuedAt(agora)
            .expiresAt(expira)
            .subject(requireNotNull(usuario.id) { "Usuário sem id" })
            .build()
        val cabecalho = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).tokenValue
        return TokenGerado(token, expira)
    }
}
