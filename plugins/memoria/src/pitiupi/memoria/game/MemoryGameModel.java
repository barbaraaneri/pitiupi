package pitiupi.memoria.game;

import java.util.Random;

/**
 * Regras do Jogo da Memória para dois jogadores. Sem Swing e sem rede: só estado e regras.
 *
 * <p>Os dois computadores constroem este modelo com a mesma {@code seed} e aplicam as mesmas
 * jogadas na mesma ordem; por isso os dois tabuleiros ficam iguais sem trocar o baralho.</p>
 *
 * <p>Jogador 0 é quem convidou e começa. Acertou o par: continua e soma ponto. Errou: as duas
 * cartas ficam à mostra ({@link #hasPendingHide()}) até {@link #hidePending()} e a vez passa.</p>
 */
public class MemoryGameModel {

    public enum CardState { HIDDEN, REVEALED, MATCHED }

    public enum FlipResult { INVALID, FIRST, MATCH, MISMATCH }

    private final int pairs;
    private final int[] values;
    private final CardState[] states;
    private final int[] matchedBy;
    private final int[] scores = new int[2];

    private int currentPlayer = 0;
    private int firstPick = -1;
    private int hideA = -1;
    private int hideB = -1;
    private int mismatchCount = 0;
    private int matchedPairs = 0;

    public MemoryGameModel(long seed, int pairs) {
        if (pairs < 2) {
            throw new IllegalArgumentException("pairs < 2");
        }
        this.pairs = pairs;
        this.values = shuffledDeck(seed, pairs);
        this.states = new CardState[values.length];
        this.matchedBy = new int[values.length];
        for (int i = 0; i < states.length; i++) {
            states[i] = CardState.HIDDEN;
            matchedBy[i] = -1;
        }
    }

    /** Embaralhamento Fisher–Yates com {@link Random}(seed): igual em qualquer JVM. */
    static int[] shuffledDeck(long seed, int pairs) {
        int[] deck = new int[pairs * 2];
        for (int i = 0; i < deck.length; i++) {
            deck[i] = i / 2;
        }
        Random random = new Random(seed);
        for (int i = deck.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = deck[i];
            deck[i] = deck[j];
            deck[j] = tmp;
        }
        return deck;
    }

    /**
     * Vira a carta {@code index} para o jogador {@code player}.
     * Se havia um erro à mostra, ele é escondido antes (a próxima jogada "limpa" a mesa).
     */
    public FlipResult flip(int player, int index) {
        hidePending();
        if (isGameOver() || player != currentPlayer) {
            return FlipResult.INVALID;
        }
        if (index < 0 || index >= states.length || states[index] != CardState.HIDDEN) {
            return FlipResult.INVALID;
        }
        states[index] = CardState.REVEALED;
        if (firstPick < 0) {
            firstPick = index;
            return FlipResult.FIRST;
        }
        int first = firstPick;
        firstPick = -1;
        if (values[first] == values[index]) {
            states[first] = CardState.MATCHED;
            states[index] = CardState.MATCHED;
            matchedBy[first] = player;
            matchedBy[index] = player;
            scores[player]++;
            matchedPairs++;
            return FlipResult.MATCH;
        }
        hideA = first;
        hideB = index;
        mismatchCount++;
        currentPlayer = 1 - player;
        return FlipResult.MISMATCH;
    }

    public boolean hasPendingHide() {
        return hideA >= 0;
    }

    /** Desvira as duas cartas do último erro, se ainda estiverem à mostra. */
    public void hidePending() {
        if (hideA < 0) {
            return;
        }
        states[hideA] = CardState.HIDDEN;
        states[hideB] = CardState.HIDDEN;
        hideA = -1;
        hideB = -1;
    }

    /** Contador de erros; quem agenda {@link #hidePending()} usa para não esconder o erro seguinte. */
    public int getMismatchCount() {
        return mismatchCount;
    }

    public boolean isGameOver() {
        return matchedPairs == pairs;
    }

    /** 0 ou 1; -1 se empate (ou jogo ainda não acabou). */
    public int getWinner() {
        if (!isGameOver() || scores[0] == scores[1]) {
            return -1;
        }
        return scores[0] > scores[1] ? 0 : 1;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public int getScore(int player) {
        return scores[player];
    }

    public int getCardCount() {
        return values.length;
    }

    public int getPairs() {
        return pairs;
    }

    public CardState getState(int index) {
        return states[index];
    }

    /** Valor (id do par) da carta. A tela só deve mostrá-lo se a carta não estiver {@code HIDDEN}. */
    public int getValue(int index) {
        return values[index];
    }

    /** Quem fechou o par (0 ou 1), ou -1 se a carta ainda não foi pareada. */
    public int getMatchedBy(int index) {
        return matchedBy[index];
    }
}