# DeckDealer Marketplace

## Integrantes
- **Samuel Barbosa Silveira** — Engenharia da Computação (Manhã)
- **Felipe Paravidino Silveira** — Engenharia da Computação (Manhã)
- **Tipo**: Dupla

## Divisão de Responsabilidades

| Aluno | Microserviço | Banco | Papéis Adicionais |
|---|---|---|---|
| Samuel Barbosa Silveira | `card-service` | MongoDB | Discovery Server, Documentação |
| Felipe Paravidino Silveira | `trade-service` | PostgreSQL | API Gateway, Resiliência, Testes |

## Descrição do Projeto

O **DeckDealer** é um marketplace de cartas colecionáveis (Magic: The Gathering, Yu-Gi-Oh!, Pokémon). O sistema resolve o problema de fragmentação no mercado, oferecendo uma plataforma centralizada onde:
- Vendedores podem catalogar cartas e criar anúncios de venda
- Compradores podem buscar cartas e realizar compras

## Arquitetura

O projeto usa uma arquitetura de microserviços com Spring Boot e Spring Cloud.

### Microservices

| Serviço | Responsabilidade | Porta | Banco de Dados |
|---|---|---|---|
| `discovery-server` | Registro e descoberta de serviços (Eureka) | `8761` | — |
| `api-gateway` | Ponto único de entrada, roteamento | `8080` | — |
| `card-service` | Catálogo geral de cartas | `8081` | MongoDB |
| `trade-service` | Anúncios de venda e compras | `8082` | PostgreSQL |

### Tecnologias

- **Java 21**
- **Spring Boot 3.3.4**
- **Spring Cloud 2023.0.3** (Eureka, Gateway)
- **Spring WebFlux** (card-service — modelo reativo)
- **Spring MVC** (trade-service)
- **MongoDB** (card_db)
- **PostgreSQL** (trade_db)
- **Apache Kafka** (comunicação assíncrona)
- **Resilience4j** (Circuit Breaker)
- **Docker Compose**

## Como Executar

### 1. Subir a infraestrutura (bancos e Kafka)
```bash
docker-compose up -d
```

### 2. Compilar todos os projetos
```bash
mvn clean package -DskipTests
```

### 3. Executar os microserviços (em ordem)
Abra 4 terminais e execute um em cada:

**Terminal 1 (Discovery Server):**
```bash
mvn -pl discovery-server spring-boot:run
```

**Terminal 2 (API Gateway):**
```bash
mvn -pl api-gateway spring-boot:run
```

**Terminal 3 (Card Service):**
```bash
mvn -pl card-service spring-boot:run
```

**Terminal 4 (Trade Service):**
```bash
mvn -pl trade-service spring-boot:run
```

## Discovery Server

Acesse o painel do Eureka em: `http://localhost:8761`

Você deverá ver registrados: `API-GATEWAY`, `CARD-SERVICE` e `TRADE-SERVICE`.

## API Gateway

O API Gateway centraliza o acesso na porta `8080`.

| Rota | Destino |
|---|---|
| `/api/cards/**` | card-service |
| `/api/trades/**` | trade-service |

## Exemplos de Requisições

Todas as requisições passam pelo API Gateway (porta `8080`).

### 1. Cadastrar uma carta
```bash
curl -X POST http://localhost:8080/api/cards \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Black Lotus",
    "game": "Magic: The Gathering",
    "expansion": "Alpha",
    "rarity": "Mythic Rare",
    "averagePrice": 25000.00,
    "attributes": {
      "manaCost": "0",
      "type": "Artifact"
    }
  }'
```

### 2. Criar um anúncio de venda
Substitua `COLOQUE_O_ID_AQUI` pelo ID retornado acima.
```bash
curl -X POST http://localhost:8080/api/trades/listings \
  -H "Content-Type: application/json" \
  -d '{
    "cardId": "COLOQUE_O_ID_AQUI",
    "sellerName": "Loja do Samuel",
    "price": 24000.00,
    "cardCondition": "Near Mint"
  }'
```

### 3. Ver detalhes do anúncio (comunicação síncrona entre serviços)
```bash
curl -X GET http://localhost:8080/api/trades/listings/1
```

### 4. Comprar uma carta (dispara evento Kafka)
```bash
curl -X POST http://localhost:8080/api/trades/listings/1/buy
```

### 5. Verificar métricas
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/prometheus
```
