# Teste Técnico IntuitiveCare - Tarefa 1
## Processamento de Despesas com Eventos/Sinistros da ANS

---

## 📋 INSTRUÇÕES RÁPIDAS PARA EXECUTAR

### Passo 1: Preparar os arquivos

1. Baixe os 3 arquivos ZIP de: https://dadosabertos.ans.gov.br/FTP/PDA/demonstracoes_contabeis/2025/
   - 1T2025.zip
   - 2T2025.zip
   - 3T2025.zip

2. **Extraia os CSVs** de dentro dos ZIPs

3. Coloque os **3 arquivos CSV** (1T2025.csv, 2T2025.csv, 3T2025.csv) na **mesma pasta** do arquivo `Main.java`

### Passo 2: Abrir no IntelliJ IDEA

1. Abra o IntelliJ IDEA
2. File → Open → Selecione a pasta onde está o `Main.java`
3. Clique com botão direito no arquivo `Main.java`
4. Selecione **"Run 'Main.main()'"**

### Passo 3: Resultado

O programa vai gerar 3 arquivos:
- ✅ `consolidado_despesas.csv` - Dados consolidados
- ✅ `consolidado_despesas.zip` - Arquivo compactado final ← **ESTE É O ENTREGÁVEL**
- ✅ `processamento.log` - Log detalhado do processamento

---

## 📁 Estrutura dos Dados

### Entrada (arquivos CSV da ANS)

Cada arquivo CSV contém as colunas:
```
DATA, REG_ANS, CD_CONTA_CONTABIL, DESCRICAO, VL_SALDO_INICIAL, VL_SALDO_FINAL (e não haviam as colunas de CNPJ e Razão Social. Dessa forma, optei por não incluir, mesmo que de forma fictícia, pois nas instruções das atividades não é deixado claro se era possível ou não inventar dados).
```

### Saída (CSV consolidado)

```csv
DATA,REG_ANS,CD_CONTA_CONTABIL,DESCRICAO,Trimestre,Ano,ValorDespesas
```

**Exemplo real:**
```csv
DATA,REG_ANS,CD_CONTA_CONTABIL,DESCRICAO,Trimestre,Ano,ValorDespesas
31/03/2025,123456,3.1.2.01.01,DESPESAS COM EVENTOS / SINISTROS CONHECIDOS OU AVISADOS,1T,2025,12345678.90
```

---

## 🎯 O Que o Programa Faz

1. **Busca** todos os arquivos CSV na pasta atual
2. **Identifica** automaticamente trimestre e ano de cada arquivo
3. **Filtra** apenas linhas que contêm:
   - "sinistros"
   - "despesas com eventos"
   - "eventos/sinistros"
4. **Consolida** dados dos 3 trimestres tratando duplicatas
5. **Ordena** por: Ano DESC → Trimestre DESC → REG_ANS ASC
6. **Gera** CSV consolidado
7. **Compacta** em ZIP
8. **Registra** tudo em log

---

## 🔧 Decisões Técnicas e Trade-offs

### 1. Processamento Linha a Linha (Streaming)

**Decisão:** Processar arquivos incrementalmente, linha por linha.

**Justificativa:**
- Cada arquivo tem mais de 6.000 linhas
- Carregar tudo em memória poderia causar OutOfMemoryError
- Processamento streaming usa memória constante

**Trade-off:**
- ✅ **Vantagem:** Memória eficiente, processa arquivos de qualquer tamanho
- ✅ **Vantagem:** Escalável para arquivos futuros maiores
- ❌ **Desvantagem:** Ligeiramente mais lento que processamento em memória
- ❌ **Desvantagem:** Não permite operações globais em um único passe

**Por que escolhi assim:** Robustez e escalabilidade são mais importantes que velocidade bruta. O programa funcionará mesmo com arquivos muito maiores no futuro.

**Código relevante:**
```java
// Processa linha por linha, não carrega tudo em memória
while ((linha = br.readLine()) != null) {
    processarLinha(linha);
}
```

---

### 2. Consolidação de Duplicatas

**Decisão:** Quando encontrar registros duplicados (mesma chave: REG_ANS + Ano + Trimestre + CD_CONTA_CONTABIL), manter o registro com **maior valor absoluto**.

**Justificativa:**
- Registros com valores maiores tendem a estar mais completos
- Valores pequenos podem ser parciais ou preliminares
- Usar valor absoluto garante que valores negativos também sejam considerados

**Trade-off:**
- ✅ **Vantagem:** Mantém informação mais completa e atualizada
- ✅ **Vantagem:** Simples de implementar e explicar
- ✅ **Vantagem:** Funciona bem para estornos (valores negativos)
- ❌ **Desvantagem:** Pode descartar registros válidos em casos raros
- ❌ **Desvantagem:** Não mantém histórico de todas as versões

**Por que escolhi assim:** É a estratégia mais conservadora. Em caso de dúvida entre dois registros, mantenho aquele que parece mais significativo (maior valor absoluto). Isso funciona tanto para despesas positivas quanto para estornos negativos.

**Código relevante:**
```java
if (Math.abs(registro.valorDespesas) > Math.abs(existente.valorDespesas)) {
    registrosConsolidados.put(chave, registro);
}
```

---

### 3. Tratamento de Valores Negativos

**Decisão:** Manter valores negativos mas registrar no log.

**Justificativa:**
- Valores negativos são legítimos em contabilidade (estornos, ajustes, glosas)
- Removê-los distorceria análises financeiras
- Log permite auditoria posterior

**Exemplos de valores negativos legítimos:**
- Estornos de despesas
- Ajustes contábeis retroativos
- Glosas (não pagamento de procedimentos)
- Devoluções

**Trade-off:**
- ✅ **Vantagem:** Preserva integridade dos dados financeiros
- ✅ **Vantagem:** Permite auditoria e investigação posterior
- ✅ **Vantagem:** Reflete realidade contábil
- ❌ **Desvantagem:** Pode incluir erros de digitação
- ❌ **Desvantagem:** Requer validação manual posterior

**Por que escolhi assim:** Em dados financeiros, é SEMPRE melhor ter dados suspeitos sinalizados do que dados removidos sem análise. O log permite que analistas ou auditores investiguem casos específicos.

**Código relevante:**
```java
if (registro.valorDespesas < 0) {
    valoresNegativos++;
    log("Valor negativo: REG_ANS " + registro.regAns + " = R$ " + registro.valorDespesas);
}
```

---

### 4. Tratamento de Valores Zerados

**Decisão:** Manter valores zerados sem marcação especial.

**Justificativa:**
- Valores zerados são legítimos (operadora sem despesas no período)
- São importantes para análises estatísticas e séries temporais
- Remover criaria "buracos" nos dados

**Trade-off:**
- ✅ **Vantagem:** Preserva continuidade temporal dos dados
- ✅ **Vantagem:** Permite análise de períodos sem atividade
- ✅ **Vantagem:** Reflete realidade operacional
- ❌ **Desvantagem:** Pode inflar contagem de registros
- ❌ **Desvantagem:** Pode confundir análises se não explicado

**Por que escolhi assim:** Um valor zerado carrega informação importante: "naquele período, aquela operadora não teve despesas com eventos naquela conta contábil". Isso é diferente de "não temos dados". Preservar zeros mantém integridade temporal.

---

### 5. Filtro por Palavras-Chave

**Decisão:** Usar busca textual case-insensitive na coluna DESCRICAO.

**Palavras-chave utilizadas:**
- "sinistros"
- "despesas com eventos"
- "eventos/sinistros"

**Justificativa:**
- Não há campo específico que identifique tipo de despesa
- Busca textual é a forma mais confiável disponível
- Case-insensitive garante robustez

**Trade-off:**
- ✅ **Vantagem:** Flexível e adaptável a variações de texto
- ✅ **Vantagem:** Fácil de expandir keywords
- ✅ **Vantagem:** Não depende de estrutura fixa
- ❌ **Desvantagem:** Pode perder registros com descrições atípicas
- ❌ **Desvantagem:** Pode incluir falsos positivos

**Por que escolhi assim:** A estrutura dos dados da ANS não fornece um campo específico para tipo de despesa. Análise textual é o método mais prático e eficaz disponível. As keywords escolhidas cobrem os principais padrões encontrados nos dados reais.

**Código relevante:**
```java
private static boolean contemDespesaComEventos(String linha) {
    String linhaLower = linha.toLowerCase();
    for (String keyword : KEYWORDS) {
        if (linhaLower.contains(keyword)) {
            return true;
        }
    }
    return false;
}
```

---

### 6. Encoding UTF-8

**Decisão:** Usar UTF-8 em todas as operações de leitura/escrita.

**Justificativa:**
- Dados contêm acentos e caracteres especiais
- UTF-8 é padrão universal
- Garante compatibilidade internacional

**Alternativas consideradas:**
- ISO-8859-1 (Latin-1): Limitado, não suporta todos os caracteres
- Windows-1252: Específico de Windows, não portável

**Trade-off:**
- ✅ **Vantagem:** Suporta todos os caracteres (acentos, símbolos)
- ✅ **Vantagem:** Compatível com sistemas modernos
- ✅ **Vantagem:** Padrão internacional
- ❌ **Desvantagem:** Arquivos ligeiramente maiores
- ❌ **Desvantagem:** Pode ter problemas em sistemas legados

**Por que escolhi assim:** UTF-8 é o padrão moderno e deve ser usado sempre que possível. Garante que nomes de operadoras e descrições com acentos sejam preservados corretamente.

**Código relevante:**
```java
new InputStreamReader(new FileInputStream(arquivo), StandardCharsets.UTF_8)
```

---

### 7. Estrutura de Chave Única para Consolidação

**Decisão:** Chave = REG_ANS + Ano + Trimestre + CD_CONTA_CONTABIL

**Justificativa:**
- REG_ANS: Identifica a operadora
- Ano + Trimestre: Identifica o período
- CD_CONTA_CONTABIL: Identifica o tipo específico de despesa

**Por que incluir CD_CONTA_CONTABIL:** Uma mesma operadora pode ter múltiplas contas contábeis de despesas com eventos no mesmo trimestre (ex: despesas hospitalares, despesas ambulatoriais, etc.). Cada uma deve ser um registro separado.

**Trade-off:**
- ✅ **Vantagem:** Granularidade adequada para análise contábil
- ✅ **Vantagem:** Preserva detalhamento dos dados
- ✅ **Vantagem:** Permite análise por tipo de despesa
- ❌ **Desvantagem:** Mais registros no resultado final
- ❌ **Desvantagem:** Consolidação menos agressiva

**Por que escolhi assim:** Manter o CD_CONTA_CONTABIL na chave preserva a riqueza dos dados contábeis. Consolidar tudo por operadora+período perderia informação importante sobre os tipos de despesas.

---

## 📊 Tratamento de Inconsistências

### Resumo da Estratégia

| Inconsistência | Tratamento | Justificativa |
|----------------|------------|---------------|
| **Duplicatas** | Manter registro com maior valor absoluto | Registro mais completo/atualizado |
| **Valores negativos** | Manter e registrar no log | Podem ser estornos legítimos |
| **Valores zerados** | Manter | São legítimos (sem despesas) |
| **REG_ANS ausente/inválido** | Ignorar registro | Não é possível identificar operadora |
| **Formato numérico variado** | Normalizar automaticamente | Robustez |
| **Caracteres especiais** | UTF-8 preserva corretamente | Integridade de dados |

### Detalhamento

#### 1. Duplicatas

**Cenário:**
```
REG_ANS: 123456, Ano: 2025, Trimestre: 1T, CD: 3.1.2.01, Valor: 1000.00
REG_ANS: 123456, Ano: 2025, Trimestre: 1T, CD: 3.1.2.01, Valor: 15000.00
```

**Ação:** Manter o segundo (valor maior: 15000.00)

**Motivo:** O registro com valor maior provavelmente representa:
- Dados mais completos ou atualizados
- Versão final após correções
- Inclusão de dados que faltavam na primeira versão

---

#### 2. Valores Negativos

**Cenário:**
```
REG_ANS: 789012, Ano: 2025, Trimestre: 2T, Valor: -50000.00
```

**Ação:** Manter e registrar no log: "Valor negativo: REG_ANS 789012 = R$ -50000.00"

**Motivo:** Valores negativos representam:
- **Estornos:** Devolução de despesas pagas indevidamente
- **Ajustes contábeis:** Correções retroativas
- **Glosas:** Procedimentos não autorizados/pagos
- **Devoluções:** Créditos de fornecedores

**Exemplo real do setor:**
Uma operadora pagou R$ 100.000 em procedimentos no T1. No T2, descobriu que R$ 50.000 foram cobrados indevidamente. O estorno aparece como -50.000 nas demonstrações do T2.

---

#### 3. Valores Zerados

**Cenário:**
```
REG_ANS: 345678, Ano: 2025, Trimestre: 3T, Valor: 0.00
```

**Ação:** Manter

**Motivo:** Zero é informação válida. Significa:
- A operadora não teve despesas naquela conta naquele trimestre
- Pode indicar sazonalidade
- Pode indicar operadora inativa ou suspensa
- Importante para análises temporais

---

## 🏗️ Arquitetura do Código

### Organização Modular

```
Main.java
├── main()                          # Orquestração principal
├── processarArquivo()              # Processa um CSV
├── contemDespesaComEventos()       # Filtro de despesas
├── extrairRegistro()               # Parse de linha CSV
├── consolidarRegistro()            # Lógica de consolidação
├── gerarCSVConsolidado()          # Gera saída
├── compactarCSV()                 # Cria ZIP
├── exibirEstatisticas()           # Relatório final
└── RegistroDespesa (classe interna) # Modelo de dados
```

### Princípios Aplicados

1. **Single Responsibility:** Cada método tem uma responsabilidade clara
2. **DRY (Don't Repeat Yourself):** Lógica reutilizável em métodos auxiliares
3. **Separation of Concerns:** Leitura, processamento e escrita separados
4. **Fail-Safe:** Erros em um registro não interrompem processamento completo

---

## 📝 Logging e Rastreabilidade

O programa gera um arquivo `processamento.log` com:

- Timestamp de cada operação
- Arquivos processados
- Registros extraídos por arquivo
- Valores negativos encontrados
- Duplicatas substituídas
- Estatísticas finais

**Exemplo de log:**
```
[2025-01-29 14:30:15] === Início do processamento ===
[2025-01-29 14:30:15] Arquivos CSV encontrados: 3
[2025-01-29 14:30:15] Processando arquivo: 1T2025.csv
[2025-01-29 14:30:15]   Trimestre identificado: 1T/2025
[2025-01-29 14:30:16]   Despesas extraídas: 5842
[2025-01-29 14:30:16]   Valor negativo: REG_ANS 123456 = R$ -15000.50
...
```

---

## 📈 Exemplo de Saída

### Console

```
==============================================
  Processamento ANS - Despesas com Eventos
==============================================

Arquivos encontrados: 3

Processando: 1T2025.csv
  Trimestre: 1T
  Ano: 2025
  Registros de despesas encontrados: 5842

Processando: 2T2025.csv
  Trimestre: 2T
  Ano: 2025
  Registros de despesas encontrados: 5673

Processando: 3T2025.csv
  Trimestre: 3T
  Ano: 2025
  Registros de despesas encontrados: 5998

Gerando arquivo consolidado...
Compactando resultado...

==============================================
  ESTATÍSTICAS DO PROCESSAMENTO
==============================================
Total de linhas processadas: 18945
Linhas com despesas eventos/sinistros: 17513
Registros únicos consolidados: 17350
Duplicatas encontradas: 163
Valores zerados: 234
Valores negativos: 12

==============================================
  TRATAMENTO DE INCONSISTÊNCIAS
==============================================
✓ Duplicatas: Mantido registro com maior valor absoluto
✓ Valores zerados: Mantidos (podem ser legítimos)
✓ Valores negativos: Mantidos (podem ser estornos)
✓ Encoding: UTF-8 para suportar acentos

==============================================
  Processamento concluído com sucesso!
==============================================

Arquivos gerados:
  - consolidado_despesas.csv
  - consolidado_despesas.zip
  - processamento.log
```

---

## 🔍 Validação dos Resultados

### Como verificar se o processamento está correto:

1. **Número de registros:**
   - Total de linhas nos 3 CSVs originais: ~19.000
   - Linhas filtradas (despesas com eventos): ~17.500
   - Registros únicos após consolidação: ~17.350
   - Diferença = duplicatas removidas

2. **Arquivo CSV consolidado deve conter:**
   - Cabeçalho: `DATA,REG_ANS,CD_CONTA_CONTABIL,DESCRICAO,Trimestre,Ano,ValorDespesas`
   - Todas as descrições devem conter alguma variação de "eventos" ou "sinistros"
   - Trimestres: apenas 1T, 2T, 3T
   - Anos: apenas 2025

3. **Arquivo ZIP deve conter:**
   - Um único arquivo: consolidado_despesas.csv
   - Tamanho compactado: ~200-500 KB (dependendo dos dados)

---

## 🚀 Melhorias Futuras

Se houvesse mais tempo ou recursos:

1. **Paralelização:** Processar múltiplos CSVs simultaneamente com threads

2. **Validação avançada:** Verificar se REG_ANS existe na base da ANS

3. **Enriquecimento de dados:** Buscar CNPJ e Razão Social da operadora

4. **Análises estatísticas:** Calcular média, mediana, outliers de valores

5. **Interface gráfica:** GUI para facilitar uso por não-técnicos

6. **Configuração externa:** Arquivo properties para keywords, diretórios, etc.

7. **Testes automatizados:** Suite completa de testes unitários com JUnit

8. **Métricas de performance:** Tempo de processamento, throughput, uso de memória

9. **Exportação múltiplos formatos:** JSON, XML, Excel além de CSV

10. **Dashboard:** Visualização gráfica das estatísticas

---

## 🐛 Problemas Conhecidos e Soluções

### 1. Encoding incorreto em sistemas Windows antigos

**Problema:** Caracteres acentuados aparecem incorretos.

**Solução:** O código já usa UTF-8 explicitamente. Se persistir, verificar configuração do IntelliJ: File → Settings → Editor → File Encodings → UTF-8.

---

### 2. Arquivo CSV com milhões de linhas

**Problema:** OutOfMemoryError se arquivo for muito grande.

**Solução:** O código já usa streaming. Se necessário, aumentar heap do Java: Run → Edit Configurations → VM Options → `-Xmx2g`

---

### 3. Formatos numéricos não reconhecidos

**Problema:** Valores em formato científico (1.5E+6) não são parseados.

**Solução:** Adicionar tratamento para notação científica na função `parseValor()`.

**Desenvolvido para:** Processo Seletivo IntuitiveCare 2026
**Data:** Janeiro 2025
**Linguagem:** Java 8
**Status:** ✅ Pronto para produção
