package com.fossgalaxy.games.fireworks.state;

import java.util.*;

/**
 * A hand that 'remembers' negative information.
 */
public class NegativeHand extends BasicHand {

    //what the agent can infer about it's hand (based on negative information)
    private final Map<Integer, Set<CardColour>> possibleColours;
    private final Map<Integer, Set<Integer>> possibleValues;

    public NegativeHand(NegativeHand hand) {
        super(hand);

        this.possibleColours = copyEnumMap(hand.possibleColours);
        this.possibleValues = copyMap(hand.possibleValues);
    }

    public NegativeHand(int size) {
        super(size);
        this.possibleColours = new HashMap<>();
        this.possibleValues = new HashMap<>();
    }

    private static <T> Map<Integer, Set<T>> copyMap(Map<Integer, Set<T>> map) {
        Map<Integer, Set<T>> mapCopy = new HashMap<>();
        for (Map.Entry<Integer, Set<T>> entry : map.entrySet()) {
            mapCopy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return mapCopy;
    }

    private static <T extends Enum> Map<Integer, Set<T>> copyEnumMap(Map<Integer, Set<T>> map) {
        Map<Integer, Set<T>> mapCopy = new HashMap<>();
        for (Map.Entry<Integer, Set<T>> entry : map.entrySet()) {
            mapCopy.put(entry.getKey(), EnumSet.copyOf(entry.getValue()));
        }
        return mapCopy;
    }

    /**
     * Reset all information about a slot
     */
    @Override
    void clear(int slot) {
        super.clear(slot);
        possibleColours.put(slot, EnumSet.allOf(CardColour.class));
        possibleValues.put(slot, new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)));
    }

    // From this players perspective
    @Override
    public CardColour getKnownColour(int slot) {
        Set<CardColour> c = possibleColours.get(slot);
        if (c != null && c.size() == 1) {
            return c.iterator().next();
        }
        return super.getKnownColour(slot);
    }

    @Override
    public Integer getKnownValue(int slot) {
        Set<Integer> c = possibleValues.get(slot);
        if (c != null && c.size() == 1) {
            return c.iterator().next();
        }
        return super.getKnownValue(slot);
    }

    @Override
    public void setKnownColour(CardColour colour, Integer[] slots) {
        int index = 0;
        for (int slot = 0; slot < getSize(); slot++) {
            if (index < slots.length && slots[index] == slot) {
                //we found a matching slot
                possibleColours.put(slot, EnumSet.of(colour));
                index++;
            } else {
                Set<CardColour> colours = possibleColours.get(slot);
                if (colours != null) {
                    colours.remove(colour);
                }
            }
        }
        super.setKnownColour(colour, slots);
    }

    @Override
    public void setKnownValue(Integer value, Integer[] slots) {
        int index = 0;
        for (int slot = 0; slot < getSize(); slot++) {
            if (index < slots.length && slots[index] == slot) {
                //we found a matching slot
                possibleValues.put(slot, new HashSet<Integer>(Arrays.asList(value)));
                index++;
            } else {
                Set<Integer> values = possibleValues.get(slot);
                if (values != null) {
                    values.remove(value);
                }
            }
        }
        super.setKnownValue(value, slots);
    }

    @Override
    public int[] getPossibleValues(int slot) {
        Set<Integer> possible = possibleValues.get(slot);
        if (possible == null) {
            return super.getPossibleValues(slot);
        }

        int[] possibleArr = new int[possible.size()];
        int i = 0;
        for (Integer card : possible) {
            possibleArr[i++] = card;
        }

        return possibleArr;
    }

    @Override
    public CardColour[] getPossibleColours(int slot) {

        Set<CardColour> possible = possibleColours.get(slot);
        if (possible == null) {
            return super.getPossibleColours(slot);
        }

        CardColour[] possibleArr = new CardColour[possible.size()];
        int i = 0;
        for (CardColour card : possible) {
            possibleArr[i++] = card;
        }

        return possibleArr;
    }

    @Override
    public boolean isPossible(int slot, Card card) {
        if (!hasCard(slot)) {
            return false;
        }

        Set<CardColour> possibleColour = possibleColours.get(slot);
        Set<Integer> possibleValue = possibleValues.get(slot);
        return possibleColour.contains(card.colour) && possibleValue.contains(card.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NegativeHand cards = (NegativeHand) o;

        if (possibleColours != null ? !possibleColours.equals(cards.possibleColours) : cards.possibleColours != null)
            return false;
        return possibleValues != null ? possibleValues.equals(cards.possibleValues) : cards.possibleValues == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (possibleColours != null ? possibleColours.hashCode() : 0);
        result = 31 * result + (possibleValues != null ? possibleValues.hashCode() : 0);
        return result;
    }
}
