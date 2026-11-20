package com.gerenciacao.api.service

import com.gerenciacao.api.domain.DonoFoto
import com.gerenciacao.api.domain.Foto
import com.gerenciacao.api.domain.FotoMeta
import com.gerenciacao.api.domain.Permissao
import com.gerenciacao.api.dto.FotoDto
import com.gerenciacao.api.error.NaoEncontradoException
import com.gerenciacao.api.error.RegraNegocioException
import com.gerenciacao.api.repository.AnimalRefRepository
import com.gerenciacao.api.repository.FotoMetaRepository
import com.gerenciacao.api.repository.FotoRepository
import com.gerenciacao.api.repository.UsuarioRepository
import com.gerenciacao.api.security.UsuarioAutenticado
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/** Fotos de animais, usuários e do abrigo, guardadas no próprio MongoDB (coleção "fotos"). */
@Service
class FotoService(
    private val fotos: FotoRepository,
    private val metas: FotoMetaRepository,
    private val animais: AnimalRefRepository,
    private val usuarios: UsuarioRepository,
    private val mongo: MongoTemplate,
) {
    /**
     * Guarda uma foto. Confere quem pode, o tamanho (até 2 MB) e o formato pelos BYTES do arquivo
     * (JPEG, PNG ou WebP), ignorando o tipo que o cliente disser: assim não entra SVG nem outro arquivo disfarçado.
     */
    fun enviar(eu: UsuarioAutenticado, donoTipo: DonoFoto, donoId: String, principal: Boolean, arquivo: MultipartFile): FotoDto {
        exigirAcesso(eu, donoTipo, donoId, escrita = true)
        val bytes = arquivo.bytes
        if (bytes.isEmpty()) throw RegraNegocioException("O arquivo está vazio.")
        if (bytes.size > MAX_BYTES) throw RegraNegocioException("A foto deve ter no máximo 2 MB.")
        val tipo = detectarTipo(bytes) ?: throw RegraNegocioException("Formato inválido. Envie uma imagem JPEG, PNG ou WebP.")

        if (donoTipo == DonoFoto.ANIMAL) {
            if (fotos.countByDonoTipoAndDonoId(donoTipo, donoId) >= MAX_POR_ANIMAL) {
                throw RegraNegocioException("Cada animal pode ter no máximo $MAX_POR_ANIMAL fotos.")
            }
        } else {
            fotos.deleteByDonoTipoAndDonoId(donoTipo, donoId) // usuário e abrigo têm uma foto só: a nova substitui a antiga
        }
        val primeira = fotos.countByDonoTipoAndDonoId(donoTipo, donoId) == 0L
        val ficaPrincipal = principal || primeira || donoTipo != DonoFoto.ANIMAL
        if (ficaPrincipal && donoTipo == DonoFoto.ANIMAL) {
            mongo.updateMulti(
                Query(Criteria.where("donoTipo").`is`(donoTipo.name).and("donoId").`is`(donoId)),
                Update().set("principal", false),
                "fotos",
            )
        }
        val salva = fotos.save(
            Foto(abrigoId = eu.abrigoId, donoTipo = donoTipo, donoId = donoId, contentType = tipo, tamanhoBytes = bytes.size, dados = bytes, principal = ficaPrincipal),
        )
        return paraDto(requireNotNull(salva.id), salva.donoTipo, salva.donoId, salva.principal, salva.contentType, salva.tamanhoBytes)
    }

    /** Lista as fotos de um dono (sem os bytes). */
    fun listar(eu: UsuarioAutenticado, donoTipo: DonoFoto, donoId: String): List<FotoDto> {
        exigirAcesso(eu, donoTipo, donoId, escrita = false)
        return metas.listar(donoTipo.name, donoId).filter { it.abrigoId == eu.abrigoId }.map { it.paraDto() }
    }

    /** Devolve a foto com os bytes, se o usuário pode ver. De outro abrigo = 404. */
    fun baixar(eu: UsuarioAutenticado, id: String): Foto {
        val foto = fotos.findById(id).orElse(null) ?: throw NaoEncontradoException("Foto não encontrada.")
        if (foto.donoTipo == DonoFoto.ABRIGO) return foto // a foto de perfil de um abrigo é pública para quem está logado (tela Conexão)
        if (foto.abrigoId != eu.abrigoId) throw NaoEncontradoException("Foto não encontrada.")
        exigirAcesso(eu, foto.donoTipo, foto.donoId, escrita = false)
        return foto
    }

    /** Apaga uma foto (mesma regra de quem pode enviar). */
    fun remover(eu: UsuarioAutenticado, id: String) {
        val foto = fotos.findById(id).orElse(null)?.takeIf { it.abrigoId == eu.abrigoId }
            ?: throw NaoEncontradoException("Foto não encontrada.")
        exigirAcesso(eu, foto.donoTipo, foto.donoId, escrita = true)
        fotos.delete(foto)
    }

    /**
     * Regras de acesso: animal (ver = VER_FICHAS, mexer = EDITAR_PRONTUARIOS); usuário (ver = mesmo abrigo,
     * mexer = a própria foto ou MODIFICAR_EQUIPE); abrigo (ver = membro, mexer = administrador ou gestor).
     */
    private fun exigirAcesso(eu: UsuarioAutenticado, donoTipo: DonoFoto, donoId: String, escrita: Boolean) {
        when (donoTipo) {
            DonoFoto.ANIMAL -> {
                animais.findById(donoId)?.takeIf { it.abrigoId == eu.abrigoId } ?: throw NaoEncontradoException("Animal não encontrado.")
                eu.exigir(if (escrita) Permissao.EDITAR_PRONTUARIOS else Permissao.VER_FICHAS)
            }
            DonoFoto.USUARIO -> {
                val alvo = usuarios.findById(donoId).orElse(null)?.takeIf { it.abrigoId == eu.abrigoId }
                    ?: throw NaoEncontradoException("Membro não encontrado.")
                if (escrita && alvo.id != eu.id) eu.exigir(Permissao.MODIFICAR_EQUIPE)
            }
            DonoFoto.ABRIGO -> {
                if (donoId != eu.abrigoId) throw NaoEncontradoException("Abrigo não encontrado.")
                if (escrita) eu.exigirGestao()
            }
        }
    }

    /** Descobre o formato olhando os primeiros bytes (JPEG, PNG ou WebP); null se não for imagem aceita. */
    private fun detectarTipo(b: ByteArray): String? = when {
        b.size >= 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() && b[2] == 0xFF.toByte() -> "image/jpeg"
        b.size >= 8 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte() && b[2] == 'N'.code.toByte() && b[3] == 'G'.code.toByte() &&
            b[4] == 0x0D.toByte() && b[5] == 0x0A.toByte() && b[6] == 0x1A.toByte() && b[7] == 0x0A.toByte() -> "image/png"
        b.size >= 12 && String(b, 0, 4, Charsets.US_ASCII) == "RIFF" && String(b, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
        else -> null
    }

    private fun FotoMeta.paraDto() = paraDto(requireNotNull(id), donoTipo, donoId, principal, contentType, tamanhoBytes)

    private fun paraDto(id: String, dono: DonoFoto, donoId: String, principal: Boolean, tipo: String, tamanho: Int) =
        FotoDto(id = id, donoTipo = dono, donoId = donoId, principal = principal, contentType = tipo, tamanhoBytes = tamanho, url = "/api/fotos/$id")

    private companion object {
        const val MAX_BYTES = 2 * 1024 * 1024
        const val MAX_POR_ANIMAL = 10
    }
}
