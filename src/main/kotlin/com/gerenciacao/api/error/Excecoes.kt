package com.gerenciacao.api.error

import org.springframework.http.HttpStatus

/** Erro "esperado" da API: leva o status HTTP e a mensagem (em português) que o app vai mostrar. */
open class ApiException(val status: HttpStatus, mensagem: String) : RuntimeException(mensagem)

/** 404: o registro não existe (ou pertence a outro abrigo). */
class NaoEncontradoException(mensagem: String = "Registro não encontrado.") :
    ApiException(HttpStatus.NOT_FOUND, mensagem)

/** 409: conflito com algo que já existe (ex.: e-mail já cadastrado). */
class ConflitoException(mensagem: String) : ApiException(HttpStatus.CONFLICT, mensagem)

/** 400: a requisição é válida, mas quebra uma regra de negócio. */
class RegraNegocioException(mensagem: String) : ApiException(HttpStatus.BAD_REQUEST, mensagem)

/** 403: o usuário está logado, mas não tem permissão para a ação. */
class AcessoNegadoException(mensagem: String = "Você não tem permissão para esta ação.") :
    ApiException(HttpStatus.FORBIDDEN, mensagem)

/** 401: e-mail ou senha incorretos. A mensagem é a mesma nos dois casos de propósito. */
class CredenciaisInvalidasException : ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.")

/** 429: muitas tentativas seguidas (proteção contra adivinhação de senha e cadastro em massa). */
class MuitasTentativasException(mensagem: String = "Muitas tentativas. Aguarde alguns minutos e tente de novo.") :
    ApiException(HttpStatus.TOO_MANY_REQUESTS, mensagem)
