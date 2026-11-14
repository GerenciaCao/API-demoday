package com.gerenciacao.api

import com.gerenciacao.api.config.JwtConfig
import com.gerenciacao.api.config.JwtProperties
import com.gerenciacao.api.domain.Cargo
import com.gerenciacao.api.domain.Usuario
import com.gerenciacao.api.security.TokenService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Testes rápidos (não precisam de banco): emissão/validação de JWT e permissões padrão dos cargos. */
class TokenServiceTest {

    private val props = JwtProperties(segredo = "segredo-de-teste-com-mais-de-32-caracteres!", expiracaoHoras = 1)
    private val config = JwtConfig(props)
    private val servico = TokenService(config.jwtEncoder(), props)

    /** O token gerado é aceito pelo decoder e carrega o id do usuário no campo "sub". */
    @Test
    fun `token gerado e valido e carrega o id do usuario`() {
        val usuario = Usuario(id = "abc123", abrigoId = "abrigo1", nome = "Ana", email = "ana@exemplo.com", cargo = Cargo.CUIDADOR)

        val gerado = servico.gerar(usuario)
        val jwt = config.jwtDecoder().decode(gerado.valor)

        assertEquals("abc123", jwt.subject)
        assertTrue(gerado.expiraEm.isAfter(java.time.Instant.now()))
    }

    /** Administrador tem tudo; Gestor não gerencia equipe; Voluntário só vê fichas. */
    @Test
    fun `permissoes padrao de cada cargo`() {
        assertEquals(com.gerenciacao.api.domain.Permissao.entries.toSet(), Cargo.ADMINISTRADOR.permissoesPadrao())
        assertTrue(com.gerenciacao.api.domain.Permissao.MODIFICAR_EQUIPE !in Cargo.GESTOR.permissoesPadrao())
        assertEquals(setOf(com.gerenciacao.api.domain.Permissao.VER_FICHAS), Cargo.VOLUNTARIO.permissoesPadrao())
    }
}
