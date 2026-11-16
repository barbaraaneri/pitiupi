package pitiupi.memoria.net;

import pitiupi.models.Messages.Message;

/**
 * Mensagem do Jogo da Memória (duas pessoas). Viaja por TCP, direto para o IP do oponente.
 *
 * <p>O tabuleiro nunca é enviado: o convite leva só a semente ({@code seed}) e o número de
 * pares. Os dois lados embaralham do mesmo jeito e, depois disso, só trafega a jogada
 * ({@link Type#FLIP} + índice da carta).</p>
 */
public class MemoryGameMessage extends Message {

    public enum Type {
        /** Convite: leva {@code seed} e {@code pairs}. */
        INVITE,
        /** O convidado aceitou. */
        ACCEPT,
        /** O convidado recusou (ou já está em outra partida). */
        DECLINE,
        /** Jogada: o remetente virou a carta {@code index}. */
        FLIP,
        /** O remetente saiu da partida. */
        LEAVE
    }

    private Type type;
    private String gameId;
    private String senderName;
    private String senderIp;
    private long seed;
    private int pairs;
    private int index;
    private String reason;

    public MemoryGameMessage() {
    }

    public MemoryGameMessage(Type type, String gameId, String senderName, String senderIp) {
        this.type = type;
        this.gameId = gameId;
        this.senderName = senderName;
        this.senderIp = senderIp;
    }

    public static MemoryGameMessage invite(String gameId, String name, String ip, long seed, int pairs) {
        MemoryGameMessage m = new MemoryGameMessage(Type.INVITE, gameId, name, ip);
        m.seed = seed;
        m.pairs = pairs;
        return m;
    }

    public static MemoryGameMessage flip(String gameId, String name, String ip, int index) {
        MemoryGameMessage m = new MemoryGameMessage(Type.FLIP, gameId, name, ip);
        m.index = index;
        return m;
    }

    public static MemoryGameMessage decline(String gameId, String name, String ip, String reason) {
        MemoryGameMessage m = new MemoryGameMessage(Type.DECLINE, gameId, name, ip);
        m.reason = reason;
        return m;
    }

    public Type getType() {
        return type;
    }

    public String getGameId() {
        return gameId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderIp() {
        return senderIp;
    }

    public long getSeed() {
        return seed;
    }

    public int getPairs() {
        return pairs;
    }

    public int getIndex() {
        return index;
    }

    public String getReason() {
        return reason;
    }
}