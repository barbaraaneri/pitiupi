import pitiupi.chat.GUI.LocalMemoryGame;

/**
 * Abre o Jogo da Memória local (dois tabuleiros lado a lado, sem rede) sem abrir o Pitiupi.
 * O mesmo modo existe dentro do Pitiupi: aba "Conversa na rede", botão "Jogo da Memória (local)".
 *
 * <p>Para rodar: {@code plugins\chat\teste-local\rodar.cmd}.</p>
 */
public class TesteJogoLocal {

    public static void main(String[] args) {
        LocalMemoryGame.open();
    }
}
