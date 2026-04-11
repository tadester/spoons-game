package game;

import game.cards.Card;
import game.players.Player;

import java.util.Arrays;
import java.util.List;

public class SpoonsEngineTest {

    public static void main(String[] args) {
        testRoundStartsWithFourCards();
        testHumanDrawDiscardReturnsToFourCards();
        testSpoonRaceAddsLetter();
        testNpcTurnsKeepFourCards();
        testSimulatedGameFinishes();
        System.out.println("All SpoonsEngine tests passed.");
    }

    private static void testRoundStartsWithFourCards() {
        SpoonsEngine engine = new SpoonsEngine(Arrays.asList("You", "Ava", "Milo", "Zara"));
        assert engine.getPlayers().size() == 4;
        for (Player player : engine.getPlayers()) {
            assert player.getHand().size() == 4;
            assert !player.hasSpoon();
        }
        assert engine.getCurrentPlayerIndex() == 0;
        assert engine.getSpoonsRemaining() == 3;
    }

    private static void testHumanDrawDiscardReturnsToFourCards() {
        SpoonsEngine engine = new SpoonsEngine(Arrays.asList("You", "Ava", "Milo", "Zara"));
        Card drawnCard = engine.drawForHuman();
        assert drawnCard != null;
        Card discarded = engine.getPlayers().get(0).getHand().get(0);
        SpoonsEngine.TurnOutcome outcome = engine.discardForHuman(discarded);
        assert outcome != null;
        assert engine.getPendingDraw() == null;
        assert engine.getPlayers().get(0).getHand().size() == 4;
        assert engine.getCurrentPlayerIndex() == 1;
    }

    private static void testSpoonRaceAddsLetter() {
        SpoonsEngine engine = new SpoonsEngine(Arrays.asList("You", "Ava", "Milo"));
        Player you = engine.getPlayers().get(0);
        Player ava = engine.getPlayers().get(1);
        Player milo = engine.getPlayers().get(2);

        you.resetHand();
        ava.resetHand();
        milo.resetHand();

        you.addCard(new Card("hearts", "7"));
        you.addCard(new Card("clubs", "7"));
        you.addCard(new Card("diamonds", "7"));
        you.addCard(new Card("spades", "7"));

        ava.addCard(new Card("hearts", "3"));
        ava.addCard(new Card("clubs", "4"));
        ava.addCard(new Card("diamonds", "5"));
        ava.addCard(new Card("spades", "6"));

        milo.addCard(new Card("hearts", "8"));
        milo.addCard(new Card("clubs", "9"));
        milo.addCard(new Card("diamonds", "10"));
        milo.addCard(new Card("spades", "jack"));

        SpoonsEngine.SpoonGrabResult firstGrab = engine.grabSpoon(0);
        assert firstGrab.isSuccess();
        SpoonsEngine.SpoonGrabResult secondGrab = engine.grabSpoon(1);
        assert secondGrab.isSuccess();
        assert secondGrab.getRoundResult() != null;
        assert secondGrab.getRoundResult().getLoserName().equals("Milo");
        assert secondGrab.getRoundResult().getLetters().equals("S");
    }

    private static void testNpcTurnsKeepFourCards() {
        SpoonsEngine engine = new SpoonsEngine(Arrays.asList("You", "Ava", "Milo", "Zara"));
        Card drawnCard = engine.drawForHuman();
        engine.discardForHuman(drawnCard);
        SpoonsEngine.TurnOutcome outcome = engine.takeNpcTurn();
        assert outcome != null;
        assert engine.getPlayers().get(1).getHand().size() == 4;
    }

    private static void testSimulatedGameFinishes() {
        SpoonsEngine engine = new SpoonsEngine(Arrays.asList("You", "Ava", "Milo"));

        for (int step = 0; step < 5000 && !engine.isGameOver(); step++) {
            if (engine.isRaceActive()) {
                if (engine.humanCanGrabSpoon()) {
                    engine.grabSpoon(0);
                }
                for (int index = 1; index < engine.getPlayers().size() && !engine.isGameOver(); index++) {
                    engine.grabSpoon(index);
                }
                continue;
            }

            if (engine.humanCanGrabSpoon()) {
                engine.grabSpoon(0);
                continue;
            }

            if (engine.humanCanDraw()) {
                Card drawnCard = engine.drawForHuman();
                Card discard = pickDiscard(engine.getPlayers().get(0), drawnCard);
                engine.discardForHuman(discard);
                continue;
            }

            if (engine.getCurrentPlayerIndex() != 0) {
                SpoonsEngine.TurnOutcome outcome = engine.takeNpcTurn();
                assert outcome != null;
                if (outcome.isMatched()) {
                    engine.grabSpoon(outcome.getPlayerIndex());
                }
            }
        }

        assert engine.isGameOver();
        assert engine.getPlayers().size() == 1;
    }

    private static Card pickDiscard(Player player, Card drawnCard) {
        Card bestDiscard = drawnCard;
        int bestScore = Integer.MIN_VALUE;

        List<Card> candidates = new java.util.ArrayList<>(player.getHand());
        candidates.add(drawnCard);

        for (Card candidate : candidates) {
            List<Card> kept = new java.util.ArrayList<>(player.getHand());
            if (!candidate.equals(drawnCard)) {
                kept.remove(candidate);
                kept.add(drawnCard);
            }

            java.util.Map<String, Integer> counts = new java.util.HashMap<>();
            int bestGroup = 0;
            for (Card keptCard : kept) {
                int count = counts.getOrDefault(keptCard.getValue(), 0) + 1;
                counts.put(keptCard.getValue(), count);
                bestGroup = Math.max(bestGroup, count);
            }

            if (bestGroup > bestScore) {
                bestScore = bestGroup;
                bestDiscard = candidate;
            }
        }

        return bestDiscard;
    }
}
