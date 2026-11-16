package pitiupi.memoria.control;

/** Serviços de interface que o controlador usa (diálogos, janela e temporizador). */
public interface MemoryGameUi {

    /** Pergunta se a pessoa aceita o convite. Devolve {@code true} se aceitou. */
    boolean confirmInvite(String peerName, String peerIp);

    /** Aviso simples para a pessoa. */
    void notice(String text);

    /** Abre a janela do jogo para esta sessão. */
    MemoryGameView openView(MemoryGameController controller, MemoryGameSession session);

    /** Executa {@code task} uma vez, depois de {@code millis} ms, na thread do Swing. */
    void schedule(int millis, Runnable task);
}