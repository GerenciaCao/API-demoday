package com.gerenciacao.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/** Classe principal da API do GerenciaCão. O Spring procura os componentes neste pacote e nos filhos. */
@SpringBootApplication
@ConfigurationPropertiesScan // ativa as classes @ConfigurationProperties (ex.: JwtProperties)
class GerenciaCaoApiApplication

/** Ponto de entrada: sobe o servidor web embutido (porta 8080 por padrão). */
fun main(args: Array<String>) {
    runApplication<GerenciaCaoApiApplication>(*args)
}
