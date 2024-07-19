package game.players;

import game.cards.Card;

import java.util.ArrayList;
import java.util.List;

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
        if (hand.size() < 4) {
            hand.add(card);
        }
    }

    public void removeCard(Card card) {
        hand.remove(card);
    }

    public boolean checkForMatch() {
        if (hand.size() < 4) return false;
        for (int i = 0; i < hand.size() - 3; i++) {
            if (hand.get(i).getSuit().equals(hand.get(i + 1).getSuit())
                && hand.get(i).getSuit().equals(hand.get(i + 2).getSuit())
                && hand.get(i).getSuit().equals(hand.get(i + 3).getSuit())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSpoon() {
        return hasSpoon;
    }

    public void grabSpoon() {
        this.hasSpoon = true;
    }

    public void resetSpoon() {
        this.hasSpoon = false;
    }

    @Override
    public String toString() {
        return name + "'s hand: " + hand.toString();
    }
}
