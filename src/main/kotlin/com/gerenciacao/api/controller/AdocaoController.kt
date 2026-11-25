package com.gerenciacao.api.controller

import com.gerenciacao.api.domain.StatusAdocao
import com.gerenciacao.api.dto.AdocaoDto
import com.gerenciacao.api.dto.AvancarEtapaRequest
import com.gerenciacao.api.dto.CancelarAdocaoRequest
import com.gerenciacao.api.dto.ConfirmarVisitaRequest
import com.gerenciacao.api.dto.NovaAdocaoRequest
import com.gerenciacao.api.dto.PreencherFormularioRequest
import com.gerenciacao.api.security.UsuarioAutenticado
import com.gerenciacao.api.service.AdocaoService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** Processo de adoção. Todas as rotas exigem login (regra geral do SecurityConfig). */
@RestController
@RequestMapping("/api/adocoes")
class AdocaoController(private val adocao: AdocaoService) {

    /** GET /api/adocoes?status=EM_ANDAMENTO — lista os processos (sem "status" devolve todos). */
    @GetMapping
    fun listar(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @RequestParam("status", required = false) status: StatusAdocao?,
    ): List<AdocaoDto> = adocao.listar(eu, status)

    /** GET /api/adocoes/{id} — um processo (tela "Status do pet"). */
    @GetMapping("/{id}")
    fun buscar(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String): AdocaoDto =
        adocao.buscar(eu, id)

    /** POST /api/adocoes — inicia um processo (interesse registrado). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun criar(@AuthenticationPrincipal eu: UsuarioAutenticado, @Valid @RequestBody req: NovaAdocaoRequest): AdocaoDto =
        adocao.criar(eu, req)

    /** PUT /api/adocoes/{id}/formulario — grava o formulário padrão de adoção (GC-ADOC01). */
    @PutMapping("/{id}/formulario")
    fun formulario(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @PathVariable("id") id: String,
        @Valid @RequestBody req: PreencherFormularioRequest,
    ): AdocaoDto = adocao.preencherFormulario(eu, id, req.respostas)

    /** POST /api/adocoes/{id}/etapa — avança para a próxima etapa (RF28). */
    @PostMapping("/{id}/etapa")
    fun avancar(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @PathVariable("id") id: String,
        @Valid @RequestBody req: AvancarEtapaRequest,
    ): AdocaoDto = adocao.avancar(eu, id, req)

    /** POST /api/adocoes/{id}/visita/confirmacao — confirma a visita (abrigo ou adotante). */
    @PostMapping("/{id}/visita/confirmacao")
    fun confirmarVisita(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @PathVariable("id") id: String,
        @Valid @RequestBody req: ConfirmarVisitaRequest,
    ): AdocaoDto = adocao.confirmarVisita(eu, id, requireNotNull(req.parte))

    /** POST /api/adocoes/{id}/autorizacao — autoriza a adoção (administrador/gestor). */
    @PostMapping("/{id}/autorizacao")
    fun autorizar(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String): AdocaoDto =
        adocao.autorizar(eu, id)

    /** POST /api/adocoes/{id}/finalizacao — finaliza e marca o animal como adotado/inativo (RF29). */
    @PostMapping("/{id}/finalizacao")
    fun finalizar(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String): AdocaoDto =
        adocao.finalizar(eu, id)

    /** POST /api/adocoes/{id}/cancelamento — cancela o processo. */
    @PostMapping("/{id}/cancelamento")
    fun cancelar(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @PathVariable("id") id: String,
        @Valid @RequestBody req: CancelarAdocaoRequest,
    ): AdocaoDto = adocao.cancelar(eu, id, req.motivo)
}