package com.gerenciacao.api.service

import com.gerenciacao.api.domain.StatusUsuario
import com.gerenciacao.api.domain.TipoToken
import com.gerenciacao.api.domain.TokenVerificacao
import com.gerenciacao.api.error.MuitasTentativasException
import com.gerenciacao.api.error.NaoEncontradoException
import com.gerenciacao.api.error.RegraNegocioException
import com.gerenciacao.api.repository.TokenRepository
import com.gerenciacao.api.repository.UsuarioRepository
import com.gerenciacao.api.security.LimitadorTaxa
import com.gerenciacao.api.security.Tokens
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/** Envio de e-mails. Troque a implementação por um provedor real (SMTP, SendGrid, SES...) antes de publicar. */
interface EmailSender {
    /** Envia uma mensagem de texto para [para]. */
    fun enviar(para: String, assunto: String, texto: String)
}

/** Implementação de DESENVOLVIMENTO: não envia e-mail, só escreve no log (para testar os fluxos). NÃO use em produção. */
@Component
class EmailSenderLog : EmailSender {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Escreve o "e-mail" no log do servidor. */
    override fun enviar(para: String, assunto: String, texto: String) {
        log.info("[E-MAIL DE DESENVOLVIMENTO] para={} assunto={}\n{}", para, assunto, texto)
    }
}

/** Recuperar senha (RF03) e validar e-mail (RF04), com códigos de uso único que expiram. */
@Service
class RecuperacaoService(
    private val usuarios: UsuarioRepository,
    private val tokens: TokenRepository,
    private val encoder: PasswordEncoder,
    private val emailSender: EmailSender,
    private val limitador: LimitadorTaxa,
) {
    /**
     * Pede a recuperação de senha. Responde igual exista ou não a conta (não revela e-mails cadastrados);
     * no máximo 3 pedidos por hora para o mesmo e-mail e IP.
     */
    fun esqueciSenha(email: String, ip: String) {
        val e = email.trim().lowercase()
        if (!limitador.registrar("esqueci|$e|$ip", 3, Duration.ofHours(1))) return
        val usuario = usuarios.findByEmail(e)?.takeIf { it.status == StatusUsuario.ATIVO } ?: return
        val codigo = emitir(requireNotNull(usuario.id), TipoToken.RECUPERACAO_SENHA, Duration.ofMinutes(30))
        emailSender.enviar(usuario.email, "Recuperação de senha — GerenciaCão", "Use este código no app para criar uma nova senha (vale por 30 minutos):\n\n$codigo\n\nSe não foi você, ignore esta mensagem.")
    }

    /** Define a nova senha com o código recebido. Tokens de login antigos deixam de valer. */
    fun redefinirSenha(token: String, novaSenha: String, ip: String) {
        if (!limitador.registrar("redefinir|$ip", 10, Duration.ofHours(1))) throw MuitasTentativasException()
        val t = consumir(token, TipoToken.RECUPERACAO_SENHA)
        val usuario = usuarios.findById(t.usuarioId).orElse(null) ?: throw RegraNegocioException("Código inválido ou expirado.")
        usuarios.save(usuario.copy(senhaHash = encoder.encode(novaSenha), senhaAlteradaEm = Instant.now()))
    }

    /** Envia o código de validação do e-mail para o usuário logado (vale 24 horas). */
    fun solicitarValidacaoEmail(usuarioId: String) {
        val usuario = usuarios.findById(usuarioId).orElse(null) ?: throw NaoEncontradoException("Usuário não encontrado.")
        if (usuario.emailVerificado) return
        if (!limitador.registrar("valida|$usuarioId", 3, Duration.ofHours(1))) throw MuitasTentativasException()
        val codigo = emitir(usuarioId, TipoToken.VALIDACAO_EMAIL, Duration.ofHours(24))
        emailSender.enviar(usuario.email, "Confirme seu e-mail — GerenciaCão", "Use este código no app para confirmar o seu e-mail (vale por 24 horas):\n\n$codigo")
    }

    /** Confirma o e-mail com o código recebido. */
    fun validarEmail(token: String) {
        val t = consumir(token, TipoToken.VALIDACAO_EMAIL)
        val usuario = usuarios.findById(t.usuarioId).orElse(null) ?: throw RegraNegocioException("Código inválido ou expirado.")
        usuarios.save(usuario.copy(emailVerificado = true))
    }

    /** Cria um token de uso único para o usuário; devolve o código (o banco guarda só o hash). */
    private fun emitir(usuarioId: String, tipo: TipoToken, validade: Duration): String {
        val codigo = Tokens.gerarAleatorio()
        tokens.save(TokenVerificacao(usuarioId = usuarioId, tipo = tipo, tokenHash = Tokens.hash(codigo), expiraEm = Instant.now().plus(validade)))
        return codigo
    }

    /** Confere o código (tipo certo, não usado, não expirado) e o marca como usado. */
    private fun consumir(token: String, tipo: TipoToken): TokenVerificacao {
        val agora = Instant.now()
        val t = tokens.findByTokenHash(Tokens.hash(token.trim()))?.takeIf { it.tipo == tipo && it.usadoEm == null && it.expiraEm.isAfter(agora) }
            ?: throw RegraNegocioException("Código inválido ou expirado.")
        return tokens.save(t.copy(usadoEm = agora))
    }
}
