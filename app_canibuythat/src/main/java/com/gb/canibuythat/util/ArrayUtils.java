package com.gb.canibuythat.util;

public class ArrayUtils {

    public static <T> String join(String delimiter, T[] tokens,
            Stringifier<T> stringifier) {
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

    public static <T> String join(String delimiter, Iterable<T> tokens,
            Stringifier<T> stringifier) {
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
        public String toString(int index, T item);
    }
}
