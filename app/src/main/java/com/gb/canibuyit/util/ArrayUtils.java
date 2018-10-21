package com.gb.canibuyit.util;

public class ArrayUtils {

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

    public interface Stringifier<T> {
        String toString(int index, T item);
    }
}
