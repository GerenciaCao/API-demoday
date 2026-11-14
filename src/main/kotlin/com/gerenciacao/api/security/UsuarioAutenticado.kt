package com.gerenciacao.api.security

import com.gerenciacao.api.domain.Cargo
import com.gerenciacao.api.domain.Permissao
import com.gerenciacao.api.error.AcessoNegadoException

/**
 * Quem está fazendo a requisição. É montado a cada chamada a partir do banco (não do token),
 * então mudanças de cargo/permissão e remoções valem imediatamente.
 */
data class UsuarioAutenticado(
    val id: String,
    val abrigoId: String,
    val cargo: Cargo,
    val permissoes: Set<Permissao>,
) {
    /** Garante que o usuário tem a permissão; senão responde 403. */
    fun exigir(permissao: Permissao) {
        if (permissao !in permissoes) throw AcessoNegadoException()
    }

    /** Garante que o usuário é Administrador ou Gestor; senão responde 403. */
    fun exigirGestao() {
        if (!cargo.ehGestao()) throw AcessoNegadoException("Somente administradores e gestores podem fazer isso.")
    }
}
