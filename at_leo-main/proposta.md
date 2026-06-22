# Proposta do Trabalho Prático - Entregas 1 e 2

## 1. Identificação da Equipe e Turma

* **Nome do Projeto**: DeckDealer Marketplace
* **Turma**: Manhã (Engenharia da Computação)
* **Tipo de Entrega**: Dupla

### Divisão de Responsabilidades da Dupla

| Aluno | Microserviço sob Responsabilidade | Banco Utilizado | Papéis Adicionais |
|---|---|---|---|
| **Samuel Barbosa Silveira** | `card-service` | MongoDB Reativo | Programação Reativa (WebFlux), Consumidor Kafka, Documentação, Actuator e Logs de Correlação |
| **Felipe Paravidino Silveira** | `trade-service` | PostgreSQL | API Gateway, Resiliência (Circuit Breaker), Produtor Kafka, Actuator e RestTemplate Interceptor |

---

## 2. Tema Escolhido e Descrição do Problema

### Tema: Marketplace de Collectible Card Games (CCG)
O projeto consiste em um marketplace especializado na compra e venda de cartas colecionáveis (como *Magic: The Gathering*, *Pokémon TCG* e *Yu-Gi-Oh!*).

### O Problema que o Sistema Resolve
Colecionadores enfrentam dificuldades para encontrar cartas de forma rápida e segura. O **DeckDealer** centraliza isso: lojas catalogam cartas e usuários buscam/compram cartas com preços justos através de um catálogo unificado.

### Por que Microserviços?
1. **Catálogo vs. Venda**: O catálogo é estático e de leitura intensiva. Listagens de venda são dinâmicas e transacionais. Separar permite escalar a leitura (catálogo) independentemente das vendas.
2. **Persistência Poliglota**: O catálogo precisa de flexibilidade (MongoDB) para atributos variados de cartas. O sistema de compras requer consistência ACID e transações (PostgreSQL).

---

## 3. Arquitetura da Solução

O sistema possui 4 microserviços integrados com Spring Cloud, comunicação síncrona e assíncrona, além de observabilidade completa.

```mermaid
graph TD
    Client[Cliente / Postman] -->|1. Chamadas HTTP| Gateway[API Gateway - Porta 8080]
    
    subgraph Spring Cloud Infrastructure
        Eureka[Discovery Server Eureka - Porta 8761]
    end
    
    Gateway -->|2. Roteia com X-Correlation-ID| CardService[card-service WebFlux - Porta 8081]
    Gateway -->|2. Roteia com X-Correlation-ID| TradeService[trade-service MVC - Porta 8082]
    
    CardService -.->|Registro| Eureka
    TradeService -.->|Registro| Eureka
    
    TradeService -->|3a. Síncrono (Circuit Breaker)| CardService
    TradeService -->|3b. Assíncrono (Kafka Topic)| Kafka[[Kafka: trades.purchases]]
    Kafka -.->|4. Consome e atualiza avgPrice| CardService
    
    subgraph Databases
        MongoReactive[(MongoDB Reactive: card_db)]
        Postgres[(PostgreSQL: trade_db)]
    end
    
    CardService --> MongoReactive
    TradeService --> Postgres
```

### Detalhamento dos Componentes

1. **Discovery Server (Porta 8761)**: Servidor Eureka para registro automático e descoberta dos serviços.
2. **API Gateway (Porta 8080)**: Roteador central (Spring Cloud Gateway). Injeta o `X-Correlation-ID` em todas as requisições para rastreamento.
3. **Card Service (Porta 8081)**: WebFlux reativo + MongoDB. Lida com o catálogo sem bloqueio de threads e consome os eventos do Kafka de forma assíncrona para atualizar os preços das cartas.
4. **Trade Service (Porta 8082)**: Spring MVC + PostgreSQL. Lida com vendas, aciona o Circuit Breaker (Resilience4j) ao consultar dados de cartas e produz eventos no Kafka quando uma compra é finalizada.

---

## 4. Comunicação e Resiliência

### 4.1. Comunicação Síncrona e Resiliência
Para exibir os detalhes completos de um anúncio, o `trade-service` precisa consultar o `card-service` via REST (`RestTemplate`). 
Utilizamos o **Resilience4j Circuit Breaker**: se o `card-service` ficar indisponível, o Circuit Breaker abre e ativa o Fallback, que retorna dados fictícios genéricos (ex: "Informações Indisponíveis"), permitindo que o usuário veja a página de vendas sem o sistema inteiro cair.

### 4.2. Comunicação Assíncrona (Kafka)
Ao comprar uma carta (`POST /api/trades/listings/{id}/buy`), o `trade-service` efetiva a transação no PostgreSQL e imediatamente responde ao usuário. Em paralelo, ele envia o evento `CompraRealizadaEvent` para o **Kafka**.
O `card-service` consome esse evento em segundo plano e atualiza o preço médio (averagePrice) no MongoDB.
Isso diminui o acoplamento: o usuário não sofre com lentidão, e se o `card-service` cair, a mensagem fica guardada no Kafka até ele voltar.

---

## 5. Observabilidade

1. **Correlation ID**: Implementado via Filtros (MVC e WebFlux). Um ID único (`X-Correlation-ID`) é gerado no Gateway e injetado no MDC de logs de todos os serviços. O ID trafega no cabeçalho HTTP e dentro do *payload* do evento do Kafka.
2. **Logs Estruturados**: O padrão do Spring Boot foi alterado em `application.yml` para imprimir sempre o `[correlationId=...]` em cada linha de log, permitindo rastrear o clique do usuário do início ao fim (Gateway -> Trade -> Kafka -> Card).
3. **Métricas (Actuator/Prometheus)**: Adicionado nos serviços de negócio para expor dados de tempo de resposta, quantidade de conexões abertas, falhas de circuit breaker, etc. Acessível em `http://localhost:808X/actuator/prometheus`.

---

## 6. Evidências de Funcionamento (Logs Rastreáveis)

Exemplo de como a Observabilidade e a Assincronicidade se comportam nos logs (mesmo fluxo ponta a ponta):

**Gateway (Porta 8080):**
`INFO [api-gateway] - Request: POST /api/trades/listings/1/buy. Gerado Correlation ID: ea5616f7`

**Trade Service (Porta 8082):**
`INFO [trade-service,correlationId=ea5616f7] - Recebida requisição de compra para o anúncio ID: 1`
`INFO [trade-service,correlationId=ea5616f7] - Publicando evento no Kafka: CompraRealizadaEvent`

**Card Service (Porta 8081):**
`INFO [card-service,correlationId=ea5616f7] - Evento CompraRealizada recebido do Kafka.`
`INFO [card-service,correlationId=ea5616f7] - Atualizando preço médio de 'Black Lotus'. Preço antigo: 100.0, Venda: 150.0. Novo: 125.0`