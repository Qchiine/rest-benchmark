# Etude de cas - Benchmark de performances des Web Services REST

Comparaison de trois implementations REST exposees sur le meme domaine metier
(Category / Item) et sur les memes endpoints.

| Variante | Stack | Type de controleurs | Port |
|---|---|---|---|
| A | Jersey (JAX-RS) + Hibernate | controleurs manuels | 8081 |
| C | Spring Boot + @RestController | controleurs manuels | 8082 |
| D | Spring Boot + Spring Data REST | exposition automatique | 8083 |

## Contenu

```
variant-a-jersey/            variante A (war Jetty, Hibernate natif)
variant-c-restcontroller/    variante C (Spring Boot, @RestController)
variant-d-springdatarest/    variante D (Spring Data REST)
data-generator/              generateur SQL (2000 categories, 100000 items) et CSV JMeter
db/                          schema de reference et script de seed genere
jmeter/                      4 plans .jmx, generateur declaratif, CSV et resultats
monitoring/                  Prometheus + InfluxDB v2 + Grafana (docker compose)
docs/                        tableaux T0 a T7 et guide d'execution
```

## Resultats mesures

Palier reduit, 5 threads, 15 s, PostgreSQL 16.4 local, 100 000 items :

| Variante | RPS | p50 | p95 | p99 | Err |
|---|---|---|---|---|---|
| A | 310,3 | 5 ms | 56 ms | 173 ms | 0,00 % |
| C | 215,0 | 10 ms | 67 ms | 152 ms | 0,00 % |
| D | 53,8 | 48 ms | 318 ms | 465 ms | 0,00 % |

Ces chiffres proviennent des fichiers .jtl produits par JMeter. Ils ne remplacent
pas les paliers de 50 a 200 threads du protocole : voir docs/resultats-T0-T7.md
pour les conditions exactes et les cases laissees a "non mesure".

## Ecarts constates entre les variantes

- A et C renvoient un corps JSON strictement identique pour une meme requete.
- D renvoie du HAL : 860 octets contre 522 pour la meme ressource, ce qui explique
  une partie de son debit plus faible.
- Les trois variantes renvoient les memes codes HTTP sur les sept operations du
  protocole, y compris 409 sur conflit d'unicite.

Pour lancer la base, les variantes, le monitoring et les scenarios, voir
docs/execution.md.
