import pitiupi.memoria.GUI.LocalMemoryGame;

/**
 * Abre o Jogo da Memória local (dois tabuleiros lado a lado, sem rede) sem abrir o Pitiupi.
 * O mesmo modo existe dentro do Pitiupi: aba Plugins → "Jogo da Memória" → "Jogar neste computador".
 *
 * <p>Para rodar: {@code plugins\memoria\teste-local\rodar.cmd}.</p>
 */
public class TesteJogoLocal {

    public static void main(String[] args) {
        LocalMemoryGame.open();
    }
}
