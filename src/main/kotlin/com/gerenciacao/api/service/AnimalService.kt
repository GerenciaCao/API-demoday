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
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern

/** Fichas dos bichinhos, vacinas e histórico médico (RF09 a RF11). Ver = VER_FICHAS; criar/editar/apagar = EDITAR_PRONTUARIOS. */
@Service
class AnimalService(
    private val animais: AnimalRepository,
    private val animaisRef: AnimalRefRepository,
    private val vacinas: VacinaRepository,
    private val historico: HistoricoRepository,
    private val fotos: FotoRepository,
    private val metas: FotoMetaRepository,
    private val adocoes: AdocaoRepository,
    private val eventos: EventoRepository,
    private val abrigos: AbrigoRepository,
    private val mongo: MongoTemplate,
) {
    /** Lista os animais do abrigo com filtros (espécie, status, nome) e paginação. Por padrão só os ativos. */
    fun listar(eu: UsuarioAutenticado, especie: Especie?, status: StatusAnimal?, busca: String?, incluirInativos: Boolean, pagina: Int, tamanho: Int): Pagina<AnimalDto> {
        eu.exigir(Permissao.VER_FICHAS)
        val criterios = mutableListOf(Criteria.where("idAbrigo").`is`(eu.abrigoId))
        if (!incluirInativos) criterios += Criteria.where("ativo").ne(false)
        especie?.let { criterios += Criteria.where("especie").`is`(it.name) }
        status?.let { criterios += Criteria.where("status").`is`(it.name) }
        busca?.trim()?.takeIf { it.isNotEmpty() }?.let { criterios += Criteria.where("nome").regex(Pattern.quote(it), "i") }
        val filtro = Criteria().andOperator(*criterios.toTypedArray())
        val tam = tamanho.coerceIn(1, 100)
        val pag = pagina.coerceAtLeast(0)
        val total = mongo.count(Query(filtro), Animal::class.java)
        val itens = mongo.find(Query(filtro).with(PageRequest.of(pag, tam, Sort.by("nome"))), Animal::class.java)
        val fotosPrincipais = fotoPrincipal(itens.mapNotNull { it.id })
        return Pagina(itens.map { it.paraDto(fotosPrincipais[it.id]) }, pag, tam, total)
    }

    /** Ficha completa: dados, vacinas, histórico médico e fotos. */
    fun detalhe(eu: UsuarioAutenticado, id: String): AnimalDetalheDto {
        eu.exigir(Permissao.VER_FICHAS)
        val a = carregar(eu, id)
        val metasFotos = metas.listar(DonoFoto.ANIMAL.name, id).map {
            FotoDto(requireNotNull(it.id), it.donoTipo, it.donoId, it.principal, it.contentType, it.tamanhoBytes, "/api/fotos/${it.id}")
        }
        return AnimalDetalheDto(
            animal = a.paraDto(metasFotos.firstOrNull { it.principal }?.id),
            vacinas = vacinas.findByAnimalIdOrderByDataDesc(id).map { it.paraDto() },
            historico = historico.findByAnimalIdOrderByDataConsultaDesc(id).map { it.paraDto() },
            fotos = metasFotos,
        )
    }

    /** Cria uma ficha. Recusa se o abrigo já está na capacidade máxima. Status inicial: disponível, em tratamento ou lar temporário. */
    fun criar(eu: UsuarioAutenticado, req: NovoAnimalRequest): AnimalDto {
        eu.exigir(Permissao.EDITAR_PRONTUARIOS)
        if (req.status == StatusAnimal.EM_PROCESSO_ADOCAO || req.status == StatusAnimal.ADOTADO) {
            throw RegraNegocioException("Uma ficha nova começa como disponível, em tratamento ou em lar temporário.")
        }
        val abrigo = abrigos.findById(eu.abrigoId).orElse(null)
        if (abrigo != null && abrigo.capacidade > 0 && animaisRef.countByAbrigoIdAndAtivo(eu.abrigoId, true) >= abrigo.capacidade) {
            throw RegraNegocioException("O abrigo está na capacidade máxima (${abrigo.capacidade}). Aumente a capacidade em Informações do abrigo ou dê baixa em algum bichinho.")
        }
        val salvo = animais.save(
            Animal(
                abrigoId = eu.abrigoId, nome = req.nome.trim(), genero = requireNotNull(req.genero), especie = requireNotNull(req.especie),
                raca = req.raca.trim().ifEmpty { "SRD" }, nascimento = req.nascimento, castrado = req.castrado, porte = req.porte,
                status = req.status, microchip = req.microchip?.trim()?.ifEmpty { null }, pesoKg = req.pesoKg,
                caracteristicas = req.caracteristicas.trim(), descricao = req.descricao.trim(),
            ),
        )
        return salvo.paraDto()
    }

    /** Edita a ficha (só os campos enviados mudam). Microchip repetido responde 409. */
    fun atualizar(eu: UsuarioAutenticado, id: String, req: AtualizarAnimalRequest): AnimalDto {
        eu.exigir(Permissao.EDITAR_PRONTUARIOS)
        val a = carregar(eu, id)
        val salvo = animais.save(
            a.copy(
                nome = req.nome?.trim() ?: a.nome, genero = req.genero ?: a.genero, especie = req.especie ?: a.especie,
                raca = req.raca?.trim()?.ifEmpty { "SRD" } ?: a.raca, nascimento = req.nascimento ?: a.nascimento,
                castrado = req.castrado ?: a.castrado, porte = req.porte ?: a.porte,
                microchip = req.microchip?.trim()?.ifEmpty { null } ?: a.microchip, pesoKg = req.pesoKg ?: a.pesoKg,
                caracteristicas = req.caracteristicas?.trim() ?: a.caracteristicas, descricao = req.descricao?.trim() ?: a.descricao,
            ),
        )
        return salvo.paraDto(fotoPrincipal(listOf(id))[id])
    }

    /** Muda a situação do bichinho. ADOTADO deixa a ficha inativa (não ocupa vaga); EM_PROCESSO_ADOCAO exige a etapa (RF28 e RF29). */
    fun mudarStatus(eu: UsuarioAutenticado, id: String, req: MudarStatusRequest): AnimalDto {
        eu.exigir(Permissao.EDITAR_PRONTUARIOS)
        val a = carregar(eu, id)
        val status = requireNotNull(req.status)
        if (status == StatusAnimal.EM_PROCESSO_ADOCAO && req.etapaAdocao == null) throw RegraNegocioException("Informe a etapa da adoção.")
        val adotado = status == StatusAnimal.ADOTADO
        val salvo = animais.save(
            a.copy(
                status = status,
                etapaAdocao = if (status == StatusAnimal.EM_PROCESSO_ADOCAO) req.etapaAdocao else null,
                ativo = !adotado,
                inativadoEm = if (adotado) a.inativadoEm ?: Instant.now() else null,
            ),
        )
        return salvo.paraDto(fotoPrincipal(listOf(id))[id])
    }

    /** Apaga a ficha e tudo que é dela (vacinas, histórico, fotos e processos de adoção). */
    fun remover(eu: UsuarioAutenticado, id: String) {
        eu.exigir(Permissao.EDITAR_PRONTUARIOS)
        val a = carregar(eu, id)
        vacinas.deleteByAnimalId(id)
        historico.deleteByAnimalId(id)
        fotos.deleteByDonoTipoAndDonoId(DonoFoto.ANIMAL, id)
        adocoes.deleteByAnimalId(id)
        animais.delete(a)
    }

    /** Registra uma vacina. Se houver data da próxima dose, cria um lembrete no calendário (automação do diário do banco). */
    fun adicionarVacina(eu: UsuarioAutenticado, id: String, req: NovaVacinaRequest): VacinaDto {
        eu.exigir(Permissao.EDITAR_PRONTUARIOS)
        val a = carregar(eu, id)
        val v = vacinas.save(
            Vacina(
                animalId = id, nome = req.nome.trim(), data = requireNotNull(req.data), lote = req.lote.trim(), veterinario = req.veterinario.trim(),
                dataProximaDose = req.dataProximaDose, dataValidade = req.dataValidade, fabricante = req.fabricante?.trim()?.ifEmpty { null },
            ),
        )
        req.dataProximaDose?.let {
            eventos.save(
                Evento(
                    abrigoId = eu.abrigoId, animalId = id, titulo = "Reforço de ${v.nome} — ${a.nome}", tipoEvento = TipoEvento.CONSULTA_VETERINARIA,
                    descricao = "Próxima dose da vacina ${v.nome}.", dataHoraInicio = it, dataHoraFim = it.plus(1, ChronoUnit.HOURS), notificarAutomatizado = true,
                ),
            )
        }
        return v.paraDto()
    }

    /** Apaga uma vacina da ficha. */
    fun removerVacina(eu: UsuarioAutenticado, id: String, vacinaId: String) {
        eu.exigir(Permissao.EDITAR_PRONTUARIOS)
        carregar(eu, id)
        val v = vacinas.findById(vacinaId).orElse(null)?.takeIf { it.animalId == id } ?: throw NaoEncontradoException("Vacina não encontrada.")
        vacinas.delete(v)
    }

    /** Registra uma consulta no histórico médico. */
    fun adicionarHistorico(eu: UsuarioAutenticado, id: String, req: NovoHistoricoRequest): HistoricoDto {
        eu.exigir(Permissao.EDITAR_PRONTUARIOS)
        carregar(eu, id)
        return historico.save(
            HistoricoMedico(animalId = id, dataConsulta = requireNotNull(req.dataConsulta), diagnostico = req.diagnostico.trim(), tratamento = req.tratamento?.trim()?.ifEmpty { null }, veterinario = req.veterinario.trim()),
        ).paraDto()
    }

    /** Apaga uma consulta do histórico. */
    fun removerHistorico(eu: UsuarioAutenticado, id: String, historicoId: String) {
        eu.exigir(Permissao.EDITAR_PRONTUARIOS)
        carregar(eu, id)
        val h = historico.findById(historicoId).orElse(null)?.takeIf { it.animalId == id } ?: throw NaoEncontradoException("Registro não encontrado.")
        historico.delete(h)
    }

    /** Carrega o animal garantindo que é do mesmo abrigo (senão, 404). */
    private fun carregar(eu: UsuarioAutenticado, id: String): Animal =
        animais.findById(id).orElse(null)?.takeIf { it.abrigoId == eu.abrigoId } ?: throw NaoEncontradoException("Animal não encontrado.")

    /** Id da foto principal de cada animal (uma consulta só, sem trazer os bytes). */
    private fun fotoPrincipal(ids: List<String>): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        val consulta = Query(Criteria.where("donoTipo").`is`(DonoFoto.ANIMAL.name).and("donoId").`in`(ids).and("principal").`is`(true))
        consulta.fields().exclude("dados")
        return mongo.find(consulta, FotoMeta::class.java).associate { it.donoId to requireNotNull(it.id) }
    }
}
