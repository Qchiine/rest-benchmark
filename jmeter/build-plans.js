#!/usr/bin/env node
/**
 * Genere les 4 plans JMeter (.jmx) du benchmark REST.
 *
 * Un seul generateur declaratif pour eviter toute divergence entre scenarios :
 * memes parametres, meme CSV, meme listener InfluxDB, seuls les samplers
 * et les pourcentages changent.
 *
 * Usage : node build-plans.js
 */

const fs = require('node:fs');
const path = require('node:path');

const DOSSIER = __dirname;

function esc(valeur) {
  return String(valeur)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function argHttp(nom, valeur) {
  return `                <elementProp name="${esc(nom)}" elementType="HTTPArgument">
                  <boolProp name="HTTPArgument.always_encode">false</boolProp>
                  <stringProp name="Argument.name">${esc(nom)}</stringProp>
                  <stringProp name="Argument.value">${esc(valeur)}</stringProp>
                  <stringProp name="Argument.metadata">=</stringProp>
                  <boolProp name="HTTPArgument.use_equals">true</boolProp>
                </elementProp>`;
}

function sampler({ nom, chemin, methode, parametres, corps }) {
  let argumentsXml;
  if (corps) {
    argumentsXml = `            <boolProp name="HTTPSampler.postBodyRaw">true</boolProp>
            <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
              <collectionProp name="Arguments.arguments">
                <elementProp name="" elementType="HTTPArgument">
                  <boolProp name="HTTPArgument.always_encode">false</boolProp>
                  <stringProp name="Argument.value">${esc(corps)}</stringProp>
                  <stringProp name="Argument.metadata">=</stringProp>
                </elementProp>
              </collectionProp>
            </elementProp>`;
  } else if (parametres && parametres.length) {
    argumentsXml = `            <elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" enabled="true">
              <collectionProp name="Arguments.arguments">
${parametres.map(([c, v]) => argHttp(c, v)).join('\n')}
              </collectionProp>
            </elementProp>`;
  } else {
    argumentsXml = `            <elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" enabled="true">
              <collectionProp name="Arguments.arguments"/>
            </elementProp>`;
  }

  const entete = corps
    ? `            <stringProp name="HTTPSampler.header_manager">Content-Type: application/json</stringProp>`
    : '';

  return `          <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="${esc(nom)}" enabled="true">
${argumentsXml}${entete}
            <stringProp name="HTTPSampler.path">${esc(chemin)}</stringProp>
            <stringProp name="HTTPSampler.method">${methode}</stringProp>
            <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
            <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
            <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
            <stringProp name="HTTPSampler.contentEncoding">UTF-8</stringProp>
          </HTTPSamplerProxy>
          <hashTree>
            <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="Entetes JSON" enabled="true">
              <collectionProp name="HeaderManager.headers">
                <elementProp name="" elementType="Header">
                  <stringProp name="Header.name">Content-Type</stringProp>
                  <stringProp name="Header.value">application/json</stringProp>
                </elementProp>
                <elementProp name="" elementType="Header">
                  <stringProp name="Header.name">Accept</stringProp>
                  <stringProp name="Header.value">application/json</stringProp>
                </elementProp>
              </collectionProp>
            </HeaderManager>
            <hashTree/>
          </hashTree>
`;
}

function blocConditionnel(nom, pourcentage, contenu) {
  return `        <ThroughputController guiclass="ThroughputControllerGui" testclass="ThroughputController" testname="${esc(nom)}" enabled="true">
          <intProp name="ThroughputController.style">1</intProp>
          <boolProp name="ThroughputController.perThread">false</boolProp>
          <intProp name="ThroughputController.maxThroughput">1</intProp>
          <stringProp name="ThroughputController.percentThroughput">${pourcentage}</stringProp>
        </ThroughputController>
        <hashTree>
${contenu}        </hashTree>
`;
}

function variable(nom, defaut) {
  const valeur = String(defaut)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
  return `          <elementProp name="${nom}" elementType="Argument">
            <stringProp name="Argument.name">${nom}</stringProp>
            <stringProp name="Argument.value">\${__P(${nom},${valeur})}</stringProp>
            <stringProp name="Argument.metadata">=</stringProp>
          </elementProp>`;
}

function plan({ fichier, nomPlan, commentaire, defauts, blocs }) {
  const xml = `<?xml version="1.0" encoding="UTF-8"?>
<!--
${commentaire}

  Parametres en ligne de commande :
    -Jhost=localhost
    -Jport=8082
    -Jbase=/api
    -Jcsv=<chemin absolu vers jmeter/csv>
    -Jthreads=${defauts.threads}
    -Jrampup=${defauts.rampup}
    -Jduration=${defauts.duration}
    -Jlabel=${defauts.label}
    -Jresults=./results/${nomPlan}
    -JinfluxEnabled=false
    -JinfluxUrl=http://localhost:8086/api/v2/write?org=perf&bucket=jmeter&precision=ms
    -JinfluxToken=bench-token
-->
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="${esc(nomPlan)}" enabled="true">
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="Variables" enabled="true">
        <collectionProp name="Arguments.arguments">
${variable('host', 'localhost')}
${variable('port', '8082')}
${variable('base', '/api')}
${variable('csv', './csv')}
${variable('threads', defauts.threads)}
${variable('rampup', defauts.rampup)}
${variable('duration', defauts.duration)}
${variable('label', defauts.label)}
${variable('results', './results/' + nomPlan)}
${variable('influxEnabled', 'false')}
${variable('influxUrl', 'http://localhost:8086/api/v2/write?org=perf&bucket=jmeter&precision=ms')}
${variable('influxToken', 'bench-token')}
        </collectionProp>
      </elementProp>
    </TestPlan>
    <hashTree>
      <ConfigTestElement guiclass="HttpDefaultsGui" testclass="ConfigTestElement" testname="HTTP Request Defaults" enabled="true">
        <elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="Parametres" enabled="true">
          <collectionProp name="Arguments.arguments"/>
        </elementProp>
        <stringProp name="HTTPSampler.domain">\${host}</stringProp>
        <stringProp name="HTTPSampler.port">\${port}</stringProp>
        <stringProp name="HTTPSampler.protocol">http</stringProp>
        <stringProp name="HTTPSampler.contentEncoding">UTF-8</stringProp>
        <stringProp name="HTTPSampler.connect_timeout">10000</stringProp>
        <stringProp name="HTTPSampler.response_timeout">30000</stringProp>
        <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
      </ConfigTestElement>
      <hashTree/>
      <CSVDataSet guiclass="TestBeanGUI" testclass="CSVDataSet" testname="CSV identifiants" enabled="true">
        <stringProp name="delimiter">,</stringProp>
        <stringProp name="fileEncoding">UTF-8</stringProp>
        <stringProp name="filename">\${csv}/ids.csv</stringProp>
        <boolProp name="ignoreFirstLine">true</boolProp>
        <boolProp name="quotedData">false</boolProp>
        <boolProp name="recycle">true</boolProp>
        <stringProp name="shareMode">shareMode.all</stringProp>
        <boolProp name="stopThread">false</boolProp>
        <stringProp name="variableNames">category_id,item_id</stringProp>
      </CSVDataSet>
      <hashTree/>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="${esc(nomPlan)}" enabled="true">
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControlPanel" testclass="LoopController" testname="Boucle" enabled="true">
          <boolProp name="LoopController.continue_forever">true</boolProp>
          <intProp name="LoopController.loops">-1</intProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">\${threads}</stringProp>
        <stringProp name="ThreadGroup.ramp_time">\${rampup}</stringProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
        <stringProp name="ThreadGroup.duration">\${duration}</stringProp>
        <stringProp name="ThreadGroup.delay">0</stringProp>
        <boolProp name="ThreadGroup.same_user_on_next_iteration">true</boolProp>
      </ThreadGroup>
      <hashTree>
        <RandomVariableConfig guiclass="TestBeanGUI" testclass="RandomVariableConfig" testname="Tirage du mix" enabled="true">
          <stringProp name="maximumValue">100</stringProp>
          <stringProp name="minimumValue">1</stringProp>
          <stringProp name="perThread">true</stringProp>
          <stringProp name="randomSeed">0</stringProp>
          <stringProp name="variableName">tirage</stringProp>
        </RandomVariableConfig>
        <hashTree/>
${blocs.join('')}        <ResultCollector guiclass="StatVisualizer" testclass="ResultCollector" testname="Rapport agrege" enabled="true">
          <boolProp name="ResultCollector.error_logging">false</boolProp>
          <objProp>
            <name>saveConfig</name>
            <value class="SampleSaveConfiguration">
              <time>true</time>
              <latency>true</latency>
              <timestamp>true</timestamp>
              <success>true</success>
              <label>true</label>
              <code>true</code>
              <message>true</message>
              <threadName>true</threadName>
              <dataType>true</dataType>
              <encoding>false</encoding>
              <assertions>true</assertions>
              <subresults>true</subresults>
              <responseData>false</responseData>
              <samplerData>false</samplerData>
              <xml>false</xml>
              <fieldNames>true</fieldNames>
              <responseHeaders>false</responseHeaders>
              <requestHeaders>false</requestHeaders>
              <responseDataOnError>false</responseDataOnError>
              <saveAssertionResultsFailureMessage>true</saveAssertionResultsFailureMessage>
              <assertionsResultsToSave>0</assertionsResultsToSave>
              <bytes>true</bytes>
              <sentBytes>true</sentBytes>
              <url>true</url>
              <threadCounts>true</threadCounts>
              <idleTime>true</idleTime>
              <connectTime>true</connectTime>
            </value>
          </objProp>
          <stringProp name="filename">\${results}.jtl</stringProp>
        </ResultCollector>
        <hashTree/>
        <ResultCollector guiclass="SummaryReport" testclass="ResultCollector" testname="Resume" enabled="true">
          <boolProp name="ResultCollector.error_logging">false</boolProp>
          <objProp>
            <name>saveConfig</name>
            <value class="SampleSaveConfiguration">
              <time>true</time>
              <latency>true</latency>
              <timestamp>true</timestamp>
              <success>true</success>
              <label>true</label>
              <code>true</code>
              <message>true</message>
              <threadName>true</threadName>
              <dataType>true</dataType>
              <encoding>false</encoding>
              <assertions>true</assertions>
              <subresults>true</subresults>
              <responseData>false</responseData>
              <samplerData>false</samplerData>
              <xml>false</xml>
              <fieldNames>true</fieldNames>
              <responseHeaders>false</responseHeaders>
              <requestHeaders>false</requestHeaders>
              <responseDataOnError>false</responseDataOnError>
              <saveAssertionResultsFailureMessage>true</saveAssertionResultsFailureMessage>
              <assertionsResultsToSave>0</assertionsResultsToSave>
              <bytes>true</bytes>
              <sentBytes>true</sentBytes>
              <url>true</url>
              <threadCounts>true</threadCounts>
              <idleTime>true</idleTime>
              <connectTime>true</connectTime>
            </value>
          </objProp>
          <stringProp name="filename">\${results}-summary.txt</stringProp>
        </ResultCollector>
        <hashTree/>
        <BackendListener guiclass="BackendListenerGui" testclass="BackendListener" testname="InfluxDB v2" enabled="true">
          <elementProp name="arguments" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" enabled="true">
            <collectionProp name="Arguments.arguments">
              <elementProp name="influxdbMetricsSender" elementType="Argument">
                <stringProp name="Argument.name">influxdbMetricsSender</stringProp>
                <stringProp name="Argument.value">org.apache.jmeter.visualizers.backend.influxdb.HttpMetricsSender</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
              <elementProp name="influxdbUrl" elementType="Argument">
                <stringProp name="Argument.name">influxdbUrl</stringProp>
                <stringProp name="Argument.value">\${influxUrl}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
              <elementProp name="application" elementType="Argument">
                <stringProp name="Argument.name">application</stringProp>
                <stringProp name="Argument.value">\${label}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
              <elementProp name="measurement" elementType="Argument">
                <stringProp name="Argument.name">measurement</stringProp>
                <stringProp name="Argument.value">jmeter</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
              <elementProp name="summaryOnly" elementType="Argument">
                <stringProp name="Argument.name">summaryOnly</stringProp>
                <stringProp name="Argument.value">false</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
              <elementProp name="samplersRegex" elementType="Argument">
                <stringProp name="Argument.name">samplersRegex</stringProp>
                <stringProp name="Argument.value">.*</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
              <elementProp name="percentiles" elementType="Argument">
                <stringProp name="Argument.name">percentiles</stringProp>
                <stringProp name="Argument.value">50;95;99</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
              <elementProp name="TAG_NAME" elementType="Argument">
                <stringProp name="Argument.name">TAG_NAME</stringProp>
                <stringProp name="Argument.value">variant</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
              <elementProp name="influxToken" elementType="Argument">
                <stringProp name="Argument.name">influxToken</stringProp>
                <stringProp name="Argument.value">\${influxToken}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
          <stringProp name="classname">org.apache.jmeter.visualizers.backend.influxdb.InfluxdbBackendListenerClient</stringProp>
        </BackendListener>
        <hashTree/>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
`;
  fs.writeFileSync(path.join(DOSSIER, fichier), xml, 'utf8');
  console.log(`  ${fichier} (${(xml.length / 1024).toFixed(1)} Ko)`);
}

// --- READ-heavy ------------------------------------------------------------
plan({
  fichier: 'read-heavy.jmx',
  nomPlan: 'READ-heavy',
  commentaire: `  Scenario READ-heavy (relation incluse)

  Mix du protocole :
    50% GET /items?page=&size=50
    20% GET /items?categoryId=...&page=&size=
    20% GET /categories/{id}/items?page=&size=
    10% GET /categories?page=&size=

  Paliers prevus par le protocole : 50 -> 100 -> 200 threads,
  ramp-up 60 s, 10 min par palier.`,
  defauts: { threads: 50, rampup: 60, duration: 600, label: 'READ-A' },
  blocs: [
    blocConditionnel('50% items liste', 50,
      sampler({ nom: 'GET items liste', chemin: '${base}/items', methode: 'GET', parametres: [['page', '0'], ['size', '50']] })),
    blocConditionnel('20% items par categorie', 20,
      sampler({
        nom: 'GET items filtre categorie',
        chemin: '${base}/items',
        methode: 'GET',
        parametres: [['categoryId', '${category_id}'], ['page', '0'], ['size', '50']],
      })),
    blocConditionnel('20% categorie vers items', 20,
      sampler({
        nom: 'GET categorie items',
        chemin: '${base}/categories/${category_id}/items',
        methode: 'GET',
        parametres: [['page', '0'], ['size', '50']],
      })),
    blocConditionnel('10% categories liste', 10,
      sampler({ nom: 'GET categories liste', chemin: '${base}/categories', methode: 'GET', parametres: [['page', '0'], ['size', '50']] })),
  ],
});

// --- JOIN-filter -----------------------------------------------------------
plan({
  fichier: 'join-filter.jmx',
  nomPlan: 'JOIN-filter',
  commentaire: `  Scenario JOIN-filter cible

  Mix du protocole :
    70% GET /items?categoryId=...&page=&size=
    30% GET /items/{id}

  Paliers prevus par le protocole : 60 -> 120 threads,
  ramp-up 60 s, 8 min par palier.`,
  defauts: { threads: 60, rampup: 60, duration: 480, label: 'JOIN-A' },
  blocs: [
    blocConditionnel('70% items par categorie', 70,
      sampler({
        nom: 'GET items filtre categorie',
        chemin: '${base}/items',
        methode: 'GET',
        parametres: [['categoryId', '${category_id}'], ['page', '0'], ['size', '50']],
      })),
    blocConditionnel('30% item par id', 30,
      sampler({ nom: 'GET item par id', chemin: '${base}/items/${item_id}', methode: 'GET' })),
  ],
});

// --- MIXED -----------------------------------------------------------------
plan({
  fichier: 'mixed.jmx',
  nomPlan: 'MIXED',
  commentaire: `  Scenario MIXED (ecritures sur deux entites)

  Mix du protocole :
    40% GET /items?page=...
    20% POST /items (1 Ko)
    10% PUT /items/{id} (1 Ko)
    10% DELETE /items/{id}
    10% POST /categories (0.5-1 Ko)
    10% PUT /categories/{id}

  Paliers prevus par le protocole : 50 -> 100 threads,
  ramp-up 60 s, 10 min par palier.`,
  defauts: { threads: 50, rampup: 60, duration: 600, label: 'MIXED-A' },
  blocs: [
    blocConditionnel('40% items liste', 40,
      sampler({ nom: 'GET items liste', chemin: '${base}/items', methode: 'GET', parametres: [['page', '0'], ['size', '50']] })),
    blocConditionnel('20% POST item 1Ko', 20,
      sampler({ nom: 'POST item leger', chemin: '${base}/items', methode: 'POST', corps: '${body}' })),
    blocConditionnel('10% PUT item 1Ko', 10,
      sampler({ nom: 'PUT item leger', chemin: '${base}/items/${item_id}', methode: 'PUT', corps: '${body}' })),
    blocConditionnel('10% DELETE item', 10,
      sampler({ nom: 'DELETE item', chemin: '${base}/items/${item_id}', methode: 'DELETE' })),
    blocConditionnel('10% POST categorie', 10,
      sampler({
        nom: 'POST categorie',
        chemin: '${base}/categories',
        methode: 'POST',
        corps: '{"code":"BENCH-${__threadNum}-${__time(,)}","name":"Categorie benchmark"}',
      })),
    blocConditionnel('10% PUT categorie', 10,
      sampler({
        nom: 'PUT categorie',
        chemin: '${base}/categories/${category_id}',
        methode: 'PUT',
        corps: '{"code":"CAT-${category_id}","name":"Categorie modifiee"}',
      })),
  ],
});

// --- HEAVY-body ------------------------------------------------------------
plan({
  fichier: 'heavy-body.jmx',
  nomPlan: 'HEAVY-body',
  commentaire: `  Scenario HEAVY-body (payload 5 Ko)

  Mix du protocole :
    50% POST /items (5 Ko)
    50% PUT /items/{id} (5 Ko)

  Paliers prevus par le protocole : 30 -> 60 threads,
  ramp-up 60 s, 8 min par palier.`,
  defauts: { threads: 30, rampup: 60, duration: 480, label: 'HEAVY-A' },
  blocs: [
    blocConditionnel('50% POST item 5Ko', 50,
      sampler({ nom: 'POST item lourd', chemin: '${base}/items', methode: 'POST', corps: '${body}' })),
    blocConditionnel('50% PUT item 5Ko', 50,
      sampler({ nom: 'PUT item lourd', chemin: '${base}/items/${item_id}', methode: 'PUT', corps: '${body}' })),
  ],
});

console.log('Plans JMeter generes.');

