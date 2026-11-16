package pitiupi.chat.control;

/** O que o controlador precisa da janela do jogo. Tudo é chamado na thread do Swing. */
public interface MemoryGameView {

    /** Redesenha a janela a partir do estado atual da sessão. */
    void refresh();

    /** Fecha a janela (sem enviar nada para o oponente). */
    void close();
}