package com.gerenciacao.api.service

import com.gerenciacao.api.domain.Abrigo
import com.gerenciacao.api.domain.Cargo
import com.gerenciacao.api.domain.StatusUsuario
import com.gerenciacao.api.domain.Usuario
import com.gerenciacao.api.dto.AceitarConviteRequest
import com.gerenciacao.api.dto.AuthResponse
import com.gerenciacao.api.dto.CadastroRequest
import com.gerenciacao.api.dto.LoginRequest
import com.gerenciacao.api.dto.UsuarioDto
import com.gerenciacao.api.dto.paraDto
import com.gerenciacao.api.error.ConflitoException
import com.gerenciacao.api.error.MuitasTentativasException
import com.gerenciacao.api.error.CredenciaisInvalidasException
import com.gerenciacao.api.error.NaoEncontradoException
import com.gerenciacao.api.error.RegraNegocioException
import com.gerenciacao.api.repository.AbrigoRepository
import com.gerenciacao.api.repository.UsuarioRepository
import com.gerenciacao.api.security.LimitadorTaxa
import com.gerenciacao.api.security.TokenService
import com.gerenciacao.api.security.Tokens
import org.springframework.dao.DuplicateKeyException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/** Cadastro, login e aceite de convite (RF01 e RF02). */
@Service
class AuthService(
    private val usuarios: UsuarioRepository,
    private val abrigos: AbrigoRepository,
    private val encoder: PasswordEncoder,
    private val tokens: TokenService,
    private val limitador: LimitadorTaxa,
) {
    /** Hash de uma senha qualquer, usado para o login gastar o mesmo tempo mesmo quando o e-mail não existe. */
    private val hashFalso: String by lazy { requireNotNull(encoder.encode("senha-que-ninguem-usa")) }

    /**
     * Cria a conta (RF01). Cada conta nova ganha o seu próprio abrigo e vira ADMINISTRADOR dele,
     * com todas as permissões. Já devolve o token, então o app entra direto depois do cadastro.
     */
    fun cadastrar(req: CadastroRequest, ip: String): AuthResponse {
        // No máximo 10 cadastros por hora por endereço de IP
        if (!limitador.registrar("cadastro|$ip", 10, Duration.ofHours(1))) throw MuitasTentativasException()
        val email = req.email.trim().lowercase()
        if (usuarios.existsByEmail(email)) throw ConflitoException("Já existe uma conta com esse e-mail.")

        // "Maria da Silva" -> nome "Maria", sobrenome "da Silva"
        val partes = req.nome.trim().split(Regex("\\s+"), limit = 2)
        val nome = partes[0]
        val sobrenome = partes.getOrElse(1) { "" }

        val abrigo = abrigos.save(Abrigo(nome = "Abrigo de $nome"))
        val usuario = try {
            usuarios.save(
                Usuario(
                    abrigoId = requireNotNull(abrigo.id),
                    nome = nome,
                    sobrenome = sobrenome,
                    email = email,
                    telefone = req.telefone.trim(),
                    senhaHash = encoder.encode(req.senha),
                    cargo = Cargo.ADMINISTRADOR,
                    permissoes = Cargo.ADMINISTRADOR.permissoesPadrao(),
                    status = StatusUsuario.ATIVO,
                ),
            )
        } catch (e: DuplicateKeyException) {
            abrigos.delete(abrigo) // desfaz o abrigo criado se o usuário não pôde ser salvo
            throw ConflitoException("Já existe uma conta com esse e-mail.")
        } catch (e: RuntimeException) {
            abrigos.delete(abrigo)
            throw e
        }
        return respostaDeLogin(usuario)
    }

    /**
     * Entra com e-mail e senha (RF02). Para qualquer falha (e-mail inexistente, senha errada,
     * conta não ativa) a resposta é a mesma, para não revelar quais e-mails têm conta.
     */
    fun login(req: LoginRequest, ip: String): AuthResponse {
        val email = req.email.trim().lowercase()
        // Depois de 5 erros em 15 minutos (mesmo e-mail + mesmo IP), bloqueia novas tentativas por um tempo
        val chave = "login|$email|$ip"
        if (limitador.excedeu(chave, MAX_FALHAS, JANELA_FALHAS)) throw MuitasTentativasException()

        val usuario = usuarios.findByEmail(email)
        val hash = usuario?.senhaHash
        val senhaConfere = encoder.matches(req.senha, hash ?: hashFalso)
        if (usuario == null || hash == null || !senhaConfere || usuario.status != StatusUsuario.ATIVO) {
            limitador.registrar(chave, MAX_FALHAS, JANELA_FALHAS)
            throw CredenciaisInvalidasException()
        }
        limitador.limpar(chave)
        return respostaDeLogin(usuario)
    }

    /** Conclui um convite (RF06): confere o token, grava a senha escolhida, ativa a conta e já faz o login. */
    fun aceitarConvite(req: AceitarConviteRequest): AuthResponse {
        val usuario = usuarios.findByConviteTokenHash(Tokens.hash(req.token.trim()))
            ?: throw RegraNegocioException("Convite inválido ou já utilizado.")
        val expiraEm = usuario.conviteExpiraEm
        if (usuario.status != StatusUsuario.CONVIDADO || expiraEm == null || expiraEm.isBefore(Instant.now())) {
            throw RegraNegocioException("Convite inválido ou expirado. Peça um novo convite.")
        }
        val ativado = usuarios.save(
            usuario.copy(
                senhaHash = encoder.encode(req.senha),
                status = StatusUsuario.ATIVO,
                conviteTokenHash = null,
                conviteExpiraEm = null,
            ),
        )
        return respostaDeLogin(ativado)
    }

    /** Devolve os dados do usuário logado (tela de perfil / "Minha Conta"). */
    fun eu(id: String): UsuarioDto =
        usuarios.findById(id).orElse(null)?.paraDto() ?: throw NaoEncontradoException("Usuário não encontrado.")

    private companion object {
        const val MAX_FALHAS = 5
        val JANELA_FALHAS: Duration = Duration.ofMinutes(15)
    }

    /** Monta a resposta com o token JWT e os dados do usuário. */
    private fun respostaDeLogin(usuario: Usuario): AuthResponse {
        val token = tokens.gerar(usuario)
        return AuthResponse(token = token.valor, expiraEm = token.expiraEm, usuario = usuario.paraDto())
    }
}
