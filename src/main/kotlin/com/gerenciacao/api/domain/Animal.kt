package com.gerenciacao.api.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

/**
 * Visão MÍNIMA de um animal (coleção "animais"), só para contar os animais ativos do abrigo e conferir a quem pertence
 * (usada pelas fotos e pela ocupação). As fichas completas entram na próxima etapa.
 * Nunca salve este objeto: ele não tem todos os campos e apagaria o resto do documento.
 */
@Document(collection = "animais")
data class AnimalRef(
    @Id val id: String? = null,
    @Field("idAbrigo") val abrigoId: String,
    val ativo: Boolean = true,
)
