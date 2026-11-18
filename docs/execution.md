# Scripts de lancement du benchmark REST
#
# Toutes les commandes ci-dessous supposent PowerShell 7 ou Windows PowerShell 5.1.
# Les chemins des outils sont sous C:\Users\qchii\perf-toolchain ; adapter les
# variables d'environnement si la toolchain est ailleurs.

## 1. Base de donnees

```powershell
# Initialisation (une seule fois)
& $env:PG_HOME\bin\initdb.exe -D C:\Users\qchii\perf-toolchain\pgdata -U bench --auth=trust --encoding=UTF8

# Demarrage sur le port 5433 pour ne pas perturber une instance existante
& $env:PG_HOME\bin\pg_ctl.exe -D C:\Users\qchii\perf-toolchain\pgdata -o "-p 5433" -l C:\Users\qchii\perf-toolchain\pg.log start

# Creation de la base
& $env:PG_HOME\bin\createdb.exe -h localhost -p 5433 -U bench bench

# Schema puis donnees
& $env:PG_HOME\bin\psql.exe -h localhost -p 5433 -U bench -d bench -f db\schema.sql
& $env:PG_HOME\bin\psql.exe -h localhost -p 5433 -U bench -d bench -f db\seed.sql
```

Verification :

```powershell
& $env:PG_HOME\bin\psql.exe -h localhost -p 5433 -U bench -d bench -c "select (select count(*) from category) as categories, (select count(*) from item) as items;"
```

Attendu : 2000 categories, 100000 items.

## 2. Variantes REST

Chaque variante utilise un port distinct pour permettre des comparaisons
successives sans conflit.

```powershell
# Ports : A = 8081, C = 8082, D = 8083

# Variante A (Jersey + Hibernate, war Jetty)
cd variant-a-jersey
mvn jetty:run

# Variante C (Spring Boot @RestController)
cd variant-c-restcontroller
java -Xms512m -Xmx1024m -XX:+UseG1GC -jar target\variant-c.jar

# Variante D (Spring Data REST)
cd variant-d-springdatarest
java -Xms512m -Xmx1024m -XX:+UseG1GC -jar target\variant-d.jar
```

Verification rapide sur chaque variante :

```powershell
Invoke-RestMethod http://localhost:8082/api/items?page=0&size=2
Invoke-RestMethod http://localhost:8082/api/categories?page=0&size=2
Invoke-RestMethod http://localhost:8082/api/categories/1/items?page=0&size=2
Invoke-RestMethod http://localhost:8082/api/items/1
```

## 3. Monitoring

```powershell
cd monitoring
docker compose up -d
```

- Prometheus : http://localhost:9090
- InfluxDB : http://localhost:8086
- Grafana : http://localhost:3000 (admin / admin)

## 4. Scenarios JMeter

Les quatre plans partagent les memes parametres. Exemple pour READ-heavy sur la
variante C, palier 50 threads :

```powershell
$JM = "C:\Users\qchii\perf-toolchain\apache-jmeter-5.6.3\bin\jmeter.bat"
$CSV = "C:\Users\qchii\Desktop\rest-benchmark\jmeter\csv"

& $JM -n -t jmeter\read-heavy.jmx `
  -Jhost=localhost -Jport=8082 -Jbase=/api `
  -Jcsv=$CSV `
  -Jthreads=50 -Jrampup=60 -Jduration=600 `
  -Jlabel=READ-C-50 `
  -Jresults="C:\Users\qchii\Desktop\rest-benchmark\jmeter\results\READ-C-50" `
  -JinfluxEnabled=true -JinfluxToken=bench-token
```

Paliers a enchainer pour READ-heavy : 50, 100, 200.
Pour JOIN-filter : 60, 120. MIXED : 50, 100. HEAVY-body : 30, 60.

### Matrice complete des runs

| Variante | Scenario | Paliers | Label JMeter |
|---|---|---|---|
| A | READ-heavy | 50, 100, 200 | READ-A-50, READ-A-100, READ-A-200 |
| A | JOIN-filter | 60, 120 | JOIN-A-60, JOIN-A-120 |
| A | MIXED | 50, 100 | MIXED-A-50, MIXED-A-100 |
| A | HEAVY-body | 30, 60 | HEAVY-A-30, HEAVY-A-60 |
| C | READ-heavy | 50, 100, 200 | READ-C-50, READ-C-100, READ-C-200 |
| C | JOIN-filter | 60, 120 | JOIN-C-60, JOIN-C-120 |
| C | MIXED | 50, 100 | MIXED-C-50, MIXED-C-100 |
| C | HEAVY-body | 30, 60 | HEAVY-C-30, HEAVY-C-60 |
| D | READ-heavy | 50, 100, 200 | READ-D-50, READ-D-100, READ-D-200 |
| D | JOIN-filter | 60, 120 | JOIN-D-60, JOIN-D-120 |
| D | MIXED | 50, 100 | MIXED-D-50, MIXED-D-100 |
| D | HEAVY-body | 30, 60 | HEAVY-D-30, HEAVY-D-60 |

Soit 24 runs au total avec les durees du protocole.

## 5. Analyse des resultats

Les fichiers `.jtl` produits sont au format CSV. Generation du rapport HTML :

```powershell
& $JM -g jmeter\results\READ-C-50.jtl -o jmeter\results\report-READ-C-50
```

Extraction des percentiles directement depuis le `.jtl` :

```powershell
Import-Csv jmeter\results\READ-C-50.jtl |
  Measure-Object -Property elapsed -Average -Minimum -Maximum
```

Pour p95 et p99, trier la colonne `elapsed` puis indexer, ou passer par le
rapport HTML qui les calcule.
