package com.gerenciacao.api.error

import com.gerenciacao.api.dto.ErroResposta
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

/** Transforma as exceções em respostas JSON no formato {"erro": "...", "campos": {...}}. */
@RestControllerAdvice
class GlobalExceptionHandler {

    /** Erros esperados da API (404, 409, 403, 401...). */
    @ExceptionHandler(ApiException::class)
    fun tratarApi(e: ApiException): ResponseEntity<ErroResposta> =
        ResponseEntity.status(e.status).body(ErroResposta(e.message ?: "Erro."))

    /** Campos inválidos (@NotBlank, @Email, @Size...): devolve a mensagem de cada campo. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun tratarValidacao(e: MethodArgumentNotValidException): ResponseEntity<ErroResposta> {
        val campos = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Valor inválido.") }
        return ResponseEntity.badRequest().body(ErroResposta("Dados inválidos.", campos))
    }

    /** JSON malformado ou valor de enum desconhecido (ex.: cargo "XYZ"). */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun tratarCorpoInvalido(e: HttpMessageNotReadableException): ResponseEntity<ErroResposta> =
        ResponseEntity.badRequest().body(ErroResposta("Corpo da requisição inválido."))

    /** Índice único violado (duas contas criadas ao mesmo tempo com o mesmo e-mail). */
    @ExceptionHandler(DuplicateKeyException::class)
    fun tratarDuplicado(e: DuplicateKeyException): ResponseEntity<ErroResposta> =
        ResponseEntity.status(409).body(ErroResposta("Já existe um registro com esses dados."))

    /** Arquivo maior que o limite configurado (2 MB por foto). */
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun tratarArquivoGrande(e: MaxUploadSizeExceededException): ResponseEntity<ErroResposta> =
        ResponseEntity.status(413).body(ErroResposta("A foto é grande demais (máximo de 2 MB)."))
}
