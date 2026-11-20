# API do GerenciaCão (Spring Boot + Kotlin + MongoDB Atlas)

> ## ⚠️ Este é um projeto SEPARADO do app Android
> Guarde esta pasta **fora** do projeto do app (ex.: `C:\Projetos\gerenciacao-api`) e abra-a **sozinha** numa janela nova da IDE
> (File → Open → escolha a pasta → "New Window"). **Nunca** copie estes arquivos para dentro do projeto Android:
> o `build.gradle.kts` e o `settings.gradle.kts` daqui substituiriam os do app e quebrariam o build dele.

## Preparar o Gradle (uma vez só)
O Spring Boot 4.1 precisa do Gradle 8.14 ou superior. A pasta já traz `gradle/wrapper/gradle-wrapper.properties` apontando para o Gradle 9.6.0 (o mesmo do app no Android Studio Rabbit 2, então o download é reaproveitado).
Faltam os arquivos do wrapper que são binários. Copie-os do seu projeto Android (eles servem para qualquer versão):
- `gradlew` e `gradlew.bat` (raiz do projeto Android) → raiz desta pasta
- `gradle/wrapper/gradle-wrapper.jar` (do projeto Android) → `gradle/wrapper/` desta pasta
(Não copie o `gradle-wrapper.properties` do app: o desta pasta é que vale.)
Na IDE, em Settings → Build → Gradle, deixe **Gradle JDK** em 17 ou 21 e **Distribution** em "Wrapper".

## O que você precisa
- **JDK 17 ou superior** (recomendado 21)
- **IntelliJ IDEA** (a Community serve) ou outra IDE com suporte a Gradle
- Gradle 9.6.0 (vem do wrapper; veja "Preparar o Gradle" acima)
- Uma conta gratuita no **MongoDB Atlas**

## 1. Banco de dados no MongoDB Atlas
1. Em https://www.mongodb.com/atlas crie um cluster gratuito (M0).
2. **Database Access** → crie um usuário e senha para a API.
3. **Network Access** → libere o seu IP (para testes, "Allow access from anywhere" `0.0.0.0/0`; troque por IPs fixos antes de publicar).
4. **Connect → Drivers** → copie a string de conexão e **acrescente o nome do banco** depois de `.mongodb.net/`:
   `mongodb+srv://USUARIO:SENHA@SEU-CLUSTER.mongodb.net/GerenciaCaoDB?retryWrites=true&w=majority`
   (o banco do projeto se chama **GerenciaCaoDB**, com G, C e D maiúsculos)
5. Se a senha tiver caracteres especiais (`@`, `:`, `/`), troque por códigos de URL (ex.: `@` vira `%40`).

## 2. Variáveis de ambiente (os segredos NÃO ficam no código)
| Variável | Valor |
|---|---|
| `MONGODB_URI` | a string de conexão do passo anterior |
| `JWT_SEGREDO` | texto aleatório com **32 caracteres ou mais** (assina os tokens de login) |

No IntelliJ: **Run → Edit Configurations → Environment variables** (`MONGODB_URI=...;JWT_SEGREDO=...`).
No Windows PowerShell (na mesma janela em que rodar a API): `$env:MONGODB_URI="..."` e `$env:JWT_SEGREDO="..."`. No Linux/macOS: `export MONGODB_URI=...`.

## 3. Rodar
- IntelliJ: abra a pasta `gerenciacao-api`, espere o Gradle sincronizar e rode `GerenciaCaoApiApplicationKt`.
- Terminal (Windows, na pasta da API): `.\gradlew.bat bootRun`  (Linux/macOS: `./gradlew bootRun`).
- Pela IDE (Gradle): Run → Edit Configurations → **+ → Gradle** → Run: `bootRun`, Gradle project: esta pasta, e preencha **Environment variables**.
- Teste: abra `http://localhost:8080/api/saude` → `{"status":"ok"}`.
- Testes automáticos (sem banco): `./gradlew test`.
- O arquivo `requests.http` tem um roteiro completo de chamadas.

Do **emulador Android**, a API do seu computador é `http://10.0.2.2:8080`. Em um celular real, use o IP do computador na rede (ex.: `http://192.168.0.10:8080`).

## Rotas
Tudo, exceto as rotas marcadas como públicas, exige o cabeçalho `Authorization: Bearer <token>`.

| Método e rota | Quem | O que faz |
|---|---|---|
| `GET /api/saude` | público | confere se a API está no ar |
| `POST /api/auth/cadastro` | público | cria a conta + o abrigo (vira Administrador) e já devolve o token |
| `POST /api/auth/login` | público | entra com e-mail e senha |
| `POST /api/auth/aceitar-convite` | público | o convidado define a senha com o token do convite |
| `GET /api/auth/eu` | logado | dados do usuário logado |
| `GET /api/equipe` · `GET /api/equipe/{id}` | logado | lista / detalha membros |
| `POST /api/equipe` | `MODIFICAR_EQUIPE` | convida um membro (devolve o `conviteToken`) |
| `PUT /api/equipe/{id}` | próprio perfil, ou `MODIFICAR_EQUIPE` | edita dados, cargo e permissões |
| `DELETE /api/equipe/{id}` | própria conta, ou `MODIFICAR_EQUIPE` | remove o membro |
| `GET /api/abrigo` | logado | informações do abrigo (inclui `animaisTotais`) |
| `PUT /api/abrigo` | Administrador ou Gestor | edita o abrigo |
| `POST /api/fotos` | conforme o dono | envia uma foto (multipart: `donoTipo`, `donoId`, `principal`, `arquivo`) |
| `GET /api/fotos?donoTipo=&donoId=` | conforme o dono | lista as fotos (sem os bytes) |
| `GET /api/fotos/{id}` · `DELETE /api/fotos/{id}` | conforme o dono | baixa / apaga a foto |

Erros vêm sempre como `{"erro": "mensagem em português", "campos": {...}}` com o status HTTP certo (400, 401, 403, 404, 409).

## Rotas dos demais módulos (todas exigem login, exceto as públicas de senha)
| Módulo | Rotas | Permissão |
|---|---|---|
| Animais | `GET/POST /api/animais` (filtros `especie`, `status`, `busca`, `pagina`, `tamanho`) · `GET/PUT/DELETE /api/animais/{id}` · `PUT /api/animais/{id}/status` | ver `VER_FICHAS`; criar/editar `EDITAR_PRONTUARIOS` |
| Vacinas e histórico | `POST /api/animais/{id}/vacinas` · `DELETE …/vacinas/{vid}` · `POST /api/animais/{id}/historico` · `DELETE …/historico/{hid}` | `EDITAR_PRONTUARIOS` (a próxima dose cria um lembrete no calendário) |
| Despesas | `GET/POST /api/despesas` · `PUT/DELETE /api/despesas/{id}` | `GERENCIAR_DADOS` |
| Doações | `GET/POST /api/doacoes` · `PUT/DELETE /api/doacoes/{id}` | `GERENCIAR_DADOS` |
| Finanças | `GET /api/financeiro/resumo?mes=2026-11` · `GET /api/financeiro/relatorio?periodo=SEMANAL\|MENSAL&referencia=2026-11-14` | `GERENCIAR_DADOS` |
| Estoque | `GET/POST /api/estoque` · `PUT/DELETE /api/estoque/{id}` | `GERENCIAR_ESTOQUE` |
| Calendário | `GET /api/eventos?de=&ate=` · `POST` · `PUT/DELETE /api/eventos/{id}` | ver: todos; mexer: `GERENCIAR_AGENDA` |
| Adoções | `GET/POST /api/adocoes` · `PUT /api/adocoes/{id}/etapa` · `POST …/concluir` · `POST …/cancelar` | ver `VER_FICHAS`; mexer `EDITAR_PRONTUARIOS` |
| Parcerias | `GET /api/parcerias?tipo=&categoria=&busca=&destaque=` · `POST/PUT/DELETE` | ver: todos; mexer: administradores da plataforma |
| Rede de abrigos | `GET /api/rede?busca=&somenteConexoes=` · `POST /api/rede/{abrigoId}/conectar` · `POST /api/rede/conexoes/{id}/aceitar` · `DELETE /api/rede/conexoes/{id}` | pedir/aceitar: administrador ou gestor |
| Painel (Início) | `GET /api/dashboard` | todos (finanças só com `GERENCIAR_DADOS`) |
| Senha e e-mail | `POST /api/auth/esqueci-senha` · `redefinir-senha` · `validar-email` (públicas) · `POST /api/auth/alterar-senha` · `POST /api/auth/validar-email/solicitar` | — |

**Administradores da plataforma** (criam os anúncios de Parcerias): variável de ambiente `PLATAFORMA_ADMINS` com e-mails separados por vírgula.
**E-mails:** por enquanto o código vai para o **log do servidor** (`EmailSenderLog`, só desenvolvimento). Antes de publicar, troque por um provedor real implementando `EmailSender`.
**Troca de senha:** os tokens de login emitidos antes da troca deixam de valer.

## Decisões desta etapa
- **Fotos no MongoDB** (coleção `fotos`): até 2 MB cada (JPEG, PNG ou WebP, conferidos pelos bytes), 10 por animal, 1 por usuário/abrigo. Animal: ver = `VER_FICHAS`, enviar/apagar = `EDITAR_PRONTUARIOS`; usuário: a própria foto ou `MODIFICAR_EQUIPE`; abrigo: administrador ou gestor.
- `lotacao` do banco = **capacidade máxima** do abrigo.
- **Spring Boot 4.1 + Kotlin 2.4**: o Spring Boot 3.5 saiu de suporte em 30/06/2026.
- **Cada conta nova cria o seu abrigo** e vira Administrador (como no Dossiê: "cada usuário registrado tem acesso à sua própria instância").
- **Senha**: só o hash BCrypt é guardado. O login responde igual para "e-mail inexistente" e "senha errada".
- **Token JWT** (12 h) guarda só o id; cargo e permissões são lidos do banco a cada chamada, então mudanças e remoções valem na hora.
- **Convite (RF06)**: o membro é criado como `CONVIDADO`, sem senha. O token aparece **uma vez** na resposta de `POST /api/equipe`; o banco guarda só o hash. Quando houver e-mail, o token passa a ir por e-mail.
- **Regras da equipe**: só Administrador cria/altera/remove Administrador; o abrigo nunca fica sem um; ninguém concede permissão que não tem.

## Banco de dados real (GerenciaCaoDB)
Antes de rodar a API, **corrija o banco** com os arquivos da pasta `banco_corrigido` (veja `COMO_APLICAR.md` lá). A API espera:
- cargo e status gravados como **ADMINISTRADOR**, **ATIVO** etc. (nome do enum), e datas como tipo *Date*;
- no `usuarios`: campos `idAbrigo` e `senha` (hash BCrypt);
- no `abrigos`: `localizacao`, `contatoRedes` e `lotacao` (= capacidade máxima).
O mapeamento entre os nomes do banco e os nomes do código está nas anotações `@Field` de `Usuario.kt` e `Abrigo.kt`.

Logins de teste (SÓ em desenvolvimento — antes de publicar, apague-os com `limpar_dados_teste.js`), senha **Senha@1234**: `maria@patasconscientes.org` (Administradora do abrigo_001), `carlos@patasconscientes.org` (Veterinário), `fernanda@anjosdequatropatas.org` (Gestora do abrigo_002), `anapaula@amigofiel.org` (Administradora do abrigo_004). O Lucas (`lucas@segurabichos.org`) está como convidado: use o token `convite-lucas-dev` em `POST /api/auth/aceitar-convite`.

## Segurança já aplicada
Hash BCrypt · JWT HS256 com segredo vindo de variável de ambiente · permissões conferidas no banco a cada chamada · isolamento por abrigo (sem acesso a dados de outro abrigo) · login igual para e-mail inexistente e senha errada · **limite de tentativas** (5 erros/15 min por e-mail+IP; 10 cadastros/hora por IP) · senha com letras e números · erros sem stack trace · sem CORS aberto · sem consultas montadas com texto (sem injeção NoSQL).

## Ainda não feito (próximas etapas)
Envio real de e-mails (provedor ainda não escolhido) · login com Google/Microsoft (precisa validar o token no servidor) · posts para redes sociais (RF30 a RF32) · notificações push dos lembretes · testes automáticos dos serviços · publicação (HTTPS, backup do banco).
