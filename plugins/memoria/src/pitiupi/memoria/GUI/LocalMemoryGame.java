package pitiupi.memoria.GUI;

import pitiupi.memoria.control.MemoryGameController;
import pitiupi.memoria.control.MemoryGameSession;
import pitiupi.memoria.control.MemoryGameView;
import pitiupi.memoria.net.MemoryGameMessage;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

/**
 * Jogo da Memória no mesmo computador: dois tabuleiros lado a lado, sem rede.
 *
 * <p>Usa as mesmas classes da partida em rede (controlador, modelo, janela e mensagem). Em vez do
 * TCP, cada mensagem é serializada, desserializada e entregue ao controlador do outro jogador.
 * Jogador A fica à esquerda e começa; Jogador B fica à direita.</p>
 */
public final class LocalMemoryGame {

    private MemoryGameController playerA;
    private MemoryGameController playerB;

    private LocalMemoryGame() {
    }

    /** Abre uma partida local nova. Pode ser chamado de qualquer thread. */
    public static void open() {
        SwingUtilities.invokeLater(() -> new LocalMemoryGame().start());
    }

    private void start() {
        playerA = new MemoryGameController(() -> "Jogador A", () -> "127.0.0.1",
                (m, ip) -> deliver(m, false), new SideBySideUi("Jogador A", true));
        playerB = new MemoryGameController(() -> "Jogador B", () -> "127.0.0.2",
                (m, ip) -> deliver(m, true), new SideBySideUi("Jogador B", false));
        playerA.invite("127.0.0.2", "Jogador B");
    }

    /** Simula a rede: a mensagem vira bytes, volta a ser objeto e chega ao outro lado. */
    private void deliver(MemoryGameMessage message, boolean toA) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(message.toByteArray()))) {
            MemoryGameMessage copy = (MemoryGameMessage) in.readObject();
            SwingUtilities.invokeLater(() -> (toA ? playerA : playerB).onMessage(copy));
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    /** Interface Swing normal, mas o convite é aceito sozinho e as janelas ficam lado a lado. */
    private static final class SideBySideUi extends SwingMemoryGameUi {

        private final String name;
        private final boolean left;

        SideBySideUi(String name, boolean left) {
            this.name = name;
            this.left = left;
        }

        @Override
        public boolean confirmInvite(String peerName, String peerIp) {
            return true;
        }

        @Override
        public MemoryGameView openView(MemoryGameController controller, MemoryGameSession session) {
            MemoryGameView view = super.openView(controller, session);
            if (view instanceof JFrame window) {
                window.setTitle(name + " — " + window.getTitle());
                Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                int x = left
                        ? screen.x + screen.width / 2 - window.getWidth() - 10
                        : screen.x + screen.width / 2 + 10;
                int y = screen.y + Math.max(0, (screen.height - window.getHeight()) / 2);
                window.setLocation(Math.max(screen.x, x), y);
            }
            return view;
        }
    }
}
