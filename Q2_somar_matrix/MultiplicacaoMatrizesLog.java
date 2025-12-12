import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Scanner;

/**
 * Questão 2: Multiplicação de Matrizes (Sequencial vs Paralelo)
 * Disciplina: Sistemas Operacionais - UFAM
 * * Objetivo: Comparar o desempenho da multiplicação de matrizes utilizando
 * uma abordagem tradicional (Single Thread) versus uma abordagem paralela (Multi-thread).
 * O resultado é salvo em um arquivo de log para análise de Speedup.
 */
public class MultiplicacaoMatrizesLog {

    // Define a dimensão das matrizes (NxN). Valor inicial: 1000
    private static final int DIMENSAO = 1000;

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);

        // --- 1. Identificação do Ambiente ---
        System.out.println("=== Configuração: Multiplicação de Matrizes ===");
        System.out.println("Qual computador você está usando?");
        System.out.println("1 - Notebook Local");
        System.out.println("2 - PC Lab");
        System.out.print("Opção: ");

        // Tratamento de entrada para a opção do PC
        int opcaoPc = 0;
        if (scanner.hasNextInt()) {
            opcaoPc = scanner.nextInt();
        } else {
            System.err.println("Erro: Entrada inválida. Usando Notebook Local como padrão.");
            scanner.next(); // Limpa a entrada inválida
            opcaoPc = 1;
        }

        String nomePc = (opcaoPc == 1) ? "Notebook Local" : "PC Lab";
        if (opcaoPc != 1 && opcaoPc != 2) nomePc = "Outro/Desconhecido";

        // Geração de dados aleatórios
        System.out.println("\n[INFO] Iniciando geração das matrizes de dimensão: " + DIMENSAO + "x" + DIMENSAO + "...");
        long[][] matA = gerarMatriz(DIMENSAO);
        long[][] matB = gerarMatriz(DIMENSAO);
        System.out.println("[INFO] Geração concluída.");

        System.out.print("Digite o número de threads para o teste paralelo: ");

        // Tratamento de entrada para o número de threads
        int numThreads = 0;
        if (scanner.hasNextInt()) {
            numThreads = scanner.nextInt();
        } else {
            System.err.println("Erro: Entrada inválida. Usando 4 threads como padrão.");
            numThreads = 4;
        }

        // --- 2. Execução Sequencial ---
        System.out.println("\n>>> FASE 1: Executando Multiplicação Sequencial...");
        long inicioSeq = System.nanoTime();
        long[][] resSeq = multiplicarSequencial(matA, matB);
        long fimSeq = System.nanoTime();

        double tempoSeq = (fimSeq - inicioSeq) / 1e9;
        System.out.printf("[RESULTADO] Tempo Sequencial: %.4f s\n", tempoSeq);

        // --- 3. Execução Paralela ---
        System.out.println(">>> FASE 2: Executando Multiplicação Paralela (" + numThreads + " threads)...");
        long inicioPar = System.nanoTime();
        long[][] resPar = multiplicarParalelo(matA, matB, numThreads);
        long fimPar = System.nanoTime();

        double tempoPar = (fimPar - inicioPar) / 1e9;
        System.out.printf("[RESULTADO] Tempo Paralelo:   %.4f s\n", tempoPar);

        // --- 4. Análise de Resultados ---
        double speedup = tempoSeq / tempoPar;
        System.out.println("\n=== Avaliação de Desempenho ===");
        System.out.printf("Speedup (Sp = Ts/Tp): %.2f x\n", speedup);

        // Validação de Corretude
        boolean validado = compararMatrizes(resSeq, resPar);
        if (validado) {
            System.out.println("Validação Matemática: OK (Resultados idênticos)");
        } else {
            System.err.println("Validação Matemática: ERRO (Diferenças encontradas! Verifique o paralelismo.)");
        }

        // --- CHECKPOINT ADICIONAL PARA DIAGNÓSTICO ---
        System.out.println("--- CHECKPOINT: Tentando salvar resultados no arquivo TXT ---");

        // --- 5. Persistência de Dados ---
        salvarLog(nomePc, DIMENSAO, numThreads, tempoSeq, tempoPar, speedup);

        scanner.close();
    }

    // Algoritmo Sequencial Clássico O(N^3)
    public static long[][] multiplicarSequencial(long[][] A, long[][] B) {
        long[][] C = new long[DIMENSAO][DIMENSAO];
        for (int i = 0; i < DIMENSAO; i++) {
            for (int j = 0; j < DIMENSAO; j++) {
                for (int k = 0; k < DIMENSAO; k++) {
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return C;
    }

    // Gerencia a execução paralela (Decomposição por Linhas)
    public static long[][] multiplicarParalelo(long[][] A, long[][] B, int numThreads) throws InterruptedException {
        long[][] C = new long[DIMENSAO][DIMENSAO];
        CalculadoraMatrizThread[] threads = new CalculadoraMatrizThread[numThreads];

        int linhasPorThread = DIMENSAO / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int linhaInicio = i * linhasPorThread;
            int linhaFim = (i == numThreads - 1) ? DIMENSAO : (linhaInicio + linhasPorThread);

            System.out.printf("  [Thread %d] Responsável pelas linhas %d até %d\n", i, linhaInicio, linhaFim - 1);

            threads[i] = new CalculadoraMatrizThread(A, B, C, linhaInicio, linhaFim);
            threads[i].start();
        }

        System.out.println("[INFO] Todas as threads disparadas. Aguardando conclusão (join())...");
        for (int i = 0; i < numThreads; i++) {
            threads[i].join();
        }
        System.out.println("[INFO] Agregação de resultados concluída.");

        return C;
    }

    /**
     * Classe Trabalhadora (Thread) - Renomeada para CalculadoraMatrizThread
     * Calcula um subconjunto de linhas da matriz resultante C.
     */
    static class CalculadoraMatrizThread extends Thread {
        private final long[][] A, B, C;
        private final int linhaInicio, linhaFim;

        public CalculadoraMatrizThread(long[][] A, long[][] B, long[][] C, int linhaInicio, int linhaFim) {
            this.A = A;
            this.B = B;
            this.C = C;
            this.linhaInicio = linhaInicio;
            this.linhaFim = linhaFim;
        }

        @Override
        public void run() {
            // Executa o cálculo APENAS nas linhas designadas
            for (int i = linhaInicio; i < linhaFim; i++) {
                for (int j = 0; j < DIMENSAO; j++) {
                    long soma = 0;
                    for (int k = 0; k < DIMENSAO; k++) {
                        soma += A[i][k] * B[k][j];
                    }
                    C[i][j] = soma;
                }
            }
        }
    }

    // --- Métodos Auxiliares ---

    private static void salvarLog(String pc, int tamanho, int threads, double tSeq, double tPar, double sp) {
        try (FileWriter fw = new FileWriter("resultados_matriz.txt", true);
             PrintWriter pw = new PrintWriter(fw)) {

            pw.printf("PC: %s | Matriz: %dx%d | Threads: %d | T.Seq: %.4fs | T.Par: %.4fs | Sp: %.2f%n",
                      pc, tamanho, tamanho, threads, tSeq, tPar, sp);

            System.out.println("[!] Log de Matrizes salvo em 'resultados_matriz.txt'");
        } catch (IOException e) {
            System.err.println("Erro ao salvar log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static long[][] gerarMatriz(int n) {
        Random rand = new Random();
        long[][] m = new long[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // Valores de 0 a 99 para evitar overflow em cálculos muito grandes
                m[i][j] = rand.nextInt(100);
            }
        }
        return m;
    }

    private static boolean compararMatrizes(long[][] m1, long[][] m2) {
        for (int i = 0; i < DIMENSAO; i++) {
            for (int j = 0; j < DIMENSAO; j++) {
                if (m1[i][j] != m2[i][j]) return false;
            }
        }
        return true;
    }
}