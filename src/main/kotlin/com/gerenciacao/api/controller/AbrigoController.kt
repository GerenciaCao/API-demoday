package com.gerenciacao.api.controller

import com.gerenciacao.api.dto.AtualizarMembroRequest
import com.gerenciacao.api.dto.ConviteResponse
import com.gerenciacao.api.dto.NovoMembroRequest
import com.gerenciacao.api.dto.PaginaDto
import com.gerenciacao.api.dto.UsuarioDto
import com.gerenciacao.api.security.UsuarioAutenticado
import com.gerenciacao.api.service.EquipeService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/equipe")
class EquipeController(private val equipe: EquipeService) {

    /** GET /api/equipe?page=0&size=10 — lista paginada dos membros do abrigo. */
    @GetMapping
    fun listar(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @PageableDefault(size = 10, sort = ["nome"]) pageable: Pageable,
    ): ResponseEntity<PaginaDto<UsuarioDto>> =
        ResponseEntity.ok(equipe.listar(eu, pageable))

    /** GET /api/equipe/{id} — busca um membro específico pelo ID. */
    @GetMapping("/{id}")
    fun buscarPorId(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @PathVariable("id") id: String,
    ): ResponseEntity<UsuarioDto> =
        ResponseEntity.ok(equipe.buscar(eu, id))

    @PostMapping
    fun convidar(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @Valid @RequestBody req: NovoMembroRequest,
    ): ResponseEntity<ConviteResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(equipe.convidar(eu, req))

    @PutMapping("/{id}")
    fun atualizar(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @PathVariable("id") id: String,
        @Valid @RequestBody req: AtualizarMembroRequest,
    ): ResponseEntity<UsuarioDto> =
        ResponseEntity.ok(equipe.atualizar(eu, id, req))

    @DeleteMapping("/{id}")
    fun remover(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @PathVariable("id") id: String,
    ): ResponseEntity<Void> {
        equipe.remover(eu, id)
        return ResponseEntity.noContent().build()
    }
}