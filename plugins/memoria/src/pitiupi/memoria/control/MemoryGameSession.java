package pitiupi.memoria.control;

import pitiupi.memoria.game.MemoryGameModel;

/** Uma partida em andamento (ou terminada) entre este computador e um peer. */
public final class MemoryGameSession {

    public enum Phase {
        /** Convite enviado, esperando a resposta. */
        WAITING,
        PLAYING,
        /** Todos os pares foram encontrados. */
        FINISHED,
        /** Terminou antes da hora (recusa, saída do oponente, falha de rede). */
        ABORTED
    }

    private final String gameId;
    private final String peerIp;
    private final String peerName;
    private final MemoryGameModel model;
    private final int localPlayer;

    private Phase phase;
    private String endReason = "";
    private MemoryGameView view;

    MemoryGameSession(String gameId, String peerIp, String peerName,
                      MemoryGameModel model, int localPlayer, Phase phase) {
        this.gameId = gameId;
        this.peerIp = peerIp;
        this.peerName = peerName == null || peerName.isBlank() ? peerIp : peerName;
        this.model = model;
        this.localPlayer = localPlayer;
        this.phase = phase;
    }

    public String getGameId() {
        return gameId;
    }

    public String getPeerIp() {
        return peerIp;
    }

    public String getPeerName() {
        return peerName;
    }

    public MemoryGameModel getModel() {
        return model;
    }

    /** 0 se este computador convidou (começa), 1 se foi convidado. */
    public int getLocalPlayer() {
        return localPlayer;
    }

    public int getRemotePlayer() {
        return 1 - localPlayer;
    }

    public Phase getPhase() {
        return phase;
    }

    public String getEndReason() {
        return endReason;
    }

    public boolean isActive() {
        return phase == Phase.WAITING || phase == Phase.PLAYING;
    }

    public boolean isMyTurn() {
        return phase == Phase.PLAYING && model.getCurrentPlayer() == localPlayer;
    }

    /** Pode virar carta agora? (minha vez e sem um erro ainda à mostra) */
    public boolean canLocalFlip() {
        return isMyTurn() && !model.hasPendingHide();
    }

    void setPhase(Phase phase) {
        this.phase = phase;
    }

    void abort(String reason) {
        this.phase = Phase.ABORTED;
        this.endReason = reason;
    }

    MemoryGameView getView() {
        return view;
    }

    void setView(MemoryGameView view) {
        this.view = view;
    }

    void refreshView() {
        if (view != null) {
            view.refresh();
        }
    }
}