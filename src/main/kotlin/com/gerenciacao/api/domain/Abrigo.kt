package com.gerenciacao.api.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant

/**
 * Abrigo (coleção "abrigos"). Cada conta nova cria o seu próprio abrigo (instância do sistema).
 * Os animais, despesas etc. guardam o "idAbrigo" em vez de ficarem dentro deste documento.
 * Os nomes no MongoDB seguem o banco real (GerenciaCaoDB): localizacao, contatoRedes e lotacao.
 */
@Document(collection = "abrigos")
data class Abrigo(
    @Id val id: String? = null,
    val nome: String,
    val descricao: String = "",
    val telefone: String = "",
    val email: String = "",
    /** Endereço em texto (campo "localizacao" no banco). */
    @Field("localizacao") val endereco: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Altitude em metros (já existe no banco; não é usada pelo app). */
    val altitude: Double? = null,
    /** Contas em redes sociais (campo "contatoRedes" no banco). */
    @Field("contatoRedes") val redesSociais: String = "",
    /** Capacidade máxima de bichinhos (campo "lotacao" no banco). Vagas = capacidade - animais ativos. */
    @Field("lotacao") val capacidade: Int = 0,
    val fotoUrl: String? = null,
    val criadoEm: Instant = Instant.now(),
)
