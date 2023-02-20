package com.utility;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class ValidatorUtility {
    public static boolean isNullOrEmpty(String string, boolean trim) {
        return !Optional.ofNullable(string).map((s) -> {
            return trim ? s.trim() : s;
        }).filter((s) -> {
            return !s.isEmpty();
        }).isPresent();
    }

    public static boolean isNullOrEmpty(String string) {
        return isNullOrEmpty(string, true);
    }

    public static boolean isNullOrEmpty(Map map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNullOrEmpty(Collection coll) {
        return coll == null || coll.isEmpty();
    }

    public static boolean isNullOrEmpty(Object[] arr) {
        return arr == null || arr.length == 0;
    }
}
