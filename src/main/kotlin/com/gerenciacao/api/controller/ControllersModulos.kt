package com.gerenciacao.api.controller

import com.gerenciacao.api.domain.*
import com.gerenciacao.api.dto.*
import com.gerenciacao.api.security.UsuarioAutenticado
import com.gerenciacao.api.service.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate

// Controllers dos módulos. Cada método só repassa para o serviço: as regras e permissões ficam nos serviços.

/** Fichas dos bichinhos, vacinas e histórico médico. */
@RestController
@RequestMapping("/api/animais")
class AnimalController(private val animais: AnimalService) {
    /** GET /api/animais — lista com filtros e paginação. */
    @GetMapping
    fun listar(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @RequestParam(name = "especie", required = false) especie: Especie?,
        @RequestParam(name = "status", required = false) status: StatusAnimal?,
        @RequestParam(name = "busca", required = false) busca: String?,
        @RequestParam(name = "incluirInativos", defaultValue = "false") incluirInativos: Boolean,
        @RequestParam(name = "pagina", defaultValue = "0") pagina: Int,
        @RequestParam(name = "tamanho", defaultValue = "50") tamanho: Int,
    ): Pagina<AnimalDto> = animais.listar(eu, especie, status, busca, incluirInativos, pagina, tamanho)

    /** GET /api/animais/{id} — ficha completa (vacinas, histórico e fotos). */
    @GetMapping("/{id}")
    fun detalhe(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String): AnimalDetalheDto = animais.detalhe(eu, id)

    /** POST /api/animais — cria uma ficha. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun criar(@AuthenticationPrincipal eu: UsuarioAutenticado, @Valid @RequestBody req: NovoAnimalRequest): AnimalDto = animais.criar(eu, req)

    /** PUT /api/animais/{id} — edita a ficha. */
    @PutMapping("/{id}")
    fun atualizar(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String, @Valid @RequestBody req: AtualizarAnimalRequest): AnimalDto = animais.atualizar(eu, id, req)

    /** PUT /api/animais/{id}/status — muda a situação (disponível, em tratamento, adotado...). */
    @PutMapping("/{id}/status")
    fun status(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String, @Valid @RequestBody req: MudarStatusRequest): AnimalDto = animais.mudarStatus(eu, id, req)

    /** DELETE /api/animais/{id} — apaga a ficha e tudo que é dela. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remover(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String) { animais.remover(eu, id) }

    /** POST /api/animais/{id}/vacinas — registra uma vacina. */
    @PostMapping("/{id}/vacinas")
    @ResponseStatus(HttpStatus.CREATED)
    fun vacina(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String, @Valid @RequestBody req: NovaVacinaRequest): VacinaDto = animais.adicionarVacina(eu, id, req)

    /** DELETE /api/animais/{id}/vacinas/{vacinaId}. */
    @DeleteMapping("/{id}/vacinas/{vacinaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removerVacina(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String, @PathVariable("vacinaId") vacinaId: String) { animais.removerVacina(eu, id, vacinaId) }

    /** POST /api/animais/{id}/historico — registra uma consulta. */
    @PostMapping("/{id}/historico")
    @ResponseStatus(HttpStatus.CREATED)
    fun historico(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String, @Valid @RequestBody req: NovoHistoricoRequest): HistoricoDto = animais.adicionarHistorico(eu, id, req)

    /** DELETE /api/animais/{id}/historico/{historicoId}. */
    @DeleteMapping("/{id}/historico/{historicoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removerHistorico(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String, @PathVariable("historicoId") historicoId: String) { animais.removerHistorico(eu, id, historicoId) }
}

/** Despesas, doações e relatórios. */
@RestController
@RequestMapping("/api")
class FinanceiroController(private val fin: FinanceiroService) {
    /** GET /api/despesas. */
    @GetMapping("/despesas")
    fun despesas(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @RequestParam(name = "tipo", required = false) tipo: TipoDespesa?,
        @RequestParam(name = "de", required = false) de: Instant?,
        @RequestParam(name = "ate", required = false) ate: Instant?,
        @RequestParam(name = "busca", required = false) busca: String?,
        @RequestParam(name = "pagina", defaultValue = "0") pagina: Int,
        @RequestParam(name = "tamanho", defaultValue = "50") tamanho: Int,
    ): Pagina<DespesaDto> = fin.listarDespesas(eu, tipo, de, ate, busca, pagina, tamanho)

    /** POST /api/despesas. */
    @PostMapping("/despesas")
    @ResponseStatus(HttpStatus.CREATED)
    fun novaDespesa(@AuthenticationPrincipal eu: UsuarioAutenticado, @Valid @RequestBody req: DespesaRequest): DespesaDto = fin.criarDespesa(eu, req)

    /** PUT /api/despesas/{id}. */
    @PutMapping("/despesas/{id}")
    fun editarDespesa(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String, @Valid @RequestBody req: DespesaRequest): DespesaDto = fin.atualizarDespesa(eu, id, req)

    /** DELETE /api/despesas/{id}. */
    @DeleteMapping("/despesas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun apagarDespesa(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String) { fin.removerDespesa(eu, id) }

    /** GET /api/doacoes. */
    @GetMapping("/doacoes")
    fun doacoes(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @RequestParam(name = "tipo", required = false) tipo: TipoDoacao?,
        @RequestParam(name = "de", required = false) de: Instant?,
        @RequestParam(name = "ate", required = false) ate: Instant?,
        @RequestParam(name = "pagina", defaultValue = "0") pagina: Int,
        @RequestParam(name = "tamanho", defaultValue = "50") tamanho: Int,
    ): Pagina<DoacaoDto> = fin.listarDoacoes(eu, tipo, de, ate, pagina, tamanho)

    /** POST /api/doacoes. */
    @PostMapping("/doacoes")
    @ResponseStatus(HttpStatus.CREATED)
    fun novaDoacao(@AuthenticationPrincipal eu: UsuarioAutenticado, @Valid @RequestBody req: DoacaoRequest): DoacaoDto = fin.criarDoacao(eu, req)

    /** PUT /api/doacoes/{id}. */
    @PutMapping("/doacoes/{id}")
    fun editarDoacao(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String, @Valid @RequestBody req: DoacaoRequest): DoacaoDto = fin.atualizarDoacao(eu, id, req)

    /** DELETE /api/doacoes/{id}. */
    @DeleteMapping("/doacoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun apagarDoacao(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String) { fin.removerDoacao(eu, id) }

    /** GET /api/financeiro/resumo?mes=2026-11 — doações, despesas e saldo do mês. */
    @GetMapping("/financeiro/resumo")
    fun resumo(@AuthenticationPrincipal eu: UsuarioAutenticado, @RequestParam(name = "mes", required = false) mes: String?): ResumoFinanceiroDto = fin.resumoDoMes(eu, mes)

    /** GET /api/financeiro/relatorio?periodo=SEMANAL|MENSAL&referencia=2026-11-14. */
    @GetMapping("/financeiro/relatorio")
    fun relatorio(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @RequestParam(name = "periodo", defaultValue = "MENSAL") periodo: String,
        @RequestParam(name = "referencia", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) referencia: LocalDate?,
    ): RelatorioDto = fin.relatorio(eu, periodo, referencia)
}

/** Estoque do abrigo. */
@RestController
@RequestMapping("/api/estoque")
class EstoqueController(private val estoque: EstoqueService) {
    /** GET /api/estoque. */
    @GetMapping
    fun listar(@AuthenticationPrincipal eu: UsuarioAutenticado): List<EstoqueDto> = estoque.listar(eu)

    /** POST /api/estoque. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun criar(@AuthenticationPrincipal eu: UsuarioAutenticado, @Valid @RequestBody req: EstoqueRequest): EstoqueDto = estoque.criar(eu, req)

    /** PUT /api/estoque/{id}. */
    @PutMapping("/{id}")
    fun atualizar(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String, @Valid @RequestBody req: EstoqueRequest): EstoqueDto = estoque.atualizar(eu, id, req)

    /** DELETE /api/estoque/{id}. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remover(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String) { estoque.remover(eu, id) }
}

/** Calendário. */
@RestController
@RequestMapping("/api/eventos")
class AgendaController(private val agenda: AgendaService) {
    /** GET /api/eventos?de=&ate= (datas ISO, ex.: 2026-11-01T00:00:00Z). */
    @GetMapping
    fun listar(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @RequestParam(name = "de", required = false) de: Instant?,
        @RequestParam(name = "ate", required = false) ate: Instant?,
    ): List<EventoDto> = agenda.listar(eu, de, ate)

    /** POST /api/eventos. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun criar(@AuthenticationPrincipal eu: UsuarioAutenticado, @Valid @RequestBody req: EventoRequest): EventoDto = agenda.criar(eu, req)

    /** PUT /api/eventos/{id} — edita ou reagenda. */
    @PutMapping("/{id}")
    fun atualizar(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String, @Valid @RequestBody req: EventoRequest): EventoDto = agenda.atualizar(eu, id, req)

    /** DELETE /api/eventos/{id}. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remover(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String) { agenda.remover(eu, id) }
}

/** Processo de adoção. */
@RestController
@RequestMapping("/api/adocoes")
class AdocaoController(private val adocoes: AdocaoService) {
    /** GET /api/adocoes?emAndamento=true. */
    @GetMapping
    fun listar(@AuthenticationPrincipal eu: UsuarioAutenticado, @RequestParam(name = "emAndamento", defaultValue = "true") emAndamento: Boolean): List<AdocaoDto> = adocoes.listar(eu, emAndamento)

    /** POST /api/adocoes — registra um interessado. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun criar(@AuthenticationPrincipal eu: UsuarioAutenticado, @Valid @RequestBody req: NovaAdocaoRequest): AdocaoDto = adocoes.criar(eu, req)

    /** PUT /api/adocoes/{id}/etapa. */
    @PutMapping("/{id}/etapa")
    fun etapa(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String, @Valid @RequestBody req: EtapaRequest): AdocaoDto = adocoes.mudarEtapa(eu, id, req)

    /** POST /api/adocoes/{id}/concluir — marca o bichinho como adotado. */
    @PostMapping("/{id}/concluir")
    fun concluir(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String): AdocaoDto = adocoes.concluir(eu, id)

    /** POST /api/adocoes/{id}/cancelar. */
    @PostMapping("/{id}/cancelar")
    fun cancelar(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String): AdocaoDto = adocoes.cancelar(eu, id)
}

/** Parcerias (anúncios). */
@RestController
@RequestMapping("/api/parcerias")
class ParceriaController(private val parcerias: ParceriaService) {
    /** GET /api/parcerias?tipo=&categoria=&busca=&destaque=. */
    @GetMapping
    fun listar(
        @RequestParam(name = "tipo", required = false) tipo: TipoParceria?,
        @RequestParam(name = "categoria", required = false) categoria: String?,
        @RequestParam(name = "busca", required = false) busca: String?,
        @RequestParam(name = "destaque", required = false) destaque: Boolean?,
    ): List<ParceriaDto> = parcerias.listar(tipo, categoria, busca, destaque)

    /** POST /api/parcerias — só administrador da plataforma. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun criar(@AuthenticationPrincipal eu: UsuarioAutenticado, @Valid @RequestBody req: ParceriaRequest): ParceriaDto = parcerias.criar(eu, req)

    /** PUT /api/parcerias/{id}. */
    @PutMapping("/{id}")
    fun atualizar(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String, @Valid @RequestBody req: ParceriaRequest): ParceriaDto = parcerias.atualizar(eu, id, req)

    /** DELETE /api/parcerias/{id}. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remover(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String) { parcerias.remover(eu, id) }
}

/** Rede de abrigos (tela Conexão). */
@RestController
@RequestMapping("/api/rede")
class RedeController(private val rede: RedeService) {
    /** GET /api/rede?busca=&somenteConexoes=false. */
    @GetMapping
    fun listar(
        @AuthenticationPrincipal eu: UsuarioAutenticado,
        @RequestParam(name = "busca", required = false) busca: String?,
        @RequestParam(name = "somenteConexoes", defaultValue = "false") somenteConexoes: Boolean,
    ): List<RedeAbrigoDto> = rede.listar(eu, busca, somenteConexoes)

    /** POST /api/rede/{abrigoId}/conectar — pede conexão. */
    @PostMapping("/{abrigoId}/conectar")
    @ResponseStatus(HttpStatus.CREATED)
    fun conectar(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("abrigoId") abrigoId: String): RedeAbrigoDto = rede.conectar(eu, abrigoId)

    /** POST /api/rede/conexoes/{id}/aceitar. */
    @PostMapping("/conexoes/{id}/aceitar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun aceitar(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String) { rede.aceitar(eu, id) }

    /** DELETE /api/rede/conexoes/{id} — recusa ou desfaz. */
    @DeleteMapping("/conexoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remover(@AuthenticationPrincipal eu: UsuarioAutenticado, @PathVariable("id") id: String) { rede.remover(eu, id) }
}

/** Painel da tela Início. */
@RestController
@RequestMapping("/api/dashboard")
class DashboardController(private val dashboard: DashboardService) {
    /** GET /api/dashboard. */
    @GetMapping
    fun painel(@AuthenticationPrincipal eu: UsuarioAutenticado): DashboardDto = dashboard.painel(eu)
}

/** Senha e e-mail: recuperar, redefinir, alterar e validar. */
@RestController
@RequestMapping("/api/auth")
class SenhaController(private val recuperacao: RecuperacaoService, private val auth: AuthService) {
    /** POST /api/auth/esqueci-senha — sempre responde 202, exista ou não a conta. */
    @PostMapping("/esqueci-senha")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun esqueci(@Valid @RequestBody req: EsqueciSenhaRequest, request: HttpServletRequest): MensagemDto {
        recuperacao.esqueciSenha(req.email, request.remoteAddr ?: "desconhecido")
        return MensagemDto("Se o e-mail estiver cadastrado, enviamos um código para criar uma nova senha.")
    }

    /** POST /api/auth/redefinir-senha — cria a nova senha com o código recebido. */
    @PostMapping("/redefinir-senha")
    fun redefinir(@Valid @RequestBody req: RedefinirSenhaRequest, request: HttpServletRequest): MensagemDto {
        recuperacao.redefinirSenha(req.token, req.novaSenha, request.remoteAddr ?: "desconhecido")
        return MensagemDto("Senha alterada. Entre com a nova senha.")
    }

    /** POST /api/auth/alterar-senha — usuário logado troca a senha e recebe um token novo. */
    @PostMapping("/alterar-senha")
    fun alterar(@AuthenticationPrincipal eu: UsuarioAutenticado, @Valid @RequestBody req: AlterarSenhaRequest): AuthResponse = auth.alterarSenha(eu.id, req.senhaAtual, req.novaSenha)

    /** POST /api/auth/validar-email/solicitar — manda o código de validação para o e-mail do usuário logado. */
    @PostMapping("/validar-email/solicitar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun solicitarValidacao(@AuthenticationPrincipal eu: UsuarioAutenticado): MensagemDto {
        recuperacao.solicitarValidacaoEmail(eu.id)
        return MensagemDto("Enviamos um código para o seu e-mail.")
    }

    /** POST /api/auth/validar-email — confirma o e-mail com o código. */
    @PostMapping("/validar-email")
    fun validar(@Valid @RequestBody req: TokenRequest): MensagemDto {
        recuperacao.validarEmail(req.token)
        return MensagemDto("E-mail confirmado.")
    }
}
