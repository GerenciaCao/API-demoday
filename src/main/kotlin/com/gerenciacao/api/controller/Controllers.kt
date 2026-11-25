package com.gerenciacao.api.controller

import com.gerenciacao.api.dto.AbrigoDto
import com.gerenciacao.api.dto.AceitarConviteRequest
import com.gerenciacao.api.dto.AtualizarAbrigoRequest
import com.gerenciacao.api.dto.AtualizarMembroRequest
import com.gerenciacao.api.dto.AuthResponse
import com.gerenciacao.api.dto.CadastroRequest
import com.gerenciacao.api.dto.ConviteResponse
import com.gerenciacao.api.dto.LoginRequest
import com.gerenciacao.api.dto.NovoMembroRequest
import com.gerenciacao.api.dto.UsuarioDto
import com.gerenciacao.api.security.UsuarioAutenticado
import com.gerenciacao.api.service.AbrigoService
import com.gerenciacao.api.service.AuthService
import com.gerenciacao.api.service.EquipeService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController



/** Cadastro, login, aceite de convite e "quem sou eu". */
@RestController
@RequestMapping("/api/auth")
class AuthController(private val auth: AuthService) {

    /** POST /api/auth/cadastro — cria a conta e o abrigo (RF01). */
    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    fun cadastro(@Valid @RequestBody req: CadastroRequest, request: HttpServletRequest): AuthResponse =
        auth.cadastrar(req, request.remoteAddr ?: "desconhecido")

    /** POST /api/auth/login — entra com e-mail e senha (RF02). */
    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest, request: HttpServletRequest): AuthResponse =
        auth.login(req, request.remoteAddr ?: "desconhecido")

    /** POST /api/auth/aceitar-convite — o convidado define a senha e entra (RF06). */
    @PostMapping("/aceitar-convite")
    fun aceitarConvite(@Valid @RequestBody req: AceitarConviteRequest): AuthResponse = auth.aceitarConvite(req)

    /** GET /api/auth/eu — dados do usuário logado. */
    @GetMapping("/eu")
    fun eu(@AuthenticationPrincipal eu: UsuarioAutenticado): UsuarioDto = auth.eu(eu.id)
}

/** Equipe do abrigo (telas Equipe, Perfil do membro, Editar membro e Adicionar membro). */
@RestController
@RequestMapping("/api/equipe")
class EquipeController(private val equipe: EquipeService) {

    /** GET /api/equipe — lista os membros. */
    @GetMapping
    fun listar(@AuthenticationPrincipal eu: UsuarioAutenticado): List<UsuarioDto> = equipe.listar(eu)

    /** GET /api/equipe/{id} — um membro. */
    @GetMapping("/{id}")
    fun buscar(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String): UsuarioDto =
        equipe.buscar(eu, id)

    /** POST /api/equipe — convida um novo membro (RF06). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun convidar(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @Valid @RequestBody req: NovoMembroRequest,
    ): ConviteResponse = equipe.convidar(eu, req)

    /** PUT /api/equipe/{id} — edita um membro (só os campos enviados mudam). */
    @PutMapping("/{id}")
    fun atualizar(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @PathVariable("id") id: String,
        @Valid @RequestBody req: AtualizarMembroRequest,
    ): UsuarioDto = equipe.atualizar(eu, id, req)

    /** DELETE /api/equipe/{id} — remove um membro (RF07). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remover(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String) {
        equipe.remover(eu, id)
    }
}

/** Informações do abrigo do usuário logado. */
@RestController
@RequestMapping("/api/abrigo")
class AbrigoController(private val abrigo: AbrigoService) {

    /** GET /api/abrigo — dados do abrigo. */
    @GetMapping
    fun buscar(@AuthenticationPrincipal eu: UsuarioAutenticado): AbrigoDto = abrigo.buscar(eu)

    @GetMapping("/{id}")
    fun buscar(@AuthenticationPrincipal eu: UsuarioAutenticado): AbrigoDto = abrigo.buscar(eu)

    /** PUT /api/abrigo — edita o abrigo (administrador ou gestor). */
    @PutMapping
    fun atualizar(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @Valid @RequestBody req: AtualizarAbrigoRequest,
    ): AbrigoDto = abrigo.atualizar(eu, req)
}
