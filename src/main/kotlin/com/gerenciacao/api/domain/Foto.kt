package com.gerenciacao.api.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant

/** A quem a foto pertence. */
enum class DonoFoto { ANIMAL, USUARIO, ABRIGO }

/**
 * Foto guardada DENTRO do MongoDB (coleção "fotos"), como decidido pela equipe.
 * O app deve reduzir a imagem (ex.: 1280 px, JPEG) antes de enviar: o limite é 2 MB por foto
 * (o MongoDB aceita até 16 MB por documento).
 */
@Document(collection = "fotos")
data class Foto(
    @Id val id: String? = null,
    @Field("idAbrigo") @Indexed val abrigoId: String,
    val donoTipo: DonoFoto,
    @Indexed val donoId: String,
    val contentType: String,
    val tamanhoBytes: Int,
    val dados: ByteArray,
    val principal: Boolean = false,
    val criadoEm: Instant = Instant.now(),
)

/** A mesma coleção "fotos", mas SEM os bytes: usada para listar sem trazer as imagens do banco. Nunca salvar. */
@Document(collection = "fotos")
data class FotoMeta(
    @Id val id: String? = null,
    @Field("idAbrigo") val abrigoId: String,
    val donoTipo: DonoFoto,
    val donoId: String,
    val contentType: String,
    val tamanhoBytes: Int,
    val principal: Boolean = false,
    val criadoEm: Instant = Instant.now(),
)
