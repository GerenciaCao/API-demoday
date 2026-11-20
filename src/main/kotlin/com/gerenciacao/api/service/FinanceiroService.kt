package com.gerenciacao.api.service

import com.gerenciacao.api.domain.*
import com.gerenciacao.api.dto.*
import com.gerenciacao.api.error.NaoEncontradoException
import com.gerenciacao.api.error.RegraNegocioException
import com.gerenciacao.api.repository.*
import com.gerenciacao.api.security.UsuarioAutenticado
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.regex.Pattern

/** Despesas, doações e relatórios (RF12 a RF14). Tudo exige a permissão GERENCIAR_DADOS e fica restrito ao abrigo de quem pede. */
@Service
class FinanceiroService(
    private val despesas: DespesaRepository,
    private val doacoes: DoacaoRepository,
    private val animais: AnimalRepository,
    private val mongo: MongoTemplate,
) {
    // ------------------------------------------------------------ Despesas

    /** Lista despesas com filtros (tipo, período, texto), da mais recente para a mais antiga. */
    fun listarDespesas(eu: UsuarioAutenticado, tipo: TipoDespesa?, de: Instant?, ate: Instant?, busca: String?, pagina: Int, tamanho: Int): Pagina<DespesaDto> {
        eu.exigir(Permissao.GERENCIAR_DADOS)
        val c = mutableListOf(Criteria.where("idAbrigo").`is`(eu.abrigoId))
        tipo?.let { c += Criteria.where("tipo").`is`(it.name) }
        de?.let { c += Criteria.where("data").gte(it) }
        ate?.let { c += Criteria.where("data").lt(it) }
        busca?.trim()?.takeIf { it.isNotEmpty() }?.let { c += Criteria.where("nome").regex(Pattern.quote(it), "i") }
        val filtro = Criteria().andOperator(*c.toTypedArray())
        val tam = tamanho.coerceIn(1, 100)
        val pag = pagina.coerceAtLeast(0)
        val total = mongo.count(Query(filtro), Despesa::class.java)
        val itens = mongo.find(Query(filtro).with(PageRequest.of(pag, tam, Sort.by(Sort.Direction.DESC, "data"))), Despesa::class.java)
        return Pagina(itens.map { it.paraDto() }, pag, tam, total)
    }

    /** Registra uma despesa. Despesa MEDICO precisa de animal do abrigo e do tipo médico. */
    fun criarDespesa(eu: UsuarioAutenticado, req: DespesaRequest): DespesaDto {
        eu.exigir(Permissao.GERENCIAR_DADOS)
        validarDespesa(eu, req)
        return despesas.save(montarDespesa(null, eu, req)).paraDto()
    }

    /** Edita uma despesa. */
    fun atualizarDespesa(eu: UsuarioAutenticado, id: String, req: DespesaRequest): DespesaDto {
        eu.exigir(Permissao.GERENCIAR_DADOS)
        carregarDespesa(eu, id)
        validarDespesa(eu, req)
        return despesas.save(montarDespesa(id, eu, req)).paraDto()
    }

    /** Apaga uma despesa. */
    fun removerDespesa(eu: UsuarioAutenticado, id: String) {
        eu.exigir(Permissao.GERENCIAR_DADOS)
        despesas.delete(carregarDespesa(eu, id))
    }

    // ------------------------------------------------------------ Doações

    /** Lista doações com filtros de tipo e período. */
    fun listarDoacoes(eu: UsuarioAutenticado, tipo: TipoDoacao?, de: Instant?, ate: Instant?, pagina: Int, tamanho: Int): Pagina<DoacaoDto> {
        eu.exigir(Permissao.GERENCIAR_DADOS)
        val c = mutableListOf(Criteria.where("idAbrigo").`is`(eu.abrigoId))
        tipo?.let { c += Criteria.where("tipo").`is`(it.name) }
        de?.let { c += Criteria.where("data").gte(it) }
        ate?.let { c += Criteria.where("data").lt(it) }
        val filtro = Criteria().andOperator(*c.toTypedArray())
        val tam = tamanho.coerceIn(1, 100)
        val pag = pagina.coerceAtLeast(0)
        val total = mongo.count(Query(filtro), Doacao::class.java)
        val itens = mongo.find(Query(filtro).with(PageRequest.of(pag, tam, Sort.by(Sort.Direction.DESC, "data"))), Doacao::class.java)
        return Pagina(itens.map { it.paraDto() }, pag, tam, total)
    }

    /** Registra uma doação (RF13): DINHEIRO exige valor; PRODUTO exige quantidade; ANIMAL exige o animal do abrigo. */
    fun criarDoacao(eu: UsuarioAutenticado, req: DoacaoRequest): DoacaoDto {
        eu.exigir(Permissao.GERENCIAR_DADOS)
        validarDoacao(eu, req)
        return doacoes.save(montarDoacao(null, eu, req)).paraDto()
    }

    /** Edita uma doação. */
    fun atualizarDoacao(eu: UsuarioAutenticado, id: String, req: DoacaoRequest): DoacaoDto {
        eu.exigir(Permissao.GERENCIAR_DADOS)
        carregarDoacao(eu, id)
        validarDoacao(eu, req)
        return doacoes.save(montarDoacao(id, eu, req)).paraDto()
    }

    /** Apaga uma doação. */
    fun removerDoacao(eu: UsuarioAutenticado, id: String) {
        eu.exigir(Permissao.GERENCIAR_DADOS)
        doacoes.delete(carregarDoacao(eu, id))
    }

    // ------------------------------------------------------------ Resumo e relatórios

    /** Resumo do mês ("2026-11"): doações em dinheiro, despesas e saldo (doações - despesas). */
    fun resumoDoMes(eu: UsuarioAutenticado, mes: String?): ResumoFinanceiroDto {
        eu.exigir(Permissao.GERENCIAR_DADOS)
        val ym = parseMes(mes)
        val inicio = ym.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        val fim = ym.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        val totalDoacoes = somaDoacoes(eu.abrigoId, inicio, fim)
        val totalDespesas = somaDespesas(eu.abrigoId, inicio, fim)
        return ResumoFinanceiroDto(ym.toString(), totalDoacoes, totalDespesas, arredondar(totalDoacoes - totalDespesas))
    }

    /** Relatório SEMANAL (segunda a domingo da data de referência) ou MENSAL (mês da referência), com despesas por tipo, adoções e resgates. */
    fun relatorio(eu: UsuarioAutenticado, periodo: String, referencia: LocalDate?): RelatorioDto {
        eu.exigir(Permissao.GERENCIAR_DADOS)
        val ref = referencia ?: LocalDate.now(ZoneOffset.UTC)
        val (ini, fimExclusivo) = when (periodo.uppercase()) {
            "SEMANAL" -> ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { it to it.plusDays(7) }
            "MENSAL" -> YearMonth.from(ref).let { it.atDay(1) to it.plusMonths(1).atDay(1) }
            else -> throw RegraNegocioException("O período deve ser SEMANAL ou MENSAL.")
        }
        val inicio = ini.atStartOfDay().toInstant(ZoneOffset.UTC)
        val fim = fimExclusivo.atStartOfDay().toInstant(ZoneOffset.UTC)
        val listaDespesas = mongo.find(Query(periodoCriterio(eu.abrigoId, "data", inicio, fim)), Despesa::class.java)
        val listaDoacoes = mongo.find(Query(periodoCriterio(eu.abrigoId, "data", inicio, fim)), Doacao::class.java)
        val totalDespesas = arredondar(listaDespesas.sumOf { it.custo })
        val totalDoacoes = arredondar(listaDoacoes.filter { it.tipo == TipoDoacao.DINHEIRO }.sumOf { it.valor ?: 0.0 })
        val adocoes = mongo.count(Query(Criteria.where("idAbrigo").`is`(eu.abrigoId).and("status").`is`(StatusAnimal.ADOTADO.name).and("inativadoEm").gte(inicio).lt(fim)), Animal::class.java)
        val resgates = mongo.count(Query(periodoCriterio(eu.abrigoId, "dataEntrada", inicio, fim)), Animal::class.java)
        return RelatorioDto(
            periodo = periodo.uppercase(), inicio = inicio, fim = fim.minusSeconds(1), totalDoacoes = totalDoacoes, totalDespesas = totalDespesas,
            saldo = arredondar(totalDoacoes - totalDespesas),
            despesasPorTipo = listaDespesas.groupBy { it.tipo }.mapValues { (_, v) -> arredondar(v.sumOf { it.custo }) },
            quantidadeDespesas = listaDespesas.size, quantidadeDoacoes = listaDoacoes.size, adocoesNoPeriodo = adocoes.toInt(), resgatesNoPeriodo = resgates.toInt(),
        )
    }

    /** Soma das doações em dinheiro do abrigo no intervalo (usada também pelo painel). */
    fun somaDoacoes(abrigoId: String, inicio: Instant, fim: Instant): Double =
        arredondar(mongo.find(Query(periodoCriterio(abrigoId, "data", inicio, fim).and("tipo").`is`(TipoDoacao.DINHEIRO.name)), Doacao::class.java).sumOf { it.valor ?: 0.0 })

    /** Soma das despesas do abrigo no intervalo. */
    fun somaDespesas(abrigoId: String, inicio: Instant, fim: Instant): Double =
        arredondar(mongo.find(Query(periodoCriterio(abrigoId, "data", inicio, fim)), Despesa::class.java).sumOf { it.custo })

    // ------------------------------------------------------------ Apoio

    private fun periodoCriterio(abrigoId: String, campo: String, inicio: Instant, fim: Instant): Criteria =
        Criteria.where("idAbrigo").`is`(abrigoId).and(campo).gte(inicio).lt(fim)

    private fun parseMes(mes: String?): YearMonth =
        try { if (mes.isNullOrBlank()) YearMonth.now(ZoneOffset.UTC) else YearMonth.parse(mes) } catch (e: Exception) { throw RegraNegocioException("Mês inválido. Use o formato AAAA-MM, ex.: 2026-11.") }

    private fun arredondar(v: Double): Double = Math.round(v * 100.0) / 100.0

    private fun carregarDespesa(eu: UsuarioAutenticado, id: String): Despesa =
        despesas.findById(id).orElse(null)?.takeIf { it.abrigoId == eu.abrigoId } ?: throw NaoEncontradoException("Despesa não encontrada.")

    private fun carregarDoacao(eu: UsuarioAutenticado, id: String): Doacao =
        doacoes.findById(id).orElse(null)?.takeIf { it.abrigoId == eu.abrigoId } ?: throw NaoEncontradoException("Doação não encontrada.")

    private fun exigirAnimalDoAbrigo(eu: UsuarioAutenticado, animalId: String) {
        animais.findById(animalId).orElse(null)?.takeIf { it.abrigoId == eu.abrigoId } ?: throw NaoEncontradoException("Animal não encontrado.")
    }

    private fun validarDespesa(eu: UsuarioAutenticado, req: DespesaRequest) {
        if (req.tipo == TipoDespesa.MEDICO && (req.animalId.isNullOrBlank() || req.medicoTipo == null)) {
            throw RegraNegocioException("Despesa médica precisa do animal e do tipo (cirurgia, medicamento ou exames).")
        }
        req.animalId?.takeIf { it.isNotBlank() }?.let { exigirAnimalDoAbrigo(eu, it) }
    }

    private fun montarDespesa(id: String?, eu: UsuarioAutenticado, req: DespesaRequest) = Despesa(
        id = id, abrigoId = eu.abrigoId, animalId = req.animalId?.takeIf { it.isNotBlank() }, tipo = requireNotNull(req.tipo),
        medicoTipo = if (req.tipo == TipoDespesa.MEDICO) req.medicoTipo else null, nome = req.nome.trim(), descricao = req.descricao.trim(),
        quantidade = req.quantidade, custo = req.custo, data = req.data ?: Instant.now(),
    )

    private fun validarDoacao(eu: UsuarioAutenticado, req: DoacaoRequest) {
        when (req.tipo) {
            TipoDoacao.DINHEIRO -> if (req.valor == null) throw RegraNegocioException("Informe o valor da doação.")
            TipoDoacao.PRODUTO -> if (req.quantidade == null || req.descricao.isBlank()) throw RegraNegocioException("Informe o produto e a quantidade.")
            TipoDoacao.ANIMAL -> exigirAnimalDoAbrigo(eu, req.animalId ?: throw RegraNegocioException("Informe o animal doado."))
            null -> Unit
        }
    }

    private fun montarDoacao(id: String?, eu: UsuarioAutenticado, req: DoacaoRequest) = Doacao(
        id = id, abrigoId = eu.abrigoId, tipo = requireNotNull(req.tipo), descricao = req.descricao.trim(), valor = req.valor, quantidade = req.quantidade,
        animalId = req.animalId?.takeIf { it.isNotBlank() }, doador = req.doador.trim(), data = req.data ?: Instant.now(),
    )
}
