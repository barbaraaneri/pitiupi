package pitiupi.chat.GUI;

import pitiupi.chat.control.MemoryGameController;
import pitiupi.chat.control.MemoryGameSession;
import pitiupi.chat.control.MemoryGameUi;
import pitiupi.chat.control.MemoryGameView;

import javax.swing.JOptionPane;
import javax.swing.Timer;

/** Diálogos, janela e temporizador do Jogo da Memória em Swing. */
public class SwingMemoryGameUi implements MemoryGameUi {

    private static final String TITLE = "Jogo da Memória";

    @Override
    public boolean confirmInvite(String peerName, String peerIp) {
        String who = peerName == null || peerName.isBlank() ? peerIp : peerName + " (" + peerIp + ")";
        int answer = JOptionPane.showConfirmDialog(
                null,
                who + " convidou você para o Jogo da Memória.\nAceitar?",
                "Convite de jogo",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        return answer == JOptionPane.YES_OPTION;
    }

    @Override
    public void notice(String text) {
        JOptionPane.showMessageDialog(null, text, TITLE, JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public MemoryGameView openView(MemoryGameController controller, MemoryGameSession session) {
        return new MemoryGameWindow(controller, session);
    }

    @Override
    public void schedule(int millis, Runnable task) {
        Timer timer = new Timer(millis, e -> task.run());
        timer.setRepeats(false);
        timer.start();
    }
}