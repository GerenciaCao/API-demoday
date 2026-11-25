package com.gerenciacao.api.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** Rota pública para o app testar se a API está no ar. */
@RestController
class SaudeController {
    /** GET /api/saude -> {"status":"ok"} */
    @GetMapping("/api/saude")
    fun saude(): Map<String, String> = mapOf("status" to "ok")
}