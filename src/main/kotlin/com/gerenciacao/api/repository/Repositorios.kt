package com.gerenciacao.api.repository

import com.gerenciacao.api.domain.Abrigo
import com.gerenciacao.api.domain.Usuario
import org.springframework.data.mongodb.repository.MongoRepository

/** Acesso à coleção "usuarios". O Spring Data cria as consultas a partir dos nomes dos métodos. */
interface UsuarioRepository : MongoRepository<Usuario, String> {
    /** Busca pelo e-mail (já em minúsculas). */
    fun findByEmail(email: String): Usuario?

    /** Diz se já existe conta com esse e-mail. */
    fun existsByEmail(email: String): Boolean

    /** Lista a equipe de um abrigo. */
    fun findByAbrigoId(abrigoId: String): List<Usuario>

    /** Encontra o usuário a partir do hash do token de convite. */
    fun findByConviteTokenHash(hash: String): Usuario?
}

/** Acesso à coleção "abrigos". */
interface AbrigoRepository : MongoRepository<Abrigo, String>

/** Só leitura: contagem e consulta de animais (visão mínima). Estende Repository (e não MongoRepository) de propósito: sem save/delete. */
interface AnimalRefRepository : org.springframework.data.repository.Repository<com.gerenciacao.api.domain.AnimalRef, String> {
    /** Quantos animais do abrigo estão ativos (ocupam vaga). */
    fun countByAbrigoIdAndAtivo(abrigoId: String, ativo: Boolean): Long

    /** Busca um animal pelo id (visão mínima). */
    fun findById(id: String): com.gerenciacao.api.domain.AnimalRef?
}

/** Fotos com os bytes (para enviar, baixar e apagar). */
interface FotoRepository : MongoRepository<com.gerenciacao.api.domain.Foto, String> {
    /** Quantas fotos o dono tem. */
    fun countByDonoTipoAndDonoId(donoTipo: com.gerenciacao.api.domain.DonoFoto, donoId: String): Long

    /** Apaga as fotos do dono (usado para substituir a foto única de usuário/abrigo). */
    fun deleteByDonoTipoAndDonoId(donoTipo: com.gerenciacao.api.domain.DonoFoto, donoId: String): Long
}

/** Lista de fotos SEM os bytes (o campo "dados" é excluído da consulta). */
interface FotoMetaRepository : org.springframework.data.repository.Repository<com.gerenciacao.api.domain.FotoMeta, String> {
    /** Fotos de um dono, sem a imagem. */
    @org.springframework.data.mongodb.repository.Query(value = "{ 'donoTipo': ?0, 'donoId': ?1 }", fields = "{ 'dados': 0 }")
    fun listar(donoTipo: String, donoId: String): List<com.gerenciacao.api.domain.FotoMeta>
}
