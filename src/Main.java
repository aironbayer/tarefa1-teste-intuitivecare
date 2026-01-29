import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Processador de Dados ANS - Despesas com Eventos/Sinistros
 *
 * Este programa consolida dados de demonstrações contábeis da ANS,
 * filtrando especificamente despesas com eventos/sinistros.
 *
 * Autor: Candidato Estágio IntuitiveCare
 * Data: Janeiro 2025
 */
public class Main {

    // Configurações
    private static final String ARQUIVO_SAIDA = "consolidado_despesas.csv";
    private static final String ARQUIVO_ZIP = "consolidado_despesas.zip";
    private static final String ARQUIVO_LOG = "processamento.log";

    // Palavras-chave para filtrar despesas com eventos/sinistros
    private static final String[] KEYWORDS = {
            "sinistros",
            "despesas com eventos",
            "eventos/sinistros"
    };

    // Estrutura para armazenar registros consolidados
    private static Map<String, RegistroDespesa> registrosConsolidados = new LinkedHashMap<>();

    // Estatísticas
    private static int totalLinhasProcessadas = 0;
    private static int linhasComDespesas = 0;
    private static int duplicatasEncontradas = 0;
    private static int valoresNegativos = 0;
    private static int valoresZerados = 0;

    // Logger
    private static PrintWriter logger;

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("  Processamento ANS - Despesas com Eventos");
        System.out.println("==============================================\n");

        try {
            // Inicializar log
            inicializarLog();

            log("=== Início do processamento ===");
            log("Data/Hora: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));

            // Buscar arquivos CSV na pasta atual
            File pastaAtual = new File(".");
            File[] arquivosCSV = pastaAtual.listFiles((dir, nome) ->
                    nome.toLowerCase().endsWith(".csv") &&
                            !nome.equals(ARQUIVO_SAIDA)
            );

            if (arquivosCSV == null || arquivosCSV.length == 0) {
                System.err.println("ERRO: Nenhum arquivo CSV encontrado!");
                System.err.println("Coloque os arquivos CSV dos trimestres na mesma pasta do Main.java");
                log("ERRO: Nenhum arquivo CSV encontrado");
                return;
            }

            System.out.println("Arquivos encontrados: " + arquivosCSV.length);
            log("Arquivos CSV encontrados: " + arquivosCSV.length);

            // Processar cada arquivo
            for (File arquivo : arquivosCSV) {
                processarArquivo(arquivo);
            }

            // Gerar CSV consolidado
            System.out.println("\nGerando arquivo consolidado...");
            gerarCSVConsolidado();

            // Compactar resultado
            System.out.println("Compactando resultado...");
            compactarCSV();

            // Exibir estatísticas
            exibirEstatisticas();

            log("=== Processamento finalizado com sucesso ===");
            System.out.println("\n==============================================");
            System.out.println("  Processamento concluído com sucesso!");
            System.out.println("==============================================");
            System.out.println("\nArquivos gerados:");
            System.out.println("  - " + ARQUIVO_SAIDA);
            System.out.println("  - " + ARQUIVO_ZIP);
            System.out.println("  - " + ARQUIVO_LOG);

        } catch (Exception e) {
            System.err.println("\nERRO: " + e.getMessage());
            e.printStackTrace();
            log("ERRO FATAL: " + e.getMessage());
        } finally {
            if (logger != null) {
                logger.close();
            }
        }
    }

    /**
     * Processa um arquivo CSV extraindo registros de despesas com eventos/sinistros
     */
    private static void processarArquivo(File arquivo) throws IOException {
        System.out.println("\nProcessando: " + arquivo.getName());
        log("Processando arquivo: " + arquivo.getName());

        // Extrair trimestre e ano do nome do arquivo
        String nomeArquivo = arquivo.getName();
        String trimestre = extrairTrimestre(nomeArquivo);
        String ano = extrairAno(nomeArquivo);

        System.out.println("  Trimestre: " + trimestre);
        System.out.println("  Ano: " + ano);
        log("  Trimestre identificado: " + trimestre + "/" + ano);

        int linhasArquivo = 0;
        int despesasArquivo = 0;

        // Ler arquivo com encoding UTF-8
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(arquivo), StandardCharsets.UTF_8))) {

            String linha;
            boolean primeiraLinha = true;

            while ((linha = br.readLine()) != null) {
                totalLinhasProcessadas++;
                linhasArquivo++;

                // Pular cabeçalho
                if (primeiraLinha) {
                    primeiraLinha = false;
                    continue;
                }

                // Verificar se contém despesas com eventos/sinistros
                if (!contemDespesaComEventos(linha)) {
                    continue;
                }

                // Processar linha
                try {
                    RegistroDespesa registro = extrairRegistro(linha, trimestre, ano);
                    if (registro != null) {
                        consolidarRegistro(registro);
                        linhasComDespesas++;
                        despesasArquivo++;
                    }
                } catch (Exception e) {
                    log("  Erro ao processar linha " + linhasArquivo + ": " + e.getMessage());
                }
            }
        }

        System.out.println("  Registros de despesas encontrados: " + despesasArquivo);
        log("  Total de linhas processadas: " + linhasArquivo);
        log("  Despesas extraídas: " + despesasArquivo);
    }

    /**
     * Verifica se a linha contém despesas com eventos/sinistros
     */
    private static boolean contemDespesaComEventos(String linha) {
        String linhaLower = linha.toLowerCase();
        for (String keyword : KEYWORDS) {
            if (linhaLower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extrai registro de uma linha CSV
     */
    private static RegistroDespesa extrairRegistro(String linha, String trimestre, String ano) {
        // Detectar delimitador (vírgula ou ponto-e-vírgula)
        char delimitador = linha.contains(";") ? ';' : ',';

        String[] partes = linha.split(String.valueOf(delimitador));

        if (partes.length < 6) {
            return null;
        }

        try {
            String data = partes[0].trim();
            String regAns = partes[1].trim();
            String cdContaContabil = partes[2].trim();
            String descricao = partes[3].trim();
            String vlSaldoFinal = partes[5].trim();

            // Limpar e converter valor
            double valor = parseValor(vlSaldoFinal);

            return new RegistroDespesa(data, regAns, cdContaContabil, descricao,
                    trimestre, ano, valor);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Consolida registro tratando duplicatas
     *
     * Trade-off: Manter registro com maior valor absoluto
     * Justificativa: Registros com valores maiores tendem a ser mais completos
     */
    private static void consolidarRegistro(RegistroDespesa registro) {
        // Analisar valor
        if (registro.valorDespesas == 0) {
            valoresZerados++;
        }
        if (registro.valorDespesas < 0) {
            valoresNegativos++;
            log("  Valor negativo: REG_ANS " + registro.regAns + " = R$ " + registro.valorDespesas);
        }

        // Chave única: REG_ANS + Ano + Trimestre + CD_CONTA_CONTABIL
        String chave = registro.regAns + "_" + registro.ano + "_" +
                registro.trimestre + "_" + registro.cdContaContabil;

        // Verificar duplicata
        if (registrosConsolidados.containsKey(chave)) {
            duplicatasEncontradas++;
            RegistroDespesa existente = registrosConsolidados.get(chave);

            // Manter registro com maior valor absoluto
            if (Math.abs(registro.valorDespesas) > Math.abs(existente.valorDespesas)) {
                registrosConsolidados.put(chave, registro);
                log("  Duplicata substituída: " + chave);
            }
        } else {
            registrosConsolidados.put(chave, registro);
        }
    }

    /**
     * Gera arquivo CSV consolidado
     */
    private static void gerarCSVConsolidado() throws IOException {
        // Ordenar registros: Ano DESC, Trimestre DESC, REG_ANS ASC
        List<RegistroDespesa> registrosOrdenados = new ArrayList<>(registrosConsolidados.values());
        Collections.sort(registrosOrdenados, new Comparator<RegistroDespesa>() {
            @Override
            public int compare(RegistroDespesa r1, RegistroDespesa r2) {
                int cmpAno = r2.ano.compareTo(r1.ano);
                if (cmpAno != 0) return cmpAno;

                int cmpTrim = r2.trimestre.compareTo(r1.trimestre);
                if (cmpTrim != 0) return cmpTrim;

                return r1.regAns.compareTo(r2.regAns);
            }
        });

        // Escrever CSV
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(ARQUIVO_SAIDA), StandardCharsets.UTF_8))) {

            // Cabeçalho (usando ponto-e-vírgula para compatibilidade com Excel)
            writer.println("DATA;REG_ANS;CD_CONTA_CONTABIL;DESCRICAO;Trimestre;Ano;ValorDespesas");

            // Dados
            for (RegistroDespesa r : registrosOrdenados) {
                writer.println(r.toCSVLine());
            }
        }

        log("CSV consolidado gerado: " + registrosOrdenados.size() + " registros");
    }

    /**
     * Compacta CSV em ZIP
     */
    private static void compactarCSV() throws IOException {
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new FileOutputStream(ARQUIVO_ZIP))) {

            File csvFile = new File(ARQUIVO_SAIDA);
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(csvFile.getName());
            zos.putNextEntry(entry);

            try (FileInputStream fis = new FileInputStream(csvFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            }

            zos.closeEntry();
        }

        log("Arquivo compactado: " + ARQUIVO_ZIP);
    }

    /**
     * Exibe estatísticas do processamento
     */
    private static void exibirEstatisticas() {
        System.out.println("\n==============================================");
        System.out.println("  ESTATÍSTICAS DO PROCESSAMENTO");
        System.out.println("==============================================");
        System.out.println("Total de linhas processadas: " + totalLinhasProcessadas);
        System.out.println("Linhas com despesas eventos/sinistros: " + linhasComDespesas);
        System.out.println("Registros únicos consolidados: " + registrosConsolidados.size());
        System.out.println("Duplicatas encontradas: " + duplicatasEncontradas);
        System.out.println("Valores zerados: " + valoresZerados);
        System.out.println("Valores negativos: " + valoresNegativos);

        System.out.println("\n==============================================");
        System.out.println("  TRATAMENTO DE INCONSISTÊNCIAS");
        System.out.println("==============================================");
        System.out.println("✓ Duplicatas: Mantido registro com maior valor absoluto");
        System.out.println("✓ Valores zerados: Mantidos (podem ser legítimos)");
        System.out.println("✓ Valores negativos: Mantidos (podem ser estornos)");
        System.out.println("✓ Encoding: UTF-8 para suportar acentos");

        log("\n=== ESTATÍSTICAS ===");
        log("Total linhas: " + totalLinhasProcessadas);
        log("Despesas: " + linhasComDespesas);
        log("Únicos: " + registrosConsolidados.size());
        log("Duplicatas: " + duplicatasEncontradas);
    }

    // Métodos auxiliares

    private static String extrairTrimestre(String nomeArquivo) {
        if (nomeArquivo.length() >= 2) {
            return nomeArquivo.substring(0, 2).toUpperCase();
        }
        return "1T";
    }

    private static String extrairAno(String nomeArquivo) {
        if (nomeArquivo.length() >= 6) {
            return nomeArquivo.substring(2, 6);
        }
        return "2025";
    }

    private static double parseValor(String valorStr) {
        if (valorStr == null || valorStr.isEmpty()) {
            return 0.0;
        }

        valorStr = valorStr.trim().replace("\"", "").replace(" ", "");

        // Formato brasileiro: 1.234,56
        if (valorStr.contains(".") && valorStr.contains(",")) {
            valorStr = valorStr.replace(".", "").replace(",", ".");
        }
        // Formato com apenas vírgula: 1234,56
        else if (valorStr.contains(",")) {
            valorStr = valorStr.replace(",", ".");
        }

        try {
            return Double.parseDouble(valorStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static void inicializarLog() throws IOException {
        logger = new PrintWriter(new FileWriter(ARQUIVO_LOG, false));
    }

    private static void log(String mensagem) {
        if (logger != null) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            logger.println("[" + timestamp + "] " + mensagem);
            logger.flush();
        }
    }

    /**
     * Classe interna para representar um registro de despesa
     */
    static class RegistroDespesa {
        String data;
        String regAns;
        String cdContaContabil;
        String descricao;
        String trimestre;
        String ano;
        double valorDespesas;

        RegistroDespesa(String data, String regAns, String cdContaContabil,
                        String descricao, String trimestre, String ano, double valorDespesas) {
            this.data = data;
            this.regAns = regAns;
            this.cdContaContabil = cdContaContabil;
            this.descricao = descricao;
            this.trimestre = trimestre;
            this.ano = ano;
            this.valorDespesas = valorDespesas;
        }

        String toCSVLine() {
            // Usando ponto-e-vírgula para compatibilidade com Excel
            return String.format("%s;%s;%s;%s;%s;%s;%.2f",
                    data, regAns, cdContaContabil,
                    escaparCSV(descricao), trimestre, ano, valorDespesas);
        }

        String escaparCSV(String valor) {
            // Para ponto-e-vírgula como delimitador, escapar se tiver ; ou " ou quebra de linha
            if (valor.contains(";") || valor.contains("\"") || valor.contains("\n")) {
                return "\"" + valor.replace("\"", "\"\"") + "\"";
            }
            return valor;
        }
    }
}