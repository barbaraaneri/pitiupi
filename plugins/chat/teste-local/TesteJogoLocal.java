import pitiupi.chat.GUI.SwingMemoryGameUi;
import pitiupi.chat.control.MemoryGameController;
import pitiupi.chat.control.MemoryGameSession;
import pitiupi.chat.control.MemoryGameView;
import pitiupi.chat.net.MemoryGameMessage;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.function.Supplier;

/**
 * Teste local do Jogo da Memória: dois jogadores no mesmo computador, sem rede.
 *
 * <p>Usa as classes reais do plugin (controlador, modelo, janela e mensagem). Cada mensagem
 * é serializada e desserializada, como no TCP, e entregue ao controlador do outro jogador.
 * As duas janelas abrem lado a lado: Jogador A à esquerda (começa), Jogador B à direita.</p>
 *
 * <p>Para rodar: {@code plugins\chat\teste-local\rodar.cmd} (veja o README desta pasta).</p>
 */
public class TesteJogoLocal {

    private static MemoryGameController jogadorA;
    private static MemoryGameController jogadorB;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            jogadorA = new MemoryGameController(() -> "Jogador A", () -> "127.0.0.1",
                    (m, ip) -> entregar(m, () -> jogadorB), new UiLadoALado("Jogador A", true));
            jogadorB = new MemoryGameController(() -> "Jogador B", () -> "127.0.0.2",
                    (m, ip) -> entregar(m, () -> jogadorA), new UiLadoALado("Jogador B", false));
            jogadorA.invite("127.0.0.2", "Jogador B");
        });
    }

    /** Simula a rede: vira bytes, volta a ser objeto e chega ao outro lado na thread do Swing. */
    private static void entregar(MemoryGameMessage m, Supplier<MemoryGameController> destino) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(m.toByteArray()))) {
            MemoryGameMessage copia = (MemoryGameMessage) in.readObject();
            SwingUtilities.invokeLater(() -> destino.get().onMessage(copia));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Igual à interface Swing do plugin, mas põe o nome do jogador no título e separa as janelas. */
    private static final class UiLadoALado extends SwingMemoryGameUi {

        private final String nome;
        private final boolean esquerda;

        UiLadoALado(String nome, boolean esquerda) {
            this.nome = nome;
            this.esquerda = esquerda;
        }

        @Override
        public boolean confirmInvite(String peerName, String peerIp) {
            return true; // no teste local o Jogador B sempre aceita
        }

        @Override
        public MemoryGameView openView(MemoryGameController controller, MemoryGameSession session) {
            MemoryGameView view = super.openView(controller, session);
            if (view instanceof JFrame janela) {
                janela.setTitle(nome + " — " + janela.getTitle());
                Rectangle tela = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                int x = esquerda
                        ? tela.x + tela.width / 2 - janela.getWidth() - 10
                        : tela.x + tela.width / 2 + 10;
                janela.setLocation(Math.max(tela.x, x), tela.y + Math.max(0, (tela.height - janela.getHeight()) / 2));
            }
            return view;
        }
    }
}
