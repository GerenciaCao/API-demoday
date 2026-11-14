package com.gerenciacao.api.dto

import com.gerenciacao.api.domain.Abrigo
import com.gerenciacao.api.domain.Cargo
import com.gerenciacao.api.domain.Permissao
import com.gerenciacao.api.domain.StatusUsuario
import com.gerenciacao.api.domain.Usuario
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

// ---------------------------------------------------------------- Erros

/** Formato padrão de erro devolvido ao app. */
data class ErroResposta(val erro: String, val campos: Map<String, String>? = null)

// ---------------------------------------------------------------- Autenticação

/** Corpo do cadastro (tela "Cadastre-se"): nome, e-mail, telefone e senha. */
data class CadastroRequest(
    @field:NotBlank(message = "Informe o seu nome.")
    @field:Size(max = 120, message = "Nome muito longo.")
    val nome: String,
    @field:NotBlank(message = "Informe o e-mail.")
    @field:Email(message = "E-mail inválido.")
    val email: String,
    @field:Size(max = 20, message = "Telefone muito longo.")
    val telefone: String = "",
    @field:NotBlank(message = "Informe a senha.")
    @field:Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
    @field:Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "A senha deve ter letras e números.")
    val senha: String,
)

/** Corpo do login (tela "Login"). */
data class LoginRequest(
    @field:NotBlank(message = "Informe o e-mail.") val email: String,
    @field:NotBlank(message = "Informe a senha.") val senha: String,
)

/** Corpo para concluir um convite: o token recebido + a senha escolhida. */
data class AceitarConviteRequest(
    @field:NotBlank(message = "Informe o código do convite.") val token: String,
    @field:NotBlank(message = "Informe a senha.")
    @field:Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
    @field:Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "A senha deve ter letras e números.")
    val senha: String,
)

/** Resposta de login/cadastro: o token JWT (enviar em "Authorization: Bearer ...") e os dados do usuário. */
data class AuthResponse(
    val token: String,
    val tipo: String = "Bearer",
    val expiraEm: Instant,
    val usuario: UsuarioDto,
)

/** Dados do usuário que o app pode ver (sem senha, sem token de convite). */
data class UsuarioDto(
    val id: String,
    val abrigoId: String,
    val nome: String,
    val sobrenome: String,
    val email: String,
    val telefone: String,
    val endereco: String,
    val fotoUrl: String?,
    val cargo: Cargo,
    val permissoes: Set<Permissao>,
    val status: StatusUsuario,
    val passouPeloOnboard: Boolean,
)

// ---------------------------------------------------------------- Equipe

/** Corpo de "Adicionar Membro". Se "permissoes" faltar, usa as permissões padrão do cargo. */
data class NovoMembroRequest(
    @field:NotBlank(message = "Informe o nome.") @field:Size(max = 80, message = "Nome muito longo.") val nome: String,
    @field:Size(max = 80, message = "Sobrenome muito longo.") val sobrenome: String = "",
    @field:NotBlank(message = "Informe o e-mail.") @field:Email(message = "E-mail inválido.") val email: String,
    @field:Size(max = 20, message = "Telefone muito longo.") val telefone: String = "",
    @field:Size(max = 200, message = "Endereço muito longo.") val endereco: String = "",
    @field:NotNull(message = "Escolha um cargo.") val cargo: Cargo?,
    val permissoes: Set<Permissao>? = null,
)

/**
 * Resposta de "Adicionar Membro". O "conviteToken" aparece só aqui, uma vez.
 * (Quando o envio de e-mail existir, ele irá por e-mail e sairá desta resposta.)
 */
data class ConviteResponse(val membro: UsuarioDto, val conviteToken: String, val conviteExpiraEm: Instant)

/** Corpo de "Editar membro". Só os campos enviados são alterados. */
data class AtualizarMembroRequest(
    @field:Size(max = 80, message = "Nome muito longo.") val nome: String? = null,
    @field:Size(max = 80, message = "Sobrenome muito longo.") val sobrenome: String? = null,
    @field:Email(message = "E-mail inválido.") val email: String? = null,
    @field:Size(max = 20, message = "Telefone muito longo.") val telefone: String? = null,
    @field:Size(max = 200, message = "Endereço muito longo.") val endereco: String? = null,
    val cargo: Cargo? = null,
    val permissoes: Set<Permissao>? = null,
)

// ---------------------------------------------------------------- Abrigo

/** Dados do abrigo mostrados na tela "Informações do abrigo". */
data class AbrigoDto(
    val id: String,
    val nome: String,
    val descricao: String,
    val telefone: String,
    val email: String,
    val endereco: String,
    val latitude: Double?,
    val longitude: Double?,
    val redesSociais: String,
    val capacidade: Int,
    val fotoUrl: String?,
    /** Animais ativos (ocupam vaga). */
    val animaisTotais: Int = 0,
)

/** Foto guardada no banco; [url] é a rota para baixar a imagem (exige login). */
data class FotoDto(
    val id: String,
    val donoTipo: com.gerenciacao.api.domain.DonoFoto,
    val donoId: String,
    val principal: Boolean,
    val contentType: String,
    val tamanhoBytes: Int,
    val url: String,
)

/** Corpo para editar o abrigo. Só os campos enviados são alterados. */
data class AtualizarAbrigoRequest(
    @field:Size(min = 1, max = 120, message = "O nome deve ter de 1 a 120 caracteres.") val nome: String? = null,
    @field:Size(max = 1000, message = "Descrição muito longa.") val descricao: String? = null,
    @field:Size(max = 20, message = "Telefone muito longo.") val telefone: String? = null,
    @field:Email(message = "E-mail inválido.") val email: String? = null,
    @field:Size(max = 200, message = "Endereço muito longo.") val endereco: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @field:Size(max = 300, message = "Texto muito longo.") val redesSociais: String? = null,
    @field:Min(value = 0, message = "A capacidade não pode ser negativa.") val capacidade: Int? = null,
)

// ---------------------------------------------------------------- Conversões entidade -> DTO

/** Converte o documento do MongoDB no DTO enviado ao app (esconde senha e token de convite). */
fun Usuario.paraDto() = UsuarioDto(
    id = requireNotNull(id) { "Usuário sem id" },
    abrigoId = abrigoId,
    nome = nome,
    sobrenome = sobrenome,
    email = email,
    telefone = telefone,
    endereco = endereco,
    fotoUrl = fotoUrl,
    cargo = cargo,
    permissoes = permissoes,
    status = status,
    passouPeloOnboard = passouPeloOnboard,
)

/** Converte o abrigo no DTO enviado ao app. */
fun Abrigo.paraDto(animaisTotais: Int = 0) = AbrigoDto(
    id = requireNotNull(id) { "Abrigo sem id" },
    nome = nome,
    descricao = descricao,
    telefone = telefone,
    email = email,
    endereco = endereco,
    latitude = latitude,
    longitude = longitude,
    redesSociais = redesSociais,
    capacidade = capacidade,
    fotoUrl = fotoUrl,
    animaisTotais = animaisTotais,
)
