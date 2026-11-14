package com.gerenciacao.api.domain

/** Permissões de acesso de um membro da equipe (as mesmas 6 da tela de edição do app). */
enum class Permissao {
    VER_FICHAS,
    EDITAR_PRONTUARIOS,
    GERENCIAR_AGENDA,
    GERENCIAR_DADOS,
    MODIFICAR_EQUIPE,
    GERENCIAR_ESTOQUE,
}

/** Cargos do abrigo (lista da tela "Cargo" do app). */
enum class Cargo {
    ADMINISTRADOR,
    GESTOR,
    CUIDADOR,
    VETERINARIO,
    ADESTRADOR,
    VOLUNTARIO;

    /** Permissões que o cargo recebe por padrão quando nenhuma é informada. */
    fun permissoesPadrao(): Set<Permissao> = when (this) {
        ADMINISTRADOR -> Permissao.entries.toSet()
        GESTOR -> Permissao.entries.toSet() - Permissao.MODIFICAR_EQUIPE
        CUIDADOR -> setOf(Permissao.VER_FICHAS, Permissao.EDITAR_PRONTUARIOS, Permissao.GERENCIAR_AGENDA)
        VETERINARIO -> setOf(Permissao.VER_FICHAS, Permissao.EDITAR_PRONTUARIOS)
        ADESTRADOR, VOLUNTARIO -> setOf(Permissao.VER_FICHAS)
    }

    /** Administrador e Gestor podem editar as informações do abrigo. */
    fun ehGestao(): Boolean = this == ADMINISTRADOR || this == GESTOR
}

/** Situação da conta: CONVIDADO ainda não definiu a senha; INATIVO não consegue entrar. */
enum class StatusUsuario {
    ATIVO,
    CONVIDADO,
    INATIVO,
}
