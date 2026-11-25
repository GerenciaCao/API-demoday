package com.gerenciacao.api.service

import com.gerenciacao.api.dto.AbrigoDto
import com.gerenciacao.api.dto.AtualizarAbrigoRequest
import com.gerenciacao.api.dto.PaginaDto
import com.gerenciacao.api.dto.paraDto
import com.gerenciacao.api.dto.paraPaginaDto
import com.gerenciacao.api.error.NaoEncontradoException
import com.gerenciacao.api.repository.AbrigoRepository
import com.gerenciacao.api.security.UsuarioAutenticado
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class AbrigoService(
    private val abrigos: AbrigoRepository,
) {

    fun listarTodos(pageable: Pageable): PaginaDto<AbrigoDto> {
        val tamanhoSeguro = pageable.pageSize.coerceIn(1, 50)
        val paginaSegura = PageRequest.of(
            pageable.pageNumber.coerceAtLeast(0),
            tamanhoSeguro,
            pageable.sort,
        )
        return abrigos.findAll(paginaSegura)
            .map { it.paraDto() }
            .paraPaginaDto()
    }

    fun buscarPorId(id: String): AbrigoDto =
        abrigos.findById(id)
            .orElseThrow { NaoEncontradoException("Abrigo não encontrado.") }
            .paraDto()

    fun buscar(eu: UsuarioAutenticado): AbrigoDto =
        abrigos.findById(eu.abrigoId)
            .orElseThrow { NaoEncontradoException("Abrigo não encontrado.") }
            .paraDto()

    fun atualizar(eu: UsuarioAutenticado, req: AtualizarAbrigoRequest): AbrigoDto {
        val atual = abrigos.findById(eu.abrigoId)
            .orElseThrow { NaoEncontradoException("Abrigo não encontrado.") }
        // Mantenha aqui sua lógica de permissão/atualização existente para `atual` e `req`
        val salvo = abrigos.save(atual)
        return salvo.paraDto()
    }
}
