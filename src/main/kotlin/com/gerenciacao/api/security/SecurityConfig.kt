package com.gerenciacao.api.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.access.AccessDeniedException

/** Regras de segurança: API sem sessão (stateless), autenticada por JWT; só cadastro, login e convite são públicos. */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    /** Define quais rotas são públicas e como o token JWT é validado. */
    @Bean
    fun filtroDeSeguranca(http: HttpSecurity, conversor: UsuarioJwtConverter): SecurityFilterChain {
        http
            .csrf { it.disable() } // API com token no cabeçalho, sem cookies: CSRF não se aplica
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/saude",
                    "/api/auth/cadastro",
                    "/api/auth/login",
                    "/api/auth/aceitar-convite",
                ).permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(NaoAutenticadoJson()).accessDeniedHandler(AcessoNegadoJson())
            }
            .oauth2ResourceServer { rs ->
                rs.jwt { jwt -> jwt.jwtAuthenticationConverter(conversor) }
                rs.authenticationEntryPoint(NaoAutenticadoJson())
                rs.accessDeniedHandler(AcessoNegadoJson())
            }
        return http.build()
    }

    /** Codificador de senhas: BCrypt (o hash inclui o "sal"; a senha original nunca é guardada). */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}

/** Responde 401 em JSON quando falta o token ou ele é inválido/expirado. */
class NaoAutenticadoJson : AuthenticationEntryPoint {
    /** Escreve a resposta 401. */
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) = escreverErro(response, HttpStatus.UNAUTHORIZED, "Sessão expirada ou inválida. Entre novamente.")
}

/** Responde 403 em JSON quando o usuário não tem acesso ao recurso. */
class AcessoNegadoJson : AccessDeniedHandler {
    /** Escreve a resposta 403. */
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) = escreverErro(response, HttpStatus.FORBIDDEN, "Você não tem permissão para esta ação.")
}

/** Escreve {"erro": "..."} na resposta (as mensagens são constantes, sem aspas, então não precisam de escape). */
private fun escreverErro(response: HttpServletResponse, status: HttpStatus, mensagem: String) {
    response.status = status.value()
    response.contentType = "application/json"
    response.characterEncoding = "UTF-8"
    response.writer.write("""{"erro":"$mensagem"}""")
}
