package com.gerenciacao.api.service

import com.gerenciacao.api.config.PlataformaProperties
import com.gerenciacao.api.domain.*
import com.gerenciacao.api.dto.*
import com.gerenciacao.api.error.AcessoNegadoException
import com.gerenciacao.api.error.ConflitoException
import com.gerenciacao.api.error.NaoEncontradoException
import com.gerenciacao.api.error.RegraNegocioException
import com.gerenciacao.api.repository.*
import com.gerenciacao.api.security.UsuarioAutenticado
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern

/** Anúncios de Parcerias (RF19 a RF22). Qualquer usuário logado vê; só administradores da plataforma criam, editam e apagam. */
@Service
class ParceriaService(
    private val parcerias: ParceriaRepository,
    private val usuarios: UsuarioRepository,
    private val plataforma: PlataformaProperties,
    private val mongo: MongoTemplate,
) {
    /** Lista anúncios ativos e dentro da validade, com filtros (tipo, categoria, destaque, texto), por relevância (destaques primeiro). */
    fun listar(tipo: TipoParceria?, categoria: String?, busca: String?, destaque: Boolean?): List<ParceriaDto> {
        val c = mutableListOf(Criteria.where("ativo").`is`(true))
        tipo?.let { c += Criteria.where("tipo").`is`(it.name) }
        destaque?.let { c += Criteria.where("destaque").`is`(it) }
        categoria?.trim()?.takeIf { it.isNotEmpty() }?.let { c += Criteria.where("categoria").regex("^" + Pattern.quote(it) + "$", "i") }
        busca?.trim()?.takeIf { it.isNotEmpty() }?.let {
            val rx = Pattern.quote(it)
            c += Criteria().orOperator(Criteria.where("titulo").regex(rx, "i"), Criteria.where("parceiro").regex(rx, "i"), Criteria.where("categoria").regex(rx, "i"))
        }
        c += Criteria().orOperator(Criteria.where("validoAte").`is`(null), Criteria.where("validoAte").gte(Instant.now()))
        val consulta = Query(Criteria().andOperator(*c.toTypedArray())).with(Sort.by(Sort.Direction.DESC, "destaque").and(Sort.by("titulo")))
        return mongo.find(consulta, Parceria::class.java).map { it.paraDto() }
    }

    /** Cria um anúncio (só administrador da plataforma). */
    fun criar(eu: UsuarioAutenticado, req: ParceriaRequest): ParceriaDto {
        exigirAdminPlataforma(eu)
        return parcerias.save(montar(null, req)).paraDto()
    }

    /** Edita um anúncio. */
    fun atualizar(eu: UsuarioAutenticado, id: String, req: ParceriaRequest): ParceriaDto {
        exigirAdminPlataforma(eu)
        parcerias.findById(id).orElse(null) ?: throw NaoEncontradoException("Anúncio não encontrado.")
        return parcerias.save(montar(id, req)).paraDto()
    }

    /** Apaga um anúncio. */
    fun remover(eu: UsuarioAutenticado, id: String) {
        exigirAdminPlataforma(eu)
        parcerias.delete(parcerias.findById(id).orElse(null) ?: throw NaoEncontradoException("Anúncio não encontrado."))
    }

    private fun exigirAdminPlataforma(eu: UsuarioAutenticado) {
        val email = usuarios.findById(eu.id).orElse(null)?.email ?: throw AcessoNegadoException()
        if (!plataforma.ehAdmin(email)) throw AcessoNegadoException("Somente administradores da plataforma gerenciam as parcerias.")
    }

    private fun montar(id: String?, r: ParceriaRequest) = Parceria(
        id = id, tipo = requireNotNull(r.tipo), titulo = r.titulo.trim(), categoria = r.categoria.trim(), descricao = r.descricao.trim(), preco = r.preco,
        parceiro = r.parceiro.trim(), linkUrl = r.linkUrl.trim(), imagemUrl = r.imagemUrl?.trim()?.ifEmpty { null }, cupom = r.cupom?.trim()?.ifEmpty { null },
        desconto = r.desconto, destaque = r.destaque, regiao = r.regiao?.trim()?.ifEmpty { null }, ativo = r.ativo, validoAte = r.validoAte,
    )
}

/** Rede de abrigos (tela Conexão): ver outros abrigos e pedir/aceitar/desfazer conexões. Pedir e aceitar exige administrador ou gestor. */
@Service
class RedeService(
    private val abrigos: AbrigoRepository,
    private val conexoes: ConexaoRepository,
    private val mongo: MongoTemplate,
) {
    /** Lista os OUTROS abrigos (só dados públicos) com a situação da conexão com o seu abrigo. */
    fun listar(eu: UsuarioAutenticado, busca: String?, somenteConexoes: Boolean): List<RedeAbrigoDto> {
        val minhas = conexoes.findByAbrigoAIdOrAbrigoBId(eu.abrigoId, eu.abrigoId)
        val porOutro = minhas.associateBy { if (it.abrigoAId == eu.abrigoId) it.abrigoBId else it.abrigoAId }
        val rx = busca?.trim()?.takeIf { it.isNotEmpty() }?.let { Regex(Pattern.quote(it), RegexOption.IGNORE_CASE) }
        val candidatos = abrigos.findAll().filter { it.id != eu.abrigoId && (rx == null || rx.containsMatchIn(it.nome)) }
        val fotos = fotoDosAbrigos(candidatos.mapNotNull { it.id })
        return candidatos.mapNotNull { a ->
            val c = porOutro[a.id]
            val situacao = when {
                c == null || c.status == StatusConexao.RECUSADA -> SituacaoConexao.NENHUMA
                c.status == StatusConexao.ACEITA -> SituacaoConexao.CONECTADA
                c.abrigoAId == eu.abrigoId -> SituacaoConexao.PENDENTE_ENVIADA
                else -> SituacaoConexao.PENDENTE_RECEBIDA
            }
            if (somenteConexoes && situacao != SituacaoConexao.CONECTADA) null
            else RedeAbrigoDto(requireNotNull(a.id), a.nome, a.endereco, a.descricao, fotos[a.id], situacao, c?.takeIf { situacao != SituacaoConexao.NENHUMA }?.id)
        }.sortedBy { it.nome.lowercase() }
    }

    /** Pede conexão com outro abrigo. */
    fun conectar(eu: UsuarioAutenticado, abrigoId: String): RedeAbrigoDto {
        eu.exigirGestao()
        if (abrigoId == eu.abrigoId) throw RegraNegocioException("Não dá para se conectar ao próprio abrigo.")
        abrigos.findById(abrigoId).orElse(null) ?: throw NaoEncontradoException("Abrigo não encontrado.")
        val ja = conexoes.findByAbrigoAIdOrAbrigoBId(eu.abrigoId, eu.abrigoId).firstOrNull {
            (it.abrigoAId == abrigoId || it.abrigoBId == abrigoId) && it.status != StatusConexao.RECUSADA
        }
        if (ja != null) throw ConflitoException("Já existe uma conexão ou um pedido com este abrigo.")
        conexoes.save(Conexao(abrigoAId = eu.abrigoId, abrigoBId = abrigoId))
        return listar(eu, null, false).first { it.abrigoId == abrigoId }
    }

    /** O abrigo que RECEBEU o pedido aceita. */
    fun aceitar(eu: UsuarioAutenticado, conexaoId: String) {
        eu.exigirGestao()
        val c = conexoes.findById(conexaoId).orElse(null)?.takeIf { it.abrigoBId == eu.abrigoId && it.status == StatusConexao.PENDENTE }
            ?: throw NaoEncontradoException("Pedido de conexão não encontrado.")
        conexoes.save(c.copy(status = StatusConexao.ACEITA))
    }

    /** Qualquer um dos dois abrigos desfaz a conexão ou recusa o pedido. */
    fun remover(eu: UsuarioAutenticado, conexaoId: String) {
        eu.exigirGestao()
        val c = conexoes.findById(conexaoId).orElse(null)?.takeIf { it.abrigoAId == eu.abrigoId || it.abrigoBId == eu.abrigoId }
            ?: throw NaoEncontradoException("Conexão não encontrada.")
        conexoes.delete(c)
    }

    private fun fotoDosAbrigos(ids: List<String>): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        val q = Query(Criteria.where("donoTipo").`is`(DonoFoto.ABRIGO.name).and("donoId").`in`(ids))
        q.fields().exclude("dados")
        return mongo.find(q, FotoMeta::class.java).associate { it.donoId to requireNotNull(it.id) }
    }
}

/** Painel da tela Início: ocupação, próximos eventos, animais recentes, adoções em andamento e finanças do mês (se tiver permissão). */
@Service
class DashboardService(
    private val abrigos: AbrigoRepository,
    private val animaisRef: AnimalRefRepository,
    private val mongo: MongoTemplate,
    private val financeiro: FinanceiroService,
    private val animalService: AnimalService,
) {
    /** Monta o painel do abrigo do usuário logado. */
    fun painel(eu: UsuarioAutenticado): DashboardDto {
        val abrigo = abrigos.findById(eu.abrigoId).orElse(null) ?: throw NaoEncontradoException("Abrigo não encontrado.")
        val ativos = animaisRef.countByAbrigoIdAndAtivo(eu.abrigoId, true).toInt()
        val agora = Instant.now()
        val eventos = mongo.find(
            Query(Criteria.where("idAbrigo").`is`(eu.abrigoId).and("status").`is`(StatusEvento.AGENDADO.name).and("dataHoraInicio").gte(agora)).with(Sort.by("dataHoraInicio")).limit(5),
            Evento::class.java,
        ).map { it.paraDto() }
        val recentes = if (eu.pode(Permissao.VER_FICHAS)) animalService.listar(eu, null, null, null, false, 0, 100).itens.sortedByDescending { it.dataEntrada }.take(5) else emptyList()
        val emAndamento = mongo.count(Query(Criteria.where("idAbrigo").`is`(eu.abrigoId).and("concluidoEm").`is`(null).and("cancelada").`is`(false)), Adocao::class.java).toInt()
        val mes = java.time.YearMonth.now(ZoneOffset.UTC)
        val financas = if (eu.pode(Permissao.GERENCIAR_DADOS)) financeiro.resumoDoMes(eu, mes.toString()) else null
        return DashboardDto(
            ocupacao = OcupacaoDto(abrigo.capacidade, ativos, (abrigo.capacidade - ativos).coerceAtLeast(0)),
            proximosEventos = eventos, animaisRecentes = recentes, adocoesEmAndamento = emAndamento, financeiroMes = financas,
        )
    }
}
