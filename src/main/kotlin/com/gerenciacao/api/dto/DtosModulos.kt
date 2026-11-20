package com.gerenciacao.api.dto

import com.gerenciacao.api.domain.*
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Resposta paginada. */
data class Pagina<T>(val itens: List<T>, val pagina: Int, val tamanho: Int, val total: Long)

// ---------------------------------------------------------------- Animais

data class AnimalDto(
    val id: String, val nome: String, val genero: Genero, val especie: Especie, val raca: String,
    val nascimento: Instant?, val idadeMeses: Int?, val dataEntrada: Instant, val castrado: Boolean, val porte: Porte,
    val status: StatusAnimal, val etapaAdocao: EtapaAdocao?, val ativo: Boolean, val microchip: String?,
    val pesoKg: Double?, val caracteristicas: String, val descricao: String, val fotoPrincipalId: String?,
)

data class VacinaDto(
    val id: String, val nome: String, val data: Instant, val lote: String, val veterinario: String,
    val dataProximaDose: Instant?, val dataValidade: Instant?, val fabricante: String?,
)

data class HistoricoDto(
    val id: String, val dataConsulta: Instant, val diagnostico: String, val tratamento: String?, val veterinario: String,
)

data class AnimalDetalheDto(val animal: AnimalDto, val vacinas: List<VacinaDto>, val historico: List<HistoricoDto>, val fotos: List<FotoDto>)

data class NovoAnimalRequest(
    @field:NotBlank(message = "Informe o nome.") @field:Size(max = 80, message = "Nome muito longo.") val nome: String,
    @field:NotNull(message = "Informe o gênero.") val genero: Genero?,
    @field:NotNull(message = "Informe a espécie.") val especie: Especie?,
    @field:Size(max = 60, message = "Raça muito longa.") val raca: String = "SRD",
    val nascimento: Instant? = null,
    val castrado: Boolean = false,
    val porte: Porte = Porte.MEDIO,
    val status: StatusAnimal = StatusAnimal.DISPONIVEL,
    @field:Pattern(regexp = "^\\d{15}$", message = "O microchip tem 15 números.") val microchip: String? = null,
    @field:DecimalMin(value = "0.0", message = "Peso inválido.") val pesoKg: Double? = null,
    @field:Size(max = 300, message = "Características muito longas.") val caracteristicas: String = "",
    @field:Size(max = 2000, message = "Descrição muito longa.") val descricao: String = "",
)

data class AtualizarAnimalRequest(
    @field:Size(min = 1, max = 80, message = "Nome inválido.") val nome: String? = null,
    val genero: Genero? = null, val especie: Especie? = null,
    @field:Size(max = 60, message = "Raça muito longa.") val raca: String? = null,
    val nascimento: Instant? = null, val castrado: Boolean? = null, val porte: Porte? = null,
    @field:Pattern(regexp = "^\\d{15}$", message = "O microchip tem 15 números.") val microchip: String? = null,
    @field:DecimalMin(value = "0.0", message = "Peso inválido.") val pesoKg: Double? = null,
    @field:Size(max = 300, message = "Características muito longas.") val caracteristicas: String? = null,
    @field:Size(max = 2000, message = "Descrição muito longa.") val descricao: String? = null,
)

data class MudarStatusRequest(@field:NotNull(message = "Informe o status.") val status: StatusAnimal?, val etapaAdocao: EtapaAdocao? = null)

data class NovaVacinaRequest(
    @field:NotBlank(message = "Informe o nome da vacina.") val nome: String,
    @field:NotNull(message = "Informe a data da aplicação.") val data: Instant?,
    val lote: String = "", val veterinario: String = "", val dataProximaDose: Instant? = null,
    val dataValidade: Instant? = null, val fabricante: String? = null,
)

data class NovoHistoricoRequest(
    @field:NotNull(message = "Informe a data da consulta.") val dataConsulta: Instant?,
    @field:NotBlank(message = "Informe o diagnóstico.") val diagnostico: String,
    val tratamento: String? = null,
    @field:NotBlank(message = "Informe o veterinário.") val veterinario: String,
)

/** Idade aproximada em meses a partir da data de nascimento. */
fun idadeEmMeses(nascimento: Instant?): Int? =
    nascimento?.let { ChronoUnit.MONTHS.between(it.atZone(ZoneOffset.UTC).toLocalDate(), LocalDate.now(ZoneOffset.UTC)).toInt().coerceAtLeast(0) }

fun Animal.paraDto(fotoPrincipalId: String? = null) = AnimalDto(
    id = requireNotNull(id), nome = nome, genero = genero, especie = especie, raca = raca, nascimento = nascimento,
    idadeMeses = idadeEmMeses(nascimento), dataEntrada = dataEntrada, castrado = castrado, porte = porte, status = status,
    etapaAdocao = etapaAdocao, ativo = ativo, microchip = microchip, pesoKg = pesoKg, caracteristicas = caracteristicas,
    descricao = descricao, fotoPrincipalId = fotoPrincipalId,
)

fun Vacina.paraDto() = VacinaDto(requireNotNull(id), nome, data, lote, veterinario, dataProximaDose, dataValidade, fabricante)
fun HistoricoMedico.paraDto() = HistoricoDto(requireNotNull(id), dataConsulta, diagnostico, tratamento, veterinario)

// ---------------------------------------------------------------- Finanças

data class DespesaDto(
    val id: String, val tipo: TipoDespesa, val medicoTipo: TipoMedico?, val nome: String, val descricao: String,
    val quantidade: Int, val custo: Double, val data: Instant, val animalId: String?,
)

data class DespesaRequest(
    @field:NotNull(message = "Informe o tipo.") val tipo: TipoDespesa?,
    val medicoTipo: TipoMedico? = null,
    @field:NotBlank(message = "Informe o nome.") @field:Size(max = 120, message = "Nome muito longo.") val nome: String,
    @field:Size(max = 500, message = "Descrição muito longa.") val descricao: String = "",
    @field:Min(value = 1, message = "A quantidade mínima é 1.") val quantidade: Int = 1,
    @field:DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.") val custo: Double,
    val data: Instant? = null,
    val animalId: String? = null,
)

data class DoacaoDto(
    val id: String, val tipo: TipoDoacao, val descricao: String, val valor: Double?, val quantidade: Int?,
    val animalId: String?, val doador: String, val data: Instant,
)

data class DoacaoRequest(
    @field:NotNull(message = "Informe o tipo.") val tipo: TipoDoacao?,
    @field:Size(max = 300, message = "Descrição muito longa.") val descricao: String = "",
    @field:DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.") val valor: Double? = null,
    @field:Min(value = 1, message = "A quantidade mínima é 1.") val quantidade: Int? = null,
    val animalId: String? = null,
    @field:Size(max = 120, message = "Nome do doador muito longo.") val doador: String = "",
    val data: Instant? = null,
)

fun Despesa.paraDto() = DespesaDto(requireNotNull(id), tipo, medicoTipo, nome, descricao, quantidade, custo, data, animalId)
fun Doacao.paraDto() = DoacaoDto(requireNotNull(id), tipo, descricao, valor, quantidade, animalId, doador, data)

data class ResumoFinanceiroDto(val mes: String, val doacoes: Double, val despesas: Double, val saldo: Double)

data class RelatorioDto(
    val periodo: String, val inicio: Instant, val fim: Instant, val totalDoacoes: Double, val totalDespesas: Double, val saldo: Double,
    val despesasPorTipo: Map<TipoDespesa, Double>, val quantidadeDespesas: Int, val quantidadeDoacoes: Int,
    val adocoesNoPeriodo: Int, val resgatesNoPeriodo: Int,
)

// ---------------------------------------------------------------- Estoque

data class EstoqueDto(
    val id: String, val item: String, val categoria: CategoriaEstoque, val quantidade: Int, val unidade: String,
    val estoqueMinimo: Int, val abaixoDoMinimo: Boolean, val atualizadoEm: Instant,
)

data class EstoqueRequest(
    @field:NotBlank(message = "Informe o item.") @field:Size(max = 120, message = "Nome muito longo.") val item: String,
    @field:NotNull(message = "Informe a categoria.") val categoria: CategoriaEstoque?,
    @field:Min(value = 0, message = "A quantidade não pode ser negativa.") val quantidade: Int,
    @field:Size(max = 30, message = "Unidade muito longa.") val unidade: String = "unidades",
    @field:Min(value = 0, message = "O mínimo não pode ser negativo.") val estoqueMinimo: Int = 0,
)

fun ItemEstoque.paraDto() = EstoqueDto(requireNotNull(id), item, categoria, quantidade, unidade, estoqueMinimo, quantidade < estoqueMinimo, atualizadoEm)

// ---------------------------------------------------------------- Calendário

data class EventoDto(
    val id: String, val titulo: String, val tipoEvento: TipoEvento, val descricao: String, val dataHoraInicio: Instant,
    val dataHoraFim: Instant, val status: StatusEvento, val recorrencia: Recorrencia, val animalId: String?,
    val responsavel: String, val responsavelId: String?, val cor: String?, val lembrete: String?,
)

data class EventoRequest(
    @field:NotBlank(message = "Informe o título.") @field:Size(max = 120, message = "Título muito longo.") val titulo: String,
    val tipoEvento: TipoEvento = TipoEvento.CUIDADO,
    @field:Size(max = 1000, message = "Descrição muito longa.") val descricao: String = "",
    @field:NotNull(message = "Informe o início.") val dataHoraInicio: Instant?,
    val dataHoraFim: Instant? = null,
    val status: StatusEvento = StatusEvento.AGENDADO,
    val recorrencia: Recorrencia = Recorrencia.NENHUMA,
    val animalId: String? = null,
    @field:Size(max = 120, message = "Responsável muito longo.") val responsavel: String = "",
    val responsavelId: String? = null,
    @field:Size(max = 20, message = "Cor inválida.") val cor: String? = null,
    @field:Size(max = 40, message = "Lembrete inválido.") val lembrete: String? = null,
)

fun Evento.paraDto() = EventoDto(requireNotNull(id), titulo, tipoEvento, descricao, dataHoraInicio, dataHoraFim, status, recorrencia, animalId, responsavel, responsavelId, cor, lembrete)

// ---------------------------------------------------------------- Adoções

data class InteressadoRequest(
    @field:NotBlank(message = "Informe o nome do interessado.") @field:Size(max = 120, message = "Nome muito longo.") val nome: String,
    @field:Size(max = 20, message = "Telefone muito longo.") val telefone: String = "",
    @field:Size(max = 120, message = "E-mail muito longo.") val email: String = "",
)

data class NovaAdocaoRequest(@field:NotBlank(message = "Informe o animal.") val animalId: String, @field:Valid val interessado: InteressadoRequest)
data class EtapaRequest(@field:NotNull(message = "Informe a etapa.") val etapa: EtapaAdocao?, @field:Size(max = 500, message = "Observação muito longa.") val observacao: String = "")

data class AdocaoDto(
    val id: String, val animalId: String, val animalNome: String, val interessado: Interessado, val etapa: EtapaAdocao,
    val historicoEtapas: List<RegistroEtapa>, val criadoEm: Instant, val concluidoEm: Instant?, val cancelada: Boolean,
)

// ---------------------------------------------------------------- Parcerias e rede

data class ParceriaDto(
    val id: String, val tipo: TipoParceria, val titulo: String, val categoria: String, val descricao: String, val preco: Double?,
    val parceiro: String, val linkUrl: String, val imagemUrl: String?, val cupom: String?, val desconto: Int?, val destaque: Boolean, val regiao: String?,
)

data class ParceriaRequest(
    @field:NotNull(message = "Informe o tipo.") val tipo: TipoParceria?,
    @field:NotBlank(message = "Informe o título.") @field:Size(max = 120, message = "Título muito longo.") val titulo: String,
    @field:Size(max = 60, message = "Categoria muito longa.") val categoria: String = "",
    @field:Size(max = 1000, message = "Descrição muito longa.") val descricao: String = "",
    @field:DecimalMin(value = "0.0", message = "Preço inválido.") val preco: Double? = null,
    @field:Size(max = 120, message = "Parceiro muito longo.") val parceiro: String = "",
    @field:Pattern(regexp = "^https://.{3,500}$", message = "O link deve começar com https://") val linkUrl: String,
    @field:Pattern(regexp = "^(https://.{3,500})?$", message = "A imagem deve começar com https://") val imagemUrl: String? = null,
    @field:Size(max = 40, message = "Cupom muito longo.") val cupom: String? = null,
    @field:Min(value = 0, message = "Desconto inválido.") val desconto: Int? = null,
    val destaque: Boolean = false,
    @field:Size(max = 60, message = "Região muito longa.") val regiao: String? = null,
    val ativo: Boolean = true,
    val validoAte: Instant? = null,
)

fun Parceria.paraDto() = ParceriaDto(requireNotNull(id), tipo, titulo, categoria, descricao, preco, parceiro, linkUrl, imagemUrl, cupom, desconto, destaque, regiao)

enum class SituacaoConexao { NENHUMA, PENDENTE_ENVIADA, PENDENTE_RECEBIDA, CONECTADA }

data class RedeAbrigoDto(
    val abrigoId: String, val nome: String, val endereco: String, val descricao: String, val fotoId: String?,
    val situacao: SituacaoConexao, val conexaoId: String?,
)

// ---------------------------------------------------------------- Painel (Início)

data class OcupacaoDto(val capacidade: Int, val animaisTotais: Int, val vagas: Int)
data class DashboardDto(
    val ocupacao: OcupacaoDto, val proximosEventos: List<EventoDto>, val animaisRecentes: List<AnimalDto>,
    val adocoesEmAndamento: Int, val financeiroMes: ResumoFinanceiroDto?,
)

// ---------------------------------------------------------------- Senha e e-mail

data class EsqueciSenhaRequest(@field:NotBlank(message = "Informe o e-mail.") @field:jakarta.validation.constraints.Email(message = "E-mail inválido.") val email: String)
data class RedefinirSenhaRequest(
    @field:NotBlank(message = "Informe o código.") val token: String,
    @field:NotBlank(message = "Informe a senha.") @field:Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
    @field:Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "A senha deve ter letras e números.") val novaSenha: String,
)
data class AlterarSenhaRequest(
    @field:NotBlank(message = "Informe a senha atual.") val senhaAtual: String,
    @field:NotBlank(message = "Informe a nova senha.") @field:Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
    @field:Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "A senha deve ter letras e números.") val novaSenha: String,
)
data class TokenRequest(@field:NotBlank(message = "Informe o código.") val token: String)
data class MensagemDto(val mensagem: String)
