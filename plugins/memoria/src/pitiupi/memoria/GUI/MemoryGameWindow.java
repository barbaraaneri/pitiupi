package pitiupi.memoria.GUI;

import pitiupi.memoria.control.MemoryGameController;
import pitiupi.memoria.control.MemoryGameSession;
import pitiupi.memoria.control.MemoryGameView;
import pitiupi.memoria.game.MemoryGameModel;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Janela do Jogo da Memória: placar, de quem é a vez e o tabuleiro. */
public class MemoryGameWindow extends JFrame implements MemoryGameView {

    private static final String[] SYMBOLS = {
            "★", "♥", "♦", "♣", "♠", "♪", "☀", "☂", "☎",
            "✈", "✿", "❄", "☘", "⚓", "♛", "♞", "✂", "☺"
    };
    private static final Color HIDDEN_BG = new Color(0x3B5B8C);
    private static final Color MINE = new Color(0x2E9E5B);
    private static final Color THEIRS = new Color(0xE08A1E);

    private final MemoryGameController controller;
    private final MemoryGameSession session;
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel scoreLabel = new JLabel(" ", SwingConstants.CENTER);
    private final CardView[] cards;

    public MemoryGameWindow(MemoryGameController controller, MemoryGameSession session) {
        super("Jogo da Memória — você × " + session.getPeerName());
        this.controller = controller;
        this.session = session;

        MemoryGameModel model = session.getModel();
        int total = model.getCardCount();
        int cols = (int) Math.ceil(Math.sqrt(total));
        int rows = (int) Math.ceil(total / (double) cols);

        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 18f));
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.PLAIN, 14f));
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
        header.add(statusLabel);
        header.add(scoreLabel);

        JPanel board = new JPanel(new GridLayout(rows, cols, 8, 8));
        board.setBorder(BorderFactory.createEmptyBorder(6, 12, 12, 12));
        cards = new CardView[total];
        for (int i = 0; i < total; i++) {
            cards[i] = new CardView(i);
            board.add(cards[i]);
        }

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                controller.onViewClosed(session);
            }
        });
        pack();
        setLocationRelativeTo(null);
        refresh();
        setVisible(true);
    }

    @Override
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }
        MemoryGameModel model = session.getModel();
        statusLabel.setText(statusText());
        scoreLabel.setText("Você: " + model.getScore(session.getLocalPlayer())
                + "   •   " + session.getPeerName() + ": " + model.getScore(session.getRemotePlayer())
                + "   •   Pares: " + (model.getScore(0) + model.getScore(1)) + "/" + model.getPairs());
        boolean clickable = session.canLocalFlip();
        for (CardView card : cards) {
            card.setCursor(clickable && model.getState(card.index) == MemoryGameModel.CardState.HIDDEN
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
            card.repaint();
        }
    }

    @Override
    public void close() {
        dispose();
    }

    private String statusText() {
        MemoryGameModel model = session.getModel();
        switch (session.getPhase()) {
            case WAITING:
                return "Aguardando " + session.getPeerName() + " aceitar o convite…";
            case PLAYING:
                return session.isMyTurn() ? "Sua vez!" : "Vez de " + session.getPeerName();
            case FINISHED:
                int winner = model.getWinner();
                if (winner < 0) {
                    return "Fim de jogo: empate!";
                }
                return winner == session.getLocalPlayer()
                        ? "Fim de jogo: você venceu!"
                        : "Fim de jogo: " + session.getPeerName() + " venceu.";
            default:
                return session.getEndReason();
        }
    }

    private static Color pairColor(int value) {
        return Color.getHSBColor((value * 0.17f) % 1f, 0.45f, 0.97f);
    }

    /** Uma carta, desenhada à mão para não depender do visual (look and feel) do sistema. */
    private final class CardView extends JComponent {

        private final int index;

        CardView(int index) {
            this.index = index;
            setPreferredSize(new Dimension(88, 88));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    controller.onLocalFlip(CardView.this.index);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                MemoryGameModel model = session.getModel();
                MemoryGameModel.CardState state = model.getState(index);
                int w = getWidth() - 1;
                int h = getHeight() - 1;

                String text;
                Color bg;
                Color fg;
                if (state == MemoryGameModel.CardState.HIDDEN) {
                    text = "?";
                    bg = HIDDEN_BG;
                    fg = Color.WHITE;
                } else {
                    int value = model.getValue(index);
                    text = SYMBOLS[value % SYMBOLS.length];
                    bg = pairColor(value);
                    fg = new Color(0x222222);
                }
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, 16, 16);

                if (state == MemoryGameModel.CardState.MATCHED) {
                    g2.setColor(model.getMatchedBy(index) == session.getLocalPlayer() ? MINE : THEIRS);
                    g2.setStroke(new BasicStroke(4f));
                    g2.drawRoundRect(2, 2, w - 4, h - 4, 14, 14);
                }

                g2.setColor(fg);
                g2.setFont(new Font(Font.DIALOG, Font.BOLD, 38));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text,
                        (getWidth() - fm.stringWidth(text)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            } finally {
                g2.dispose();
            }
        }
    }
}