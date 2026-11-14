package com.gerenciacao.api.controller

import com.gerenciacao.api.domain.DonoFoto
import com.gerenciacao.api.dto.FotoDto
import com.gerenciacao.api.security.UsuarioAutenticado
import com.gerenciacao.api.service.FotoService
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.Duration

/** Fotos guardadas no banco. Todas as rotas exigem login. */
@RestController
@RequestMapping("/api/fotos")
class FotoController(private val fotos: FotoService) {

    /** POST /api/fotos (multipart: donoTipo, donoId, principal, arquivo) — envia uma foto de até 2 MB (JPEG, PNG ou WebP). */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun enviar(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @RequestParam("donoTipo") donoTipo: DonoFoto,
        @RequestParam("donoId") donoId: String,
        @RequestParam(name = "principal", defaultValue = "false") principal: Boolean,
        @RequestParam("arquivo") arquivo: MultipartFile,
    ): FotoDto = fotos.enviar(eu, donoTipo, donoId, principal, arquivo)

    /** GET /api/fotos?donoTipo=&donoId= — lista as fotos de um animal, usuário ou do abrigo (sem os bytes). */
    @GetMapping
    fun listar(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @RequestParam("donoTipo") donoTipo: DonoFoto,
        @RequestParam("donoId") donoId: String,
    ): List<FotoDto> = fotos.listar(eu, donoTipo, donoId)

    /** GET /api/fotos/{id} — devolve a imagem. O app precisa enviar o token (use o mesmo cliente HTTP do app). */
    @GetMapping("/{id}")
    fun baixar(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String): ResponseEntity<ByteArray> {
        val foto = fotos.baixar(eu, id)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(foto.contentType))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
            .header("X-Content-Type-Options", "nosniff")
            .body(foto.dados)
    }

    /** DELETE /api/fotos/{id} — apaga uma foto. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remover(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String) {
        fotos.remover(eu, id)
    }
}
