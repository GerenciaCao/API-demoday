package com.gerenciacao.api.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant

/**
 * Usuário do sistema = membro da equipe de um abrigo (coleção "usuarios" do MongoDB).
 * Os nomes dos campos no MongoDB seguem o banco real (GerenciaCaoDB): "idAbrigo" e "senha" (que guarda o hash).
 * Cargo e status são gravados pelo NOME do enum (ex.: "ADMINISTRADOR", "ATIVO").
 */
@Document(collection = "usuarios")
data class Usuario(
    @Id val id: String? = null,
    /** Abrigo ao qual o usuário pertence (campo "idAbrigo", referência para a coleção "abrigos"). */
    @Field("idAbrigo") @Indexed val abrigoId: String,
    val nome: String,
    val sobrenome: String = "",
    /** Sempre guardado em minúsculas; o índice único impede duas contas com o mesmo e-mail. */
    @Indexed(unique = true) val email: String,
    val telefone: String = "",
    val endereco: String = "",
    val fotoUrl: String? = null,
    /** Hash BCrypt da senha (campo "senha" no banco). Ausente enquanto o convite não foi aceito. NUNCA enviado ao app. */
    @Field("senha") val senhaHash: String? = null,
    val cargo: Cargo,
    val permissoes: Set<Permissao> = emptySet(),
    val status: StatusUsuario = StatusUsuario.ATIVO,
    val emailVerificado: Boolean = false,
    val passouPeloOnboard: Boolean = false,
    /** Hash (SHA-256) do token de convite; o token em si só é mostrado uma vez a quem convidou. */
    @Indexed val conviteTokenHash: String? = null,
    val conviteExpiraEm: Instant? = null,
    val criadoEm: Instant = Instant.now(),
)
