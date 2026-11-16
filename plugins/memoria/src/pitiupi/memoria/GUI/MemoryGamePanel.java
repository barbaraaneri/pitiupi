package pitiupi.memoria.GUI;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.function.Consumer;

/** Aba do plugin: jogar neste computador ou convidar alguém pelo IP. */
public class MemoryGamePanel extends JPanel {

    public MemoryGamePanel(Runnable playLocal, Consumer<String> inviteByIp) {
        super(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("Jogo da Memória");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        content.add(left(title));
        content.add(Box.createVerticalStrut(6));
        content.add(left(new JLabel("Duas pessoas. Ache os pares; acertou, joga de novo; errou, passa a vez.")));
        content.add(Box.createVerticalStrut(24));

        content.add(left(section("Jogar com alguém na rede")));
        content.add(Box.createVerticalStrut(6));
        content.add(left(new JLabel("Clique no nome da pessoa na lista \"Usuários nesta rede\" e escolha \"Jogar Jogo da Memória\".")));
        content.add(Box.createVerticalStrut(8));
        JTextField ipField = new JTextField(16);
        JButton invite = new JButton("Convidar");
        invite.setFocusPainted(false);
        Runnable doInvite = () -> {
            String ip = ipField.getText().trim();
            if (!ip.isEmpty()) {
                inviteByIp.accept(ip);
            }
        };
        invite.addActionListener(e -> doInvite.run());
        ipField.addActionListener(e -> doInvite.run());
        JPanel ipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        ipRow.add(new JLabel("Ou convide pelo IP:"));
        ipRow.add(ipField);
        ipRow.add(invite);
        content.add(left(ipRow));
        content.add(Box.createVerticalStrut(24));

        content.add(left(section("Jogar neste computador")));
        content.add(Box.createVerticalStrut(6));
        content.add(left(new JLabel("Abre dois tabuleiros lado a lado, sem rede. Jogador A (esquerda) começa.")));
        content.add(Box.createVerticalStrut(8));
        JButton local = new JButton("Jogar neste computador");
        local.setFocusPainted(false);
        local.addActionListener(e -> playLocal.run());
        content.add(left(local));

        add(content, BorderLayout.NORTH);
    }

    private static JLabel section(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        return label;
    }

    private static Component left(java.awt.Component c) {
        if (c instanceof javax.swing.JComponent j) {
            j.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        return c;
    }
}
