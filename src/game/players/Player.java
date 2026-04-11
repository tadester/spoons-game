package game.players;

import game.cards.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Player {
    private static final String LETTER_SEQUENCE = "SPOONS";

    private final String name;
    private final List<Card> hand;
    private boolean hasSpoon;
    private int letters;

    public Player(String name) {
        this.name = name;
        this.hand = new ArrayList<>();
        this.hasSpoon = false;
        this.letters = 0;
    }

    public String getName() {
        return name;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public boolean checkForMatch() {
        if (hand.size() < 4) return false;
        String value = hand.get(0).getValue();
        for (Card card : hand) {
            if (!card.getValue().equals(value)) {
                return false;
            }
        }
        return true;
    }

    public void grabSpoon() {
        hasSpoon = true;
    }

    public boolean hasSpoon() {
        return hasSpoon;
    }

    public void resetSpoon() {
        hasSpoon = false;
    }

    public void resetHand() {
        hand.clear();
    }

    public void resetRoundState() {
        resetHand();
        resetSpoon();
    }

    public void replaceCard(int index) {
        if (index >= 0 && index < hand.size()) {
            hand.remove(index);
        }
    }

    public int getLetters() {
        return letters;
    }

    public String getLetterProgress() {
        return LETTER_SEQUENCE.substring(0, Math.min(letters, LETTER_SEQUENCE.length()));
    }

    public void addLetter() {
        if (letters < LETTER_SEQUENCE.length()) {
            letters++;
        }
    }

    public boolean isEliminated() {
        return letters >= LETTER_SEQUENCE.length();
    }
}
