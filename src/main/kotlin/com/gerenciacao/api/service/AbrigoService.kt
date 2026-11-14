package com.gerenciacao.api.service

import com.gerenciacao.api.dto.AbrigoDto
import com.gerenciacao.api.dto.AtualizarAbrigoRequest
import com.gerenciacao.api.dto.paraDto
import com.gerenciacao.api.domain.Abrigo
import com.gerenciacao.api.error.NaoEncontradoException
import com.gerenciacao.api.repository.AbrigoRepository
import com.gerenciacao.api.repository.AnimalRefRepository
import com.gerenciacao.api.security.UsuarioAutenticado
import org.springframework.stereotype.Service

/** Informações do abrigo do usuário logado (tela "Informações do abrigo", RF08). */
@Service
class AbrigoService(private val abrigos: AbrigoRepository, private val animais: AnimalRefRepository) {

    /** Devolve os dados do abrigo. Qualquer membro pode ver. */
    fun buscar(eu: UsuarioAutenticado): AbrigoDto = carregar(eu).let { it.paraDto(contarAnimais(it)) }

    /** Edita o abrigo (só administradores e gestores). Só os campos enviados são alterados. */
    fun atualizar(eu: UsuarioAutenticado, req: AtualizarAbrigoRequest): AbrigoDto {
        eu.exigirGestao()
        val atual = carregar(eu)
        val salvo = abrigos.save(
            atual.copy(
                nome = req.nome?.trim()?.takeIf { it.isNotEmpty() } ?: atual.nome,
                descricao = req.descricao?.trim() ?: atual.descricao,
                telefone = req.telefone?.trim() ?: atual.telefone,
                email = req.email?.trim()?.lowercase() ?: atual.email,
                endereco = req.endereco?.trim() ?: atual.endereco,
                latitude = req.latitude ?: atual.latitude,
                longitude = req.longitude ?: atual.longitude,
                redesSociais = req.redesSociais?.trim() ?: atual.redesSociais,
                capacidade = req.capacidade ?: atual.capacidade,
            ),
        )
        return salvo.paraDto(contarAnimais(salvo))
    }

    /** Quantos animais ativos o abrigo tem (ocupam vaga). */
    private fun contarAnimais(abrigo: Abrigo): Int =
        animais.countByAbrigoIdAndAtivo(requireNotNull(abrigo.id), true).toInt()

    /** Carrega o abrigo do usuário logado. */
    private fun carregar(eu: UsuarioAutenticado): Abrigo =
        abrigos.findById(eu.abrigoId).orElse(null) ?: throw NaoEncontradoException("Abrigo não encontrado.")
}
