package game.players;

import game.cards.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Player {
    private final String name;
    private final List<Card> hand;
    private boolean hasSpoon;

    public Player(String name) {
        this.name = name;
        this.hand = new ArrayList<>();
        this.hasSpoon = false;
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
        String suit = hand.get(0).getSuit();
        for (Card card : hand) {
            if (!card.getSuit().equals(suit)) {
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

    public void replaceCard(int index) {
        if (index >= 0 && index < hand.size()) {
            hand.remove(index);
        }
    }
}
