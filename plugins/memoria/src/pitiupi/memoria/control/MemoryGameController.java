package pitiupi.memoria.control;

import pitiupi.memoria.game.MemoryGameModel;
import pitiupi.memoria.game.MemoryGameModel.FlipResult;
import pitiupi.memoria.net.MemoryGameMessage;
import pitiupi.presentation.shell.PeerIpv4;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Protocolo e ciclo de vida do Jogo da Memória entre duas pessoas.
 *
 * <pre>
 *   convidou                          convidado
 *   INVITE (seed, pairs)  ───────────▶
 *                         ◀─────────── ACCEPT   (ou DECLINE)
 *   FLIP (índice)         ───────────▶            quem está na vez joga;
 *                         ◀─────────── FLIP       os dois modelos aplicam a mesma jogada
 *   LEAVE                 ◀──────────▶            fechou a janela
 * </pre>
 *
 * <p>Todos os métodos públicos devem ser chamados na thread do Swing.
 * O envio é feito por {@link Transport}, que cuida de não travar a tela.</p>
 */
public class MemoryGameController {

    public static final int DEFAULT_PAIRS = 8;
    public static final int MAX_PAIRS = 18;
    public static final int MISMATCH_DELAY_MS = 1000;

    /** Envia a mensagem para o IP (em segundo plano, preservando a ordem). */
    public interface Transport {
        void send(MemoryGameMessage message, String peerIp);
    }

    private final Supplier<String> localName;
    private final Supplier<String> localIp;
    private final Transport transport;
    private final MemoryGameUi ui;
    private final SecureRandom random = new SecureRandom();

    private MemoryGameSession session;
    private boolean confirmingInvite;

    public MemoryGameController(Supplier<String> localName, Supplier<String> localIp,
                                Transport transport, MemoryGameUi ui) {
        this.localName = localName;
        this.localIp = localIp;
        this.transport = transport;
        this.ui = ui;
    }

    public MemoryGameSession getSession() {
        return session;
    }

    // ---------------------------------------------------------------- lado de quem convida

    public void invite(String peerIp, String peerName) {
        String ip = PeerIpv4.normalize(peerIp);
        if (ip.isBlank()) {
            ui.notice("Não encontrei o IP desta pessoa.");
            return;
        }
        if (isBusy()) {
            ui.notice("Você já está em uma partida. Termine ou feche a janela do jogo antes de convidar de novo.");
            return;
        }
        discardOldSession();
        String gameId = UUID.randomUUID().toString();
        long seed = random.nextLong();
        MemoryGameSession s = new MemoryGameSession(gameId, ip, peerName,
                new MemoryGameModel(seed, DEFAULT_PAIRS), 0, MemoryGameSession.Phase.WAITING);
        open(s);
        transport.send(MemoryGameMessage.invite(gameId, localName.get(), localIp.get(), seed, DEFAULT_PAIRS), ip);
    }

    // ---------------------------------------------------------------- mensagens recebidas

    public void onMessage(MemoryGameMessage m) {
        if (m == null || m.getType() == null || m.getGameId() == null) {
            return;
        }
        if (m.getType() == MemoryGameMessage.Type.INVITE) {
            onInvite(m);
            return;
        }
        MemoryGameSession s = session;
        if (s == null || !s.getGameId().equals(m.getGameId())) {
            if (m.getType() == MemoryGameMessage.Type.ACCEPT) {
                // Eu já tinha desistido deste convite: avisa para o outro lado não ficar esperando.
                String ip = PeerIpv4.normalize(m.getSenderIp());
                if (!ip.isBlank()) {
                    transport.send(simple(MemoryGameMessage.Type.LEAVE, m.getGameId()), ip);
                }
            }
            return;
        }
        switch (m.getType()) {
            case ACCEPT -> onAccept(s);
            case DECLINE -> onDecline(s, m);
            case FLIP -> onRemoteFlip(s, m.getIndex());
            case LEAVE -> onLeave(s);
            default -> {
            }
        }
    }

    private void onInvite(MemoryGameMessage m) {
        String ip = PeerIpv4.normalize(m.getSenderIp());
        if (ip.isBlank()) {
            return;
        }
        if (m.getPairs() < 2 || m.getPairs() > MAX_PAIRS) {
            transport.send(MemoryGameMessage.decline(m.getGameId(), localName.get(), localIp.get(), "invalid"), ip);
            return;
        }
        if (isBusy() || confirmingInvite) {
            transport.send(MemoryGameMessage.decline(m.getGameId(), localName.get(), localIp.get(), "busy"), ip);
            return;
        }
        confirmingInvite = true;
        boolean accepted;
        try {
            accepted = ui.confirmInvite(m.getSenderName(), ip);
        } finally {
            confirmingInvite = false;
        }
        if (!accepted) {
            transport.send(MemoryGameMessage.decline(m.getGameId(), localName.get(), localIp.get(), "declined"), ip);
            return;
        }
        discardOldSession();
        MemoryGameSession s = new MemoryGameSession(m.getGameId(), ip, m.getSenderName(),
                new MemoryGameModel(m.getSeed(), m.getPairs()), 1, MemoryGameSession.Phase.PLAYING);
        open(s);
        transport.send(simple(MemoryGameMessage.Type.ACCEPT, m.getGameId()), ip);
    }

    private void onAccept(MemoryGameSession s) {
        if (s.getPhase() == MemoryGameSession.Phase.WAITING) {
            s.setPhase(MemoryGameSession.Phase.PLAYING);
            s.refreshView();
        }
    }

    private void onDecline(MemoryGameSession s, MemoryGameMessage m) {
        if (s.getPhase() != MemoryGameSession.Phase.WAITING) {
            return;
        }
        String reason = "busy".equals(m.getReason())
                ? s.getPeerName() + " já está em outra partida."
                : s.getPeerName() + " não aceitou o convite.";
        s.abort(reason);
        s.refreshView();
    }

    private void onLeave(MemoryGameSession s) {
        if (s.isActive()) {
            s.abort(s.getPeerName() + " saiu da partida.");
            s.refreshView();
        }
    }

    private void onRemoteFlip(MemoryGameSession s, int index) {
        if (s.getPhase() != MemoryGameSession.Phase.PLAYING) {
            return;
        }
        FlipResult result = s.getModel().flip(s.getRemotePlayer(), index);
        if (result != FlipResult.INVALID) {
            afterFlip(s, result);
        }
    }

    // ---------------------------------------------------------------- ações locais

    /** A pessoa clicou na carta {@code index}. */
    public void onLocalFlip(int index) {
        MemoryGameSession s = session;
        if (s == null || !s.canLocalFlip()) {
            return;
        }
        FlipResult result = s.getModel().flip(s.getLocalPlayer(), index);
        if (result == FlipResult.INVALID) {
            return;
        }
        transport.send(MemoryGameMessage.flip(s.getGameId(), localName.get(), localIp.get(), index), s.getPeerIp());
        afterFlip(s, result);
    }

    /** A janela do jogo foi fechada. */
    public void onViewClosed(MemoryGameSession s) {
        if (session != s) {
            return;
        }
        if (s.isActive()) {
            transport.send(simple(MemoryGameMessage.Type.LEAVE, s.getGameId()), s.getPeerIp());
        }
        session = null;
    }

    /** O {@link Transport} não conseguiu entregar {@code m}. */
    public void onSendFailed(MemoryGameMessage m) {
        MemoryGameSession s = session;
        if (s == null || m == null || !s.getGameId().equals(m.getGameId())) {
            return;
        }
        if (m.getType() == MemoryGameMessage.Type.LEAVE || !s.isActive()) {
            return;
        }
        s.abort("Não foi possível falar com " + s.getPeerName() + ". Confira a rede e convide de novo.");
        s.refreshView();
    }

    // ---------------------------------------------------------------- internos

    private void afterFlip(MemoryGameSession s, FlipResult result) {
        MemoryGameModel model = s.getModel();
        if (result == FlipResult.MISMATCH) {
            int stamp = model.getMismatchCount();
            ui.schedule(MISMATCH_DELAY_MS, () -> {
                if (session == s && model.getMismatchCount() == stamp) {
                    model.hidePending();
                    s.refreshView();
                }
            });
        }
        if (model.isGameOver()) {
            s.setPhase(MemoryGameSession.Phase.FINISHED);
        }
        s.refreshView();
    }

    private boolean isBusy() {
        return session != null && session.isActive();
    }

    /** Fecha a janela de uma partida que já acabou, antes de começar outra. */
    private void discardOldSession() {
        MemoryGameSession old = session;
        if (old == null) {
            return;
        }
        session = null;
        if (old.getView() != null) {
            old.getView().close();
        }
    }

    private void open(MemoryGameSession s) {
        session = s;
        s.setView(ui.openView(this, s));
        s.refreshView();
    }

    private MemoryGameMessage simple(MemoryGameMessage.Type type, String gameId) {
        return new MemoryGameMessage(type, gameId, localName.get(), localIp.get());
    }
}