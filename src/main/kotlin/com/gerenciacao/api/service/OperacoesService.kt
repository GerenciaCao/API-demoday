package com.gerenciacao.api.service

import com.gerenciacao.api.domain.*
import com.gerenciacao.api.dto.*
import com.gerenciacao.api.error.ConflitoException
import com.gerenciacao.api.error.NaoEncontradoException
import com.gerenciacao.api.error.RegraNegocioException
import com.gerenciacao.api.repository.*
import com.gerenciacao.api.security.UsuarioAutenticado
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Estoque do abrigo. Tudo exige GERENCIAR_ESTOQUE. */
@Service
class EstoqueService(private val estoque: EstoqueRepository) {

    /** Lista os itens do estoque (com a marca "abaixoDoMinimo"). */
    fun listar(eu: UsuarioAutenticado): List<EstoqueDto> {
        eu.exigir(Permissao.GERENCIAR_ESTOQUE)
        return estoque.findByAbrigoIdOrderByItem(eu.abrigoId).map { it.paraDto() }
    }

    /** Cadastra um item. */
    fun criar(eu: UsuarioAutenticado, req: EstoqueRequest): EstoqueDto {
        eu.exigir(Permissao.GERENCIAR_ESTOQUE)
        return estoque.save(montar(null, eu, req)).paraDto()
    }

    /** Edita um item (ex.: atualizar a quantidade). */
    fun atualizar(eu: UsuarioAutenticado, id: String, req: EstoqueRequest): EstoqueDto {
        eu.exigir(Permissao.GERENCIAR_ESTOQUE)
        carregar(eu, id)
        return estoque.save(montar(id, eu, req)).paraDto()
    }

    /** Apaga um item. */
    fun remover(eu: UsuarioAutenticado, id: String) {
        eu.exigir(Permissao.GERENCIAR_ESTOQUE)
        estoque.delete(carregar(eu, id))
    }

    private fun carregar(eu: UsuarioAutenticado, id: String): ItemEstoque =
        estoque.findById(id).orElse(null)?.takeIf { it.abrigoId == eu.abrigoId } ?: throw NaoEncontradoException("Item não encontrado.")

    private fun montar(id: String?, eu: UsuarioAutenticado, req: EstoqueRequest) = ItemEstoque(
        id = id, abrigoId = eu.abrigoId, item = req.item.trim(), categoria = requireNotNull(req.categoria), quantidade = req.quantidade,
        unidade = req.unidade.trim().ifEmpty { "unidades" }, estoqueMinimo = req.estoqueMinimo, atualizadoEm = Instant.now(),
    )
}

/** Calendário do abrigo (RF15 a RF18). Todos os membros veem; criar/editar/apagar exige GERENCIAR_AGENDA. */
@Service
class AgendaService(
    private val eventos: EventoRepository,
    private val animais: AnimalRepository,
    private val usuarios: UsuarioRepository,
    private val mongo: MongoTemplate,
) {
    /** Lista os eventos entre [de] (inclusive) e [ate] (exclusive); sem datas, devolve os próximos 31 dias. */
    fun listar(eu: UsuarioAutenticado, de: Instant?, ate: Instant?): List<EventoDto> {
        val inicio = de ?: Instant.now().truncatedTo(ChronoUnit.DAYS)
        val fim = ate ?: inicio.plus(31, ChronoUnit.DAYS)
        val consulta = Query(Criteria.where("idAbrigo").`is`(eu.abrigoId).and("dataHoraInicio").gte(inicio).lt(fim)).with(Sort.by("dataHoraInicio"))
        return mongo.find(consulta, Evento::class.java).map { it.paraDto() }
    }

    /** Cria um evento. O fim padrão é 1 hora depois do início. */
    fun criar(eu: UsuarioAutenticado, req: EventoRequest): EventoDto {
        eu.exigir(Permissao.GERENCIAR_AGENDA)
        validar(eu, req)
        return eventos.save(montar(null, eu, req)).paraDto()
    }

    /** Edita ou reagenda um evento (RF16). */
    fun atualizar(eu: UsuarioAutenticado, id: String, req: EventoRequest): EventoDto {
        eu.exigir(Permissao.GERENCIAR_AGENDA)
        carregar(eu, id)
        validar(eu, req)
        return eventos.save(montar(id, eu, req)).paraDto()
    }

    /** Apaga um evento (RF17). */
    fun remover(eu: UsuarioAutenticado, id: String) {
        eu.exigir(Permissao.GERENCIAR_AGENDA)
        eventos.delete(carregar(eu, id))
    }

    private fun carregar(eu: UsuarioAutenticado, id: String): Evento =
        eventos.findById(id).orElse(null)?.takeIf { it.abrigoId == eu.abrigoId } ?: throw NaoEncontradoException("Evento não encontrado.")

    private fun validar(eu: UsuarioAutenticado, req: EventoRequest) {
        val inicio = requireNotNull(req.dataHoraInicio)
        if (req.dataHoraFim != null && !req.dataHoraFim.isAfter(inicio)) throw RegraNegocioException("O fim deve ser depois do início.")
        req.animalId?.takeIf { it.isNotBlank() }?.let { id ->
            animais.findById(id).orElse(null)?.takeIf { it.abrigoId == eu.abrigoId } ?: throw NaoEncontradoException("Animal não encontrado.")
        }
        req.responsavelId?.takeIf { it.isNotBlank() }?.let { id ->
            usuarios.findById(id).orElse(null)?.takeIf { it.abrigoId == eu.abrigoId } ?: throw NaoEncontradoException("Responsável não encontrado.")
        }
    }

    private fun montar(id: String?, eu: UsuarioAutenticado, req: EventoRequest): Evento {
        val inicio = requireNotNull(req.dataHoraInicio)
        return Evento(
            id = id, abrigoId = eu.abrigoId, animalId = req.animalId?.takeIf { it.isNotBlank() }, titulo = req.titulo.trim(), tipoEvento = req.tipoEvento,
            descricao = req.descricao.trim(), dataHoraInicio = inicio, dataHoraFim = req.dataHoraFim ?: inicio.plus(1, ChronoUnit.HOURS),
            status = req.status, recorrencia = req.recorrencia, responsavel = req.responsavel.trim(), responsavelId = req.responsavelId?.takeIf { it.isNotBlank() },
            cor = req.cor, lembrete = req.lembrete,
        )
    }
}

/** Processo de adoção (RF27 a RF29). Ver = VER_FICHAS; mexer = EDITAR_PRONTUARIOS. Guarda dados pessoais do interessado. */
@Service
class AdocaoService(
    private val adocoes: AdocaoRepository,
    private val animais: AnimalRepository,
    private val mongo: MongoTemplate,
) {
    /** Lista as adoções em andamento (ou todas, se [soEmAndamento] for false). */
    fun listar(eu: UsuarioAutenticado, soEmAndamento: Boolean): List<AdocaoDto> {
        eu.exigir(Permissao.VER_FICHAS)
        val c = Criteria.where("idAbrigo").`is`(eu.abrigoId)
        if (soEmAndamento) c.and("concluidoEm").`is`(null).and("cancelada").`is`(false)
        val lista = mongo.find(Query(c).with(Sort.by(Sort.Direction.DESC, "criadoEm")), Adocao::class.java)
        val nomes = animais.findAllById(lista.map { it.animalId }).associate { it.id to it.nome }
        return lista.map { it.paraDto(nomes[it.animalId] ?: "—") }
    }

    /** Registra uma pessoa interessada em um bichinho: abre o processo e deixa o animal "em processo de adoção". */
    fun criar(eu: UsuarioAutenticado, req: NovaAdocaoRequest): AdocaoDto {
        eu.exigir(Permissao.EDITAR_PRONTUARIOS)
        val animal = carregarAnimal(eu, req.animalId)
        if (!animal.ativo || animal.status !in listOf(StatusAnimal.DISPONIVEL, StatusAnimal.EM_PROCESSO_ADOCAO)) {
            throw RegraNegocioException("Este bichinho não está disponível para adoção.")
        }
        val agora = Instant.now()
        val existente = mongo.count(Query(Criteria.where("idAnimal").`is`(animal.id).and("concluidoEm").`is`(null).and("cancelada").`is`(false)), Adocao::class.java)
        if (existente > 0) throw ConflitoException("Já existe um processo de adoção em andamento para este bichinho.")
        val salva = adocoes.save(
            Adocao(
                abrigoId = eu.abrigoId, animalId = requireNotNull(animal.id),
                interessado = Interessado(req.interessado.nome.trim(), req.interessado.telefone.trim(), req.interessado.email.trim()),
                historicoEtapas = listOf(RegistroEtapa(EtapaAdocao.PESSOA_INTERESSADA, agora, "Interesse registrado")),
            ),
        )
        animais.save(animal.copy(status = StatusAnimal.EM_PROCESSO_ADOCAO, etapaAdocao = EtapaAdocao.PESSOA_INTERESSADA))
        return salva.paraDto(animal.nome)
    }

    /** Avança (ou volta) a etapa do processo e registra no histórico. */
    fun mudarEtapa(eu: UsuarioAutenticado, id: String, req: EtapaRequest): AdocaoDto {
        eu.exigir(Permissao.EDITAR_PRONTUARIOS)
        val ad = carregarEmAndamento(eu, id)
        val etapa = requireNotNull(req.etapa)
        val animal = carregarAnimal(eu, ad.animalId)
        animais.save(animal.copy(status = StatusAnimal.EM_PROCESSO_ADOCAO, etapaAdocao = etapa))
        val salva = adocoes.save(ad.copy(etapa = etapa, historicoEtapas = ad.historicoEtapas + RegistroEtapa(etapa, Instant.now(), req.observacao.trim())))
        return salva.paraDto(animal.nome)
    }

    /** Conclui a adoção: o bichinho vira ADOTADO e a ficha fica inativa (libera a vaga). */
    fun concluir(eu: UsuarioAutenticado, id: String): AdocaoDto {
        eu.exigir(Permissao.EDITAR_PRONTUARIOS)
        val ad = carregarEmAndamento(eu, id)
        val animal = carregarAnimal(eu, ad.animalId)
        val agora = Instant.now()
        animais.save(animal.copy(status = StatusAnimal.ADOTADO, etapaAdocao = null, ativo = false, inativadoEm = agora))
        return adocoes.save(ad.copy(concluidoEm = agora)).paraDto(animal.nome)
    }

    /** Cancela o processo: o bichinho volta a ficar disponível. */
    fun cancelar(eu: UsuarioAutenticado, id: String): AdocaoDto {
        eu.exigir(Permissao.EDITAR_PRONTUARIOS)
        val ad = carregarEmAndamento(eu, id)
        val animal = carregarAnimal(eu, ad.animalId)
        animais.save(animal.copy(status = StatusAnimal.DISPONIVEL, etapaAdocao = null))
        return adocoes.save(ad.copy(cancelada = true)).paraDto(animal.nome)
    }

    private fun carregarEmAndamento(eu: UsuarioAutenticado, id: String): Adocao {
        val ad = adocoes.findById(id).orElse(null)?.takeIf { it.abrigoId == eu.abrigoId } ?: throw NaoEncontradoException("Processo de adoção não encontrado.")
        if (ad.concluidoEm != null || ad.cancelada) throw RegraNegocioException("Este processo de adoção já foi encerrado.")
        return ad
    }

    private fun carregarAnimal(eu: UsuarioAutenticado, id: String): Animal =
        animais.findById(id).orElse(null)?.takeIf { it.abrigoId == eu.abrigoId } ?: throw NaoEncontradoException("Animal não encontrado.")

    private fun Adocao.paraDto(nome: String) = AdocaoDto(requireNotNull(id), animalId, nome, interessado, etapa, historicoEtapas, criadoEm, concluidoEm, cancelada)
}
