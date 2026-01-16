# Java 21 – Balanceamento de Carga e Threads Virtuais

Sistema distribuído desenvolvido em Java 21 com Spring Boot, projetado para alta concorrência e escalabilidade horizontal. Implementa threads virtuais para otimização de recursos, cache distribuído e processamento assíncrono, avaliando diferentes estratégias de balanceamento de carga em ambiente conteinerizado.

## Arquitetura do Sistema

![Arquitetura do Sistema](./assets/architecture.png)

## Padrões de Projeto e Tecnologias

- **Strategy Pattern**: Implementado para alternância dinâmica entre algoritmos de balanceamento de carga (Round Robin, Least Connections, IP Hash)
- **Facade Pattern**: Abstração da complexidade de integração entre PostgreSQL, Redis e Redis Streams
- **Cache-Aside Pattern**: Otimização de desempenho com Redis para leituras frequentes
- **Threads Virtuais (Project Loom)**: Concorrência massiva com overhead reduzido
- **Spring Boot 3.5.4**: Framework principal com suporte a Java 21
- **PostgreSQL 15**: Persistência relacional com otimizações de escrita em lote
- **Redis 7**: Cache distribuído e processamento assíncrono via Streams
- **Docker & Docker Compose**: Conteinerização e orquestração
- **NGINX**: Balanceamento de carga com múltiplos algoritmos
- **Grafana K6**: Testes de carga e análise de desempenho

## Resultados de Desempenho por Algoritmo de Balanceamento

| Algoritmo de Balanceamento | Throughput (req/s) | Latência p95 (ms) | Latência Média (ms) | Instâncias da API |
| -------------------------- | ------------------ | ----------------- | ------------------- | ----------------- |
| Least Connections          | 1207.8             | 324.5             | 83.6                | 3                 |
| Least Connections          | 887.68             | 695.8             | 221.98              | 2                 |
| Round Robin                | 1060.64            | 875.6             | 136.9               | 3                 |
| Round Robin                | 921.64             | 997.56            | 203.2               | 2                 |
| IP Hash                    | 787.5              | 753.25            | 289.0               | 3                 |
| IP Hash                    | 725.0              | 913.5             | 340.0               | 2                 |