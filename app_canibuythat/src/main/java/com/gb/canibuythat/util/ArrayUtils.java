package com.gb.canibuythat.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ArrayUtils {

    /**
     * It is assumed, that each item in the specified <code>array</code> appears once
     * or not at all in the specified <code>text</code>.<br>
     * The items of the specified <code>array</code> are sorted based on the order in
     * which they occur in the specified <code>text</code>.
     */
    static String[] sortByOccurrence(String[] array, String text) {
        final Map<String, Integer> indexMap = new HashMap<>();
        for (String anArray : array) {
            indexMap.put(anArray, text.indexOf(anArray));
        }
        Arrays.sort(array, (lhs, rhs) -> indexMap.get(lhs).compareTo(indexMap.get(rhs)));
        return array;
    }

    public static <T> String join(String delimiter, T[] tokens, Stringifier<T> stringifier) {
        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        int index = 0;
        for (T token : tokens) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }
            if (stringifier != null) {
                sb.append(stringifier.toString(index++, token));
            } else {
                sb.append(token);
            }
        }
        return sb.toString();
    }

    public static <T> String join(String delimiter, Iterable<T> tokens, Stringifier<T> stringifier) {
        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        int index = 0;
        for (T token : tokens) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(stringifier.toString(index++, token));
        }
        return sb.toString();
    }

    public interface Stringifier<T> {
        String toString(int index, T item);
    }
}
