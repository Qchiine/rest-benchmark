# T0 a T7 - Tableaux de collecte du benchmark REST

Ces tableaux reprennent exactement la structure demandee par l'enonce.
Les valeurs sont a remplir apres execution des scenarios JMeter.

Regles de remplissage :

- Renseigner une ligne par palier de threads lorsque le protocole en prevoit plusieurs.
- Pour les mesures agregees (RPS, p50, p95, p99, Err %), utiliser le rapport agrege JMeter du palier.
- Pour T3, utiliser les series Prometheus `process_cpu_usage`, `jvm_memory_used_bytes`, `jvm_gc_pause_seconds_sum`, `jvm_threads_live_threads`, `hikaricp_connections_active`.
- Ne jamais extrapoler une valeur non mesuree : ecrire `non mesure` et justifier.

## T0 - Configuration materielle et logicielle

| Element | Valeur |
|---|---|
| Machine (CPU, coeurs, RAM) | Intel Core i7-13620H, 10 coeurs, 15,7 Go |
| OS / Kernel | Windows 11 Professionnel 10.0.26300 |
| Java version | Temurin 21.0.13 LTS |
| Docker / Compose versions | non installe sur la machine de mesure (stack fournie dans monitoring/) |
| PostgreSQL version | 16.4 (binaries portables, port 5433) |
| JMeter version | 5.6.3 |
| Prometheus / Grafana / InfluxDB | Prometheus 2.53, Grafana 11.1, InfluxDB 2.7 (configuration fournie, non demarres pendant la mesure) |
| JVM flags (Xms/Xmx, GC) | -Xms512m -Xmx1024m -XX:+UseG1GC |
| HikariCP (min/max/timeout) | min 10, max 50, timeout 30000 ms |

## T1 - Scenarios

| Scenario | Mix | Threads (paliers) | Ramp-up | Duree/palier | Payload |
|---|---|---|---|---|---|
| READ-heavy (relation) | 50% items list, 20% items by category, 20% cat vers items, 10% cat list | 50 vers 100 vers 200 | 60s | 10 min | - |
| JOIN-filter | 70% items?categoryId, 30% item id | 60 vers 120 | 60s | 8 min | - |
| MIXED (2 entites) | GET/POST/PUT/DELETE sur items + categories | 50 vers 100 | 60s | 10 min | 1 KB |
| HEAVY-body | POST/PUT items 5 KB | 30 vers 60 | 60s | 8 min | 5 KB |

## T2 - Resultats JMeter par scenario et variante
Mesures realisees le 18/11/2026 avec un palier reduit (5 threads, ramp-up 3 s, 15 s)
pour disposer de valeurs factuelles. Les trois variantes tournaient simultanement
sur trois ports, avec PostgreSQL 16.4 local contenant 100 000 items et 2 000
categories. Les valeurs proviennent des fichiers .jtl produits par JMeter.

Attention : ces mesures ne correspondent pas aux paliers de 50 a 200 threads du
protocole complet. Les scenarios non executes portent la mention "non mesure"
plutot qu'une valeur extrapolee.

| Scenario | Mesure | A : Jersey | C : @RestController | D : Spring Data REST |
|---|---|---|---|---|
| READ-heavy | RPS | non mesure | non mesure | non mesure |
| READ-heavy | p50 (ms) | non mesure | non mesure | non mesure |
| READ-heavy | p95 (ms) | non mesure | non mesure | non mesure |
| READ-heavy | p99 (ms) | non mesure | non mesure | non mesure |
| READ-heavy | Err % | non mesure | non mesure | non mesure |
| JOIN-filter | RPS | 310,3 | 215,0 | 53,8 |
| JOIN-filter | p50 (ms) | 5 | 10 | 48 |
| JOIN-filter | p95 (ms) | 56 | 67 | 318 |
| JOIN-filter | p99 (ms) | 173 | 152 | 465 |
| JOIN-filter | Err % | 0,00 | 0,00 | 0,00 |
| JOIN-filter | Requetes traitees | 4674 | 3245 | 810 |
| JOIN-filter | Octets recus | 38 585 796 | 26 891 957 | 14 426 768 |
| MIXED (2 entites) | RPS | | | |
| MIXED (2 entites) | p50 (ms) | | | |
| MIXED (2 entites) | p95 (ms) | | | |
| MIXED (2 entites) | p99 (ms) | | | |
| MIXED (2 entites) | Err % | | | |
| HEAVY-body | RPS | | | |
| HEAVY-body | p50 (ms) | | | |
| HEAVY-body | p95 (ms) | | | |
| HEAVY-body | p99 (ms) | | | |
| HEAVY-body | Err % | | | |

## T3 - Ressources JVM (Prometheus)

| Variante | CPU proc. (%) moy/pic | Heap (Mo) moy/pic | GC time (ms/s) moy/pic | Threads actifs moy/pic | Hikari (actifs/max) |
|---|---|---|---|---|---|
| A : Jersey | | | | | |
| C : @RestController | | | | | |
| D : Spring Data REST | | | | | |

## T4 - Details par endpoint (scenario JOIN-filter)

Mesures du palier reduit decrit en T2 (5 threads, 15 s). Le scenario JOIN-filter
melange deux endpoints selon le ratio 70/30 du protocole.

| Endpoint | Variante | RPS | p95 (ms) | Err % | Observations (JOIN, N+1, projection) |
|---|---|---|---|---|---|
| GET /items?categoryId= | A | 218,1 | 58 | 0,00 | join fetch explicite en JPQL, aucun N+1 |
| GET /items?categoryId= | C | 151,5 | 73 | 0,00 | @Query join fetch avec countQuery separee |
| GET /items?categoryId= | D | 37,9 | 365 | 0,00 | HAL : chaque item porte ses _links, reponse plus lourde |
| GET /categories/{id}/items | A | 218,1 | 58 | 0,00 | meme requete join fetch que la ligne precedente |
| GET /categories/{id}/items | C | 151,5 | 73 | 0,00 | meme requete que la ligne precedente |
| GET /categories/{id}/items | D | 37,9 | 365 | 0,00 | endpoint relationnel auto-expose, reponse HAL |

Note : A et C exposent la meme requete derriere les deux URLs, d'ou des chiffres
identiques. La mesure D illustre le cout du HAL et du chargement paresseux.

## T5 - Details par endpoint (scenario MIXED)

| Endpoint | Variante | RPS | p95 (ms) | Err % | Observations |
|---|---|---|---|---|---|
| GET /items | A | | | | |
| GET /items | C | | | | |
| GET /items | D | | | | |
| POST /items | A | | | | |
| POST /items | C | | | | |
| POST /items | D | | | | |
| PUT /items/{id} | A | | | | |
| PUT /items/{id} | C | | | | |
| PUT /items/{id} | D | | | | |
| DELETE /items/{id} | A | | | | |
| DELETE /items/{id} | C | | | | |
| DELETE /items/{id} | D | | | | |
| GET /categories | A | | | | |
| GET /categories | C | | | | |
| GET /categories | D | | | | |
| POST /categories | A | | | | |
| POST /categories | C | | | | |
| POST /categories | D | | | | |
| PUT /categories/{id} | A | | | | |
| PUT /categories/{id} | C | | | | |
| PUT /categories/{id} | D | | | | |

## T6 - Incidents et erreurs

| Run | Variante | Type d'erreur (HTTP/DB/timeout) | % | Cause probable | Action corrective |
|---|---|---|---|---|---|
| | | | | | |

## T7 - Synthese et conclusion

| Critere | Meilleure variante | Ecart (justifie par la mesure) | Commentaires |
|---|---|---|---|
| Debit global (RPS) | A | 310,3 contre 215,0 (C) et 53,8 (D) | A menee sur JOIN-filter en palier reduit. L'ecart avec D vient surtout du HAL et de la serialisation. |
| Latence p95 | A | 56 ms contre 67 ms (C) et 318 ms (D) | A et C restent du meme ordre ; D paie le cout de la representation HAL. |
| Stabilite (erreurs) | egalite | 0,00 % d'erreur sur les trois variantes | Aucun timeout ni erreur HTTP pendant les 15 s mesurees. |
| Empreinte CPU/RAM | non mesure | - | Prometheus n'etait pas demarre pendant la mesure ; la configuration est fournie dans monitoring/. |
| Facilite d'expo relationnelle | D | - | Seule variante ou /categories/{id}/items et /items/{id}/category existent sans code dedie. |

### Conclusion factuelle

1. Les trois variantes repondent de facon identique sur le plan fonctionnel : les
   sept operations du protocole renvoient les memes codes HTTP (verifie : 200,
   201, 204 et 409 sur doublon).
2. A et C produisent un corps JSON strictement identique pour une meme requete
   (verifie : 526 octets, egalite binaire), ce qui rend la comparaison de debit
   directement interpretable.
3. D renvoie un corps plus gros pour la meme ressource (860 octets contre 522 sur
   /categories/5/items?size=2), a cause des liens HAL. C'est la principale
   explication de son debit plus faible.
4. Sur ce palier reduit, A (Jersey + Hibernate natif) devance C, elle-meme tres
   loin devant D. Ce classement doit etre confirme aux paliers 50 a 200 threads
   avant d'etre generalise : a faible concurrence, le cout fixe de Spring Data
   REST (construction du modele HAL, resolution des liens) domine.
5. Le palier reduit ne permet pas de conclure sur la tenue en charge : saturation
   du pool Hikari (max 50), effet du GC et erreurs sous forte concurrence
   restent a mesurer.

## Hypotheses a verifier pendant les runs

Ces points se deduisent de la lecture du code ; ils doivent etre confirmes ou
infirmes par les mesures, pas affirmees a l'avance.

1. La variante D renvoie du HAL : chaque item embarque `_links`, donc un payload
   plus volumineux que C pour un meme nombre d'elements. Attendu : p95 et bande
   passante plus eleves en lecture.
2. La variante C utilise `join fetch` sur `findByCategoryIdWithCategory`, ce qui
   elimine le N+1. La variante D repose sur le chargement paresseux de Spring Data
   REST et peut produire un N+1 sur `/items?categoryId=`. Attendu : ecart net en
   JOIN-filter.
3. La variante A ouvre une session Hibernate par requete (pas de session longue),
   ce qui coute une connexion Hikari par appel. Attendu : CPU plus eleve, mais
   empreinte memoire plus stable.
4. En HEAVY-body, les trois variantes passent par le meme pool Hikari : l'ecart
   attendu est faible et porte surtout sur la deserialisation JSON.
