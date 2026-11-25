package com.gerenciacao.api.controller

import com.gerenciacao.api.dto.AceitarConviteRequest
import com.gerenciacao.api.dto.AuthResponse
import com.gerenciacao.api.dto.CadastroRequest
import com.gerenciacao.api.dto.LoginRequest
import com.gerenciacao.api.dto.UsuarioDto
import com.gerenciacao.api.security.UsuarioAutenticado
import com.gerenciacao.api.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val auth: AuthService) {

    @PostMapping("/cadastro")
    fun cadastro(
        @Valid @RequestBody req: CadastroRequest,
        request: HttpServletRequest,
    ): ResponseEntity<AuthResponse> {
        val resposta = auth.cadastrar(req, request.remoteAddr ?: "desconhecido")
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta)
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody req: LoginRequest,
        request: HttpServletRequest,
    ): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(auth.login(req, request.remoteAddr ?: "desconhecido"))

    @PostMapping("/aceitar-convite")
    fun aceitarConvite(@Valid @RequestBody req: AceitarConviteRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(auth.aceitarConvite(req))

    @GetMapping("/eu")
    fun eu(@AuthenticationPrincipal eu: UsuarioAutenticado): ResponseEntity<UsuarioDto> =
        ResponseEntity.ok(auth.eu(eu.id))
}