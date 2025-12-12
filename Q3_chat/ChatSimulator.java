import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Questão 3: Simulador de Cliente de Chat (Simulação do Processamento do Servidor)
 * Objetivo: Demonstrar o bloqueio (Sequencial) vs. a responsividade (Thread)
 * quando um Servidor de Chat processa uma mensagem com atraso (ex: salvando no BD).
 */
public class ChatSimulator extends JFrame {

    private JTextArea logArea;
    private JButton btnSequencial;
    private JButton btnThread;
    private JTextField messageField;
    private JButton btnSend;
    
    // Contadores para Métricas e Visibilidade
    private JLabel lblStatus;
    private AtomicInteger messageCount = new AtomicInteger(0);
    private int processedCount = 0;

    public ChatSimulator() {
        // Configuração da Janela
        setTitle("TP2 - Q3: Simulador de Chat (Threads na Responsividade)");
        setSize(700, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- PAINEL SUPERIOR (Controle de Modo) ---
        JPanel panelTopo = new JPanel(new GridLayout(2, 1, 5, 5));
        
        btnSequencial = new JButton("1. Modo Servidor Sequencial (Bloqueante)");
        btnSequencial.setBackground(new Color(255, 200, 200)); 
        btnThread = new JButton("2. Modo Servidor Paralelo (Com Threads)");
        btnThread.setBackground(new Color(200, 255, 200));    

        JPanel modePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        modePanel.add(btnSequencial);
        modePanel.add(btnThread);
        
        lblStatus = new JLabel("Status: Aguardando seleção de modo...", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Monospaced", Font.BOLD, 14));
        lblStatus.setForeground(Color.BLUE);
        
        panelTopo.add(modePanel);
        panelTopo.add(lblStatus);

        add(panelTopo, BorderLayout.NORTH);

        // --- PAINEL CENTRAL (Log do Chat) ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // --- PAINEL INFERIOR (Envio de Mensagem) ---
        JPanel panelRodape = new JPanel(new BorderLayout(5, 5));
        messageField = new JTextField("Digite a mensagem aqui...");
        btnSend = new JButton("Enviar Mensagem (Simular I/O)");
        btnSend.setEnabled(false); // Inicia desabilitado

        panelRodape.add(messageField, BorderLayout.CENTER);
        panelRodape.add(btnSend, BorderLayout.EAST);
        
        add(panelRodape, BorderLayout.SOUTH);

        // --- DEFINIÇÃO DAS AÇÕES DOS BOTÕES ---

        // 1. MODO SEQUENCIAL
        btnSequencial.addActionListener(e -> setMode(false));

        // 2. MODO PARALELO
        btnThread.addActionListener(e -> setMode(true));

        // 3. AÇÃO DE ENVIO (O "Bloco de Teste")
        btnSend.addActionListener(e -> {
            String message = messageField.getText();
            if (message == null || message.trim().isEmpty()) return;
            
            messageCount.incrementAndGet();
            log("CLIENTE: Enviando mensagem " + messageCount.get() + ": \"" + message + "\"");
            
            // Verifica qual modo está ativo para chamar a função correta
            if (btnSequencial.isEnabled()) {
                // Modo Sequencial (O que vai bloquear a UI)
                simularProcessamentoSequencial();
            } else {
                // Modo Paralelo (O que vai manter a UI responsiva)
                simularProcessamentoParalelo();
            }
            
            messageField.setText(""); // Limpa o campo
        });
        
        log("Selecione o modo: Sequencial (1) ou Paralelo (2).");
    }
    
    // --- Lógica de Modo ---
    private void setMode(boolean isParallel) {
        // Configura o estado dos botões para saber qual modo está ativo
        btnSequencial.setEnabled(isParallel); // Se for paralelo (true), desabilita o sequencial.
        btnThread.setEnabled(!isParallel);
        btnSend.setEnabled(true);
        messageCount.set(0);
        processedCount = 0;
        
        String mode = isParallel ? "PARALELO (Threads ativas)" : "SEQUENCIAL (Sem Threads)";
        lblStatus.setText("Status: Modo " + mode + " - Clique em 'Enviar Mensagem'.");
        logArea.setText("");
        log("[MODO] Selecionado: " + mode);
        
        if (!isParallel) {
            log("[ALERTA] No modo SEQUENCIAL, o envio de uma mensagem demorada CONGELARÁ a interface!");
        } else {
            log("[INFO] No modo PARALELO, a interface deve permanecer ativa durante o processamento.");
        }
    }

    // --- Lógica de Simulação de Processamento do Servidor ---

    /**
     * Simulação Sequencial (Bloqueia a EDT)
     */
    private void simularProcessamentoSequencial() {
        log("SERVER (SEQUENCIAL): Iniciando processamento demorado...");
        long inicio = System.currentTimeMillis();
        
        // A tarefa pesada é executada DIRETAMENTE na Event Dispatch Thread (EDT)
        processarMensagemPesada(); 
        
        long fim = System.currentTimeMillis();
        double tempoTotal = (fim - inicio) / 1000.0;
        processedCount++;
        log("SERVER (SEQUENCIAL): Mensagem #" + processedCount + " PROCESSADA em " + tempoTotal + "s.");
        log("[IMPORTANTE] Tente clicar no botão 'Enviar Mensagem' antes que este log apareça novamente!");
    }

    /**
     * Simulação Paralela (Mantém a EDT livre)
     */
    private void simularProcessamentoParalelo() {
        final int currentMessageId = messageCount.get();
        log("SERVER (PARALELO): Mensagem #" + currentMessageId + " enviada para nova Thread de processamento.");
        
        // Cria uma nova Thread para o trabalho pesado
        new Thread(() -> {
            long inicio = System.currentTimeMillis();
            
            // O trabalho pesado ocorre em segundo plano
            processarMensagemPesada();
            
            long fim = System.currentTimeMillis();
            double tempoTotal = (fim - inicio) / 1000.0;
            
            // Atualiza a UI (Log) de volta na EDT
            SwingUtilities.invokeLater(() -> {
                processedCount++;
                log("SERVER (PARALELO): Mensagem #" + currentMessageId + " PROCESSADA na thread em " + tempoTotal + "s.");
            });
        }).start(); // Inicia a thread
    }

    /**
     * Simula uma operação demorada no servidor (ex: I/O de Banco de Dados, API Externa).
     * O tempo total é fixo em 5 segundos.
     */
    private void processarMensagemPesada() {
        try {
            // Simula o atraso de 5 segundos
            Thread.sleep(5000); 
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    // --- Métodos Auxiliares ---

    // Método seguro para adicionar texto ao log vindo de qualquer thread
    private void log(String texto) {
        String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());
        
        // Garante que a atualização da área de texto seja feita na EDT
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + hora + "] " + texto + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatSimulator().setVisible(true));
    }
}