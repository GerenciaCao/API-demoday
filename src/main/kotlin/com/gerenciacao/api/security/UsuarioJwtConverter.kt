package com.gerenciacao.api.security

import com.gerenciacao.api.domain.StatusUsuario
import com.gerenciacao.api.repository.UsuarioRepository
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.stereotype.Component

/**
 * Depois que o JWT é validado (assinatura e prazo), esta classe busca o usuário no banco
 * e monta o [UsuarioAutenticado] que os controllers recebem em @AuthenticationPrincipal.
 */
@Component
class UsuarioJwtConverter(private val usuarios: UsuarioRepository) : Converter<Jwt, AbstractAuthenticationToken> {

    /** Carrega o usuário dono do token; se ele não existe mais ou não está ATIVO, o token deixa de valer (401). */
    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val id = jwt.subject ?: throw InvalidBearerTokenException("Token sem usuário.")
        val usuario = usuarios.findById(id).orElse(null)
            ?: throw InvalidBearerTokenException("Usuário não encontrado.")
        if (usuario.status != StatusUsuario.ATIVO) throw InvalidBearerTokenException("Conta inativa.")
        val trocouSenha = usuario.senhaAlteradaEm?.truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        val emitidoEm = jwt.issuedAt
        if (trocouSenha != null && (emitidoEm == null || emitidoEm.isBefore(trocouSenha))) throw InvalidBearerTokenException("Sessão antiga: a senha foi alterada.")

        val principal = UsuarioAutenticado(
            id = id,
            abrigoId = usuario.abrigoId,
            cargo = usuario.cargo,
            permissoes = usuario.permissoes,
        )
        val autoridades = usuario.permissoes.map { SimpleGrantedAuthority("PERM_${it.name}") } +
            SimpleGrantedAuthority("CARGO_${usuario.cargo.name}")
        return UsernamePasswordAuthenticationToken(principal, jwt, autoridades)
    }
}
