package com.gerenciacao.api.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant

// Documentos dos módulos. Os nomes dos campos no MongoDB seguem o banco real (GerenciaCaoDB, ver Diario_do_Banco_v2.md):
// idAbrigo, idAnimal etc.; listas fixas gravadas pelo NOME do enum (MAIÚSCULAS sem acento); datas como Date.

enum class Genero { FEMEA, MACHO }
enum class Especie { CACHORRO, GATO }
enum class Porte { PEQUENO, MEDIO, GRANDE }
enum class StatusAnimal { DISPONIVEL, EM_PROCESSO_ADOCAO, EM_TRATAMENTO, LAR_TEMPORARIO, ADOTADO }
enum class EtapaAdocao { PESSOA_INTERESSADA, ENTREVISTA, VISITA, ACOMPANHAMENTO }
enum class TipoDespesa { COMIDA, MEDICO, ENTRETENIMENTO, HIGIENICO }
enum class TipoMedico { CIRURGIA, POSOLOGIA, EXAMES }
enum class TipoDoacao { DINHEIRO, PRODUTO, ANIMAL }
enum class CategoriaEstoque { RACAO_GATO, RACAO_CACHORRO, MEDICAMENTO, LIMPEZA, OUTROS }
enum class TipoEvento { VISITA_ADOCAO, CONSULTA_VETERINARIA, HIGIENE, SERVICO, CUIDADO }
enum class StatusEvento { AGENDADO, CONCLUIDO, CANCELADO }
enum class Recorrencia { NENHUMA, DIARIA, SEMANAL, MENSAL }
enum class TipoParceria { PRODUTO, SERVICO }
enum class StatusConexao { PENDENTE, ACEITA, RECUSADA }
enum class TipoToken { RECUPERACAO_SENHA, VALIDACAO_EMAIL }

/** Ficha do bichinho (coleção "animais"). Quando adotado, fica inativo (ativo = false) e deixa de ocupar vaga. */
@Document(collection = "animais")
data class Animal(
    @Id val id: String? = null,
    @Field("idAbrigo") @Indexed val abrigoId: String,
    val nome: String,
    val genero: Genero,
    val especie: Especie,
    val raca: String = "SRD",
    val nascimento: Instant? = null,
    val dataEntrada: Instant = Instant.now(),
    val castrado: Boolean = false,
    val porte: Porte = Porte.MEDIO,
    val status: StatusAnimal = StatusAnimal.DISPONIVEL,
    val etapaAdocao: EtapaAdocao? = null,
    val ativo: Boolean = true,
    val inativadoEm: Instant? = null,
    val urlFoto: String? = null, // legado; as fotos ficam na coleção "fotos"
    val microchip: String? = null,
    val pesoKg: Double? = null,
    val caracteristicas: String = "",
    val descricao: String = "",
)

/** Vacina aplicada a um animal (coleção "vacinas"). "dataProximaDose" alimenta o calendário. */
@Document(collection = "vacinas")
data class Vacina(
    @Id val id: String? = null,
    @Field("idAnimal") @Indexed val animalId: String,
    val nome: String,
    val data: Instant,
    val lote: String = "",
    val veterinario: String = "",
    val dataProximaDose: Instant? = null,
    val dataValidade: Instant? = null,
    val fabricante: String? = null,
)

/** Consulta/diagnóstico de um animal (coleção "historico_medico"). */
@Document(collection = "historico_medico")
data class HistoricoMedico(
    @Id val id: String? = null,
    @Field("idAnimal") @Indexed val animalId: String,
    val dataConsulta: Instant,
    val diagnostico: String,
    val tratamento: String? = null,
    val veterinario: String,
    @Field("idDespesa") val despesaId: String? = null,
)

/** Despesa do abrigo (coleção "despesas"). Despesa MEDICA precisa de animal e tipo médico. */
@Document(collection = "despesas")
data class Despesa(
    @Id val id: String? = null,
    @Field("idAbrigo") @Indexed val abrigoId: String,
    @Field("idAnimal") val animalId: String? = null,
    val tipo: TipoDespesa,
    val medicoTipo: TipoMedico? = null,
    val nome: String,
    val descricao: String = "",
    val quantidade: Int = 1,
    val custo: Double,
    val data: Instant,
)

/** Doação recebida (coleção "doacoes"): dinheiro, produtos ou um animal. */
@Document(collection = "doacoes")
data class Doacao(
    @Id val id: String? = null,
    @Field("idAbrigo") @Indexed val abrigoId: String,
    val tipo: TipoDoacao,
    val descricao: String = "",
    val valor: Double? = null,
    val quantidade: Int? = null,
    @Field("idAnimal") val animalId: String? = null,
    val doador: String = "",
    val data: Instant,
    val origem: String? = null,
)

/** Item do estoque do abrigo (coleção "estoque"). */
@Document(collection = "estoque")
data class ItemEstoque(
    @Id val id: String? = null,
    @Field("idAbrigo") @Indexed val abrigoId: String,
    val item: String,
    val categoria: CategoriaEstoque,
    val quantidade: Int,
    val unidade: String = "unidades",
    val estoqueMinimo: Int = 0,
    val atualizadoEm: Instant = Instant.now(),
    val origem: String? = null,
)

/** Evento do calendário (coleção "eventos_calendario"). */
@Document(collection = "eventos_calendario")
data class Evento(
    @Id val id: String? = null,
    @Field("idAbrigo") @Indexed val abrigoId: String,
    @Field("idAnimal") val animalId: String? = null,
    val titulo: String,
    val tipoEvento: TipoEvento = TipoEvento.CUIDADO,
    val descricao: String = "",
    val dataHoraInicio: Instant,
    val dataHoraFim: Instant,
    val status: StatusEvento = StatusEvento.AGENDADO,
    val notificarAutomatizado: Boolean = false,
    val recorrencia: Recorrencia = Recorrencia.NENHUMA,
    val responsavel: String = "",
    @Field("responsavelId") val responsavelId: String? = null,
    val cor: String? = null,
    val lembrete: String? = null,
)

/** Pessoa interessada em adotar. Dados pessoais: só quem tem permissão de ver fichas enxerga. */
data class Interessado(val nome: String, val telefone: String = "", val email: String = "")

/** Um passo do processo de adoção. */
data class RegistroEtapa(val etapa: EtapaAdocao, val data: Instant, val observacao: String = "")

/** Processo de adoção de um animal (coleção "adocoes"). */
@Document(collection = "adocoes")
data class Adocao(
    @Id val id: String? = null,
    @Field("idAbrigo") @Indexed val abrigoId: String,
    @Field("idAnimal") @Indexed val animalId: String,
    val interessado: Interessado,
    val etapa: EtapaAdocao = EtapaAdocao.PESSOA_INTERESSADA,
    val historicoEtapas: List<RegistroEtapa> = emptyList(),
    val criadoEm: Instant = Instant.now(),
    val concluidoEm: Instant? = null,
    val cancelada: Boolean = false,
)

/** Anúncio de produto/serviço de parceiro (coleção "parcerias"). Vale para todos os abrigos. */
@Document(collection = "parcerias")
data class Parceria(
    @Id val id: String? = null,
    val tipo: TipoParceria,
    val titulo: String,
    val categoria: String = "",
    val descricao: String = "",
    val preco: Double? = null,
    val parceiro: String = "",
    val linkUrl: String = "",
    val imagemUrl: String? = null,
    val cupom: String? = null,
    val desconto: Int? = null,
    val destaque: Boolean = false,
    val regiao: String? = null,
    val ativo: Boolean = true,
    val validoAte: Instant? = null,
)

/** Conexão entre dois abrigos (coleção "conexoes"). O abrigo A pediu; o B aceita ou recusa. */
@Document(collection = "conexoes")
data class Conexao(
    @Id val id: String? = null,
    @Field("idAbrigoA") @Indexed val abrigoAId: String,
    @Field("idAbrigoB") @Indexed val abrigoBId: String,
    val status: StatusConexao = StatusConexao.PENDENTE,
    val criadoEm: Instant = Instant.now(),
)

/** Token de uso único (recuperar senha, validar e-mail). Só o hash é guardado; some sozinho depois de expirar (TTL). */
@Document(collection = "tokens_verificacao")
data class TokenVerificacao(
    @Id val id: String? = null,
    @Indexed val usuarioId: String,
    val tipo: TipoToken,
    @Indexed(unique = true) val tokenHash: String,
    @Indexed(expireAfter = "0s") val expiraEm: Instant,
    val usadoEm: Instant? = null,
)
