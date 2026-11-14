package com.gerenciacao.api.security

import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Limitador de tentativas em memória (janela deslizante), contra adivinhação de senha e cadastros em massa.
 * Atenção: vale para UMA instância da API. Com várias instâncias, trocar por um limitador compartilhado (ex.: Redis).
 */
@Component
class LimitadorTaxa {
    private val eventos = ConcurrentHashMap<String, ArrayDeque<Instant>>()

    /** Registra um evento para a chave e diz se ainda está dentro do limite (true = permitido). */
    fun registrar(chave: String, maximo: Int, janela: Duration): Boolean {
        val agora = Instant.now()
        val fila = eventos.computeIfAbsent(chave) { ArrayDeque() }
        synchronized(fila) {
            descartarAntigos(fila, agora.minus(janela))
            fila.addLast(agora)
            return fila.size <= maximo
        }
    }

    /** Diz se a chave já atingiu o limite na janela, sem registrar um novo evento. */
    fun excedeu(chave: String, maximo: Int, janela: Duration): Boolean {
        val fila = eventos[chave] ?: return false
        synchronized(fila) {
            descartarAntigos(fila, Instant.now().minus(janela))
            return fila.size >= maximo
        }
    }

    /** Zera o histórico da chave (ex.: depois de um login correto). */
    fun limpar(chave: String) {
        eventos.remove(chave)
    }

    /** Remove da fila os eventos mais antigos que o corte. */
    private fun descartarAntigos(fila: ArrayDeque<Instant>, corte: Instant) {
        while (fila.isNotEmpty() && fila.first().isBefore(corte)) fila.removeFirst()
    }
}
