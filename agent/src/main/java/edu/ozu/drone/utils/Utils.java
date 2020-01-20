package edu.ozu.drone.utils;

public class Utils {
    public static String toString(Object[] a) {
        return  toString(a, ", ");
    }

    public static String toString(Object[] arr, String delim) {
        if (arr == null)
            return "null";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < arr.length; i++)
        {
            sb.append(arr[i]);
            if (i == arr.length - 1)
                return sb.append(']').toString();
            sb.append(delim);
        }

        return "[]";
    }
}
