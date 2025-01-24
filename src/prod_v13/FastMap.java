package prod_v13;

import battlecode.common.*;

public class FastMap<T> {
    private StringBuilder keys = new StringBuilder();
    private T[] values;
    private int p = 0;

    FastMap(int capacity) {
        values = (T[]) new Object[capacity];
    }

    public boolean add(char key, T value) {
        String str = String.valueOf(key);
        if (keys.indexOf(str) == -1) {
            values[p++] = value;
            keys.append(str);
            return true;
        }

        return false;
    }

    public boolean contains(char key) {
        return keys.indexOf(String.valueOf(key)) > -1;
    }

    public T get(char key) {
        int i = keys.indexOf(String.valueOf(key));
        if (i == -1) return null;
        return values[i];
    }

}