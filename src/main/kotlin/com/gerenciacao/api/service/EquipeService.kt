package com.gerenciacao.api.service

import com.gerenciacao.api.domain.Cargo
import com.gerenciacao.api.domain.Permissao
import com.gerenciacao.api.domain.StatusUsuario
import com.gerenciacao.api.domain.Usuario
import com.gerenciacao.api.dto.AtualizarMembroRequest
import com.gerenciacao.api.dto.ConviteResponse
import com.gerenciacao.api.dto.NovoMembroRequest
import com.gerenciacao.api.dto.UsuarioDto
import com.gerenciacao.api.dto.paraDto
import com.gerenciacao.api.error.AcessoNegadoException
import com.gerenciacao.api.error.ConflitoException
import com.gerenciacao.api.error.NaoEncontradoException
import com.gerenciacao.api.error.RegraNegocioException
import com.gerenciacao.api.repository.UsuarioRepository
import com.gerenciacao.api.security.Tokens
import com.gerenciacao.api.security.UsuarioAutenticado
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Gestão da equipe do abrigo: listar, convidar, editar e remover membros (RF05, RF06 e RF07). */
@Service
class EquipeService(private val usuarios: UsuarioRepository) {

    /** Lista os membros do abrigo do usuário logado, em ordem alfabética. Qualquer membro pode ver. */
    fun listar(eu: UsuarioAutenticado): List<UsuarioDto> =
        usuarios.findByAbrigoId(eu.abrigoId)
            .filter { it.status != StatusUsuario.INATIVO }
            .sortedBy { it.nome.lowercase() }
            .map { it.paraDto() }

    /** Busca um membro do mesmo abrigo (de outro abrigo responde 404, como se não existisse). */
    fun buscar(eu: UsuarioAutenticado, id: String): UsuarioDto = carregar(eu, id).paraDto()

    /**
     * Convida um novo membro (RF06). Cria a conta como CONVIDADO, sem senha, e devolve um token de convite
     * (válido por 7 dias) que o convidado usa em /api/auth/aceitar-convite. Exige a permissão MODIFICAR_EQUIPE.
     */
    fun convidar(eu: UsuarioAutenticado, req: NovoMembroRequest): ConviteResponse {
        eu.exigir(Permissao.MODIFICAR_EQUIPE)
        val cargo = requireNotNull(req.cargo) { "Cargo obrigatório" }
        if (cargo == Cargo.ADMINISTRADOR && eu.cargo != Cargo.ADMINISTRADOR) {
            throw AcessoNegadoException("Somente administradores podem criar outros administradores.")
        }
        val permissoes = req.permissoes ?: cargo.permissoesPadrao()
        validarPermissoesConcedidas(eu, permissoes)

        val email = req.email.trim().lowercase()
        if (usuarios.existsByEmail(email)) throw ConflitoException("Já existe uma conta com esse e-mail.")

        val token = Tokens.gerarAleatorio()
        val expiraEm = Instant.now().plus(7, ChronoUnit.DAYS)
        val membro = try {
            usuarios.save(
                Usuario(
                    abrigoId = eu.abrigoId,
                    nome = req.nome.trim(),
                    sobrenome = req.sobrenome.trim(),
                    email = email,
                    telefone = req.telefone.trim(),
                    endereco = req.endereco.trim(),
                    senhaHash = null,
                    cargo = cargo,
                    permissoes = permissoes,
                    status = StatusUsuario.CONVIDADO,
                    conviteTokenHash = Tokens.hash(token),
                    conviteExpiraEm = expiraEm,
                ),
            )
        } catch (e: DuplicateKeyException) {
            throw ConflitoException("Já existe uma conta com esse e-mail.")
        }
        // TODO: enviar o convite por e-mail e remover o token desta resposta
        return ConviteResponse(membro = membro.paraDto(), conviteToken = token, conviteExpiraEm = expiraEm)
    }

    /**
     * Edita um membro (tela "Editar Perfil"). Cada um pode editar os próprios dados pessoais;
     * editar outra pessoa, mudar cargo ou permissões exige MODIFICAR_EQUIPE.
     * Só administradores mexem em administradores, e o abrigo nunca fica sem um.
     */
    fun atualizar(eu: UsuarioAutenticado, id: String, req: AtualizarMembroRequest): UsuarioDto {
        val alvo = carregar(eu, id)
        val ehProprio = alvo.id == eu.id

        if (!ehProprio) eu.exigir(Permissao.MODIFICAR_EQUIPE)
        if (alvo.cargo == Cargo.ADMINISTRADOR && eu.cargo != Cargo.ADMINISTRADOR) {
            throw AcessoNegadoException("Somente administradores podem alterar um administrador.")
        }

        val mudaCargo = req.cargo != null && req.cargo != alvo.cargo
        if (mudaCargo || req.permissoes != null) eu.exigir(Permissao.MODIFICAR_EQUIPE)

        val novoCargo = req.cargo ?: alvo.cargo
        if (mudaCargo) {
            if (novoCargo == Cargo.ADMINISTRADOR && eu.cargo != Cargo.ADMINISTRADOR) {
                throw AcessoNegadoException("Somente administradores podem promover alguém a administrador.")
            }
            if (alvo.cargo == Cargo.ADMINISTRADOR) garantirOutroAdministrador(alvo)
        }
        req.permissoes?.let { validarPermissoesConcedidas(eu, it) }

        // Mudou de cargo e não mandou permissões: usa as padrão do novo cargo
        val novasPermissoes = req.permissoes ?: if (mudaCargo) novoCargo.permissoesPadrao() else alvo.permissoes

        var novoEmail = alvo.email
        req.email?.trim()?.lowercase()?.let { informado ->
            if (informado.isNotEmpty() && informado != alvo.email) {
                if (usuarios.existsByEmail(informado)) throw ConflitoException("Já existe uma conta com esse e-mail.")
                novoEmail = informado
            }
        }

        val atualizado = try {
            usuarios.save(
                alvo.copy(
                    nome = req.nome?.trim()?.takeIf { it.isNotEmpty() } ?: alvo.nome,
                    sobrenome = req.sobrenome?.trim() ?: alvo.sobrenome,
                    email = novoEmail,
                    telefone = req.telefone?.trim() ?: alvo.telefone,
                    endereco = req.endereco?.trim() ?: alvo.endereco,
                    cargo = novoCargo,
                    permissoes = novasPermissoes,
                ),
            )
        } catch (e: DuplicateKeyException) {
            throw ConflitoException("Já existe uma conta com esse e-mail.")
        }
        return atualizado.paraDto()
    }

    /**
     * Remove um membro (RF07 e botão "Remover Conta"). Cada um pode apagar a própria conta;
     * apagar outra pessoa exige MODIFICAR_EQUIPE. O último administrador não pode ser removido.
     */
    fun remover(eu: UsuarioAutenticado, id: String) {
        val alvo = carregar(eu, id)
        if (alvo.id != eu.id) eu.exigir(Permissao.MODIFICAR_EQUIPE)
        if (alvo.cargo == Cargo.ADMINISTRADOR) {
            if (eu.cargo != Cargo.ADMINISTRADOR) {
                throw AcessoNegadoException("Somente administradores podem remover um administrador.")
            }
            garantirOutroAdministrador(alvo)
        }
        usuarios.delete(alvo)
    }

    /** Carrega um membro garantindo que ele é do mesmo abrigo de quem pediu (senão, 404). */
    private fun carregar(eu: UsuarioAutenticado, id: String): Usuario =
        usuarios.findById(id).orElse(null)?.takeIf { it.abrigoId == eu.abrigoId }
            ?: throw NaoEncontradoException("Membro não encontrado.")

    /** Quem não é administrador só pode conceder permissões que ele mesmo tem. */
    private fun validarPermissoesConcedidas(eu: UsuarioAutenticado, permissoes: Set<Permissao>) {
        if (eu.cargo != Cargo.ADMINISTRADOR && !eu.permissoes.containsAll(permissoes)) {
            throw AcessoNegadoException("Você não pode conceder permissões que não possui.")
        }
    }

    /** Impede deixar o abrigo sem nenhum administrador ativo ou convidado. */
    private fun garantirOutroAdministrador(alvo: Usuario) {
        val outros = usuarios.findByAbrigoId(alvo.abrigoId).count {
            it.cargo == Cargo.ADMINISTRADOR && it.id != alvo.id && it.status != StatusUsuario.INATIVO
        }
        if (outros == 0) {
            throw RegraNegocioException("O abrigo precisa ter pelo menos um administrador. Passe o cargo para outra pessoa antes.")
        }
    }
}
