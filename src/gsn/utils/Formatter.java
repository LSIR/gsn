package gsn.utils;

import java.util.Iterator;
import java.util.List;

public class Formatter {

    public static String listArray(int[] a, int from, int to) {
        return listArray(a, from, to, false);
    }

    public static String listArray(int[] a, int len) {
        return listArray(a, 0, len);
    }

    public static String listArray(int[] a, int len, boolean hexFormat) {
        return listArray(a, 0, len, hexFormat);
    }

    public static String listArray(int[] a, int from, int to, boolean hexFormat) {
        StringBuilder hex_sb_2 = new StringBuilder();
        StringBuilder dec_sb_2 = new StringBuilder();
        for (int i = from; (i <= to && i < a.length); i++) {
            hex_sb_2.append(String.format("%02x", a[i] & 0xff)).append(" ");
            dec_sb_2.append(a[i] & 0xff).append(" ");
        }

        hex_sb_2.append("(").append(String.format("%2d", to - from + 1)).append(")");
        dec_sb_2.append("(").append(String.format("%2d", to - from + 1)).append(")");

        if (hexFormat)
            return hex_sb_2.toString();
        else
            return dec_sb_2.toString();
    }

    public static String listArray(byte[] a, int from, int to) {
        return listArray(a, from, to, false);
    }

    public static String listArray(byte[] a, int len) {
        return listArray(a, 0, len);
    }

    public static String listArray(byte[] a, int len, boolean hexFormat) {
        return listArray(a, 0, len, hexFormat);
    }

    public static String listArray(byte[] a, int from, int to, boolean hexFormat) {
        StringBuilder hex_sb_2 = new StringBuilder();
        StringBuilder dec_sb_2 = new StringBuilder();
        for (int i = from; (i <= to && i < a.length); i++) {
            hex_sb_2.append(String.format("%02x", a[i] & 0xff)).append(" ");
            dec_sb_2.append(a[i] & 0xff).append(" ");
        }

        hex_sb_2.append("(").append(String.format("%2d", to - from + 1)).append(")");
        dec_sb_2.append("(").append(String.format("%2d", to - from + 1)).append(")");

        if (hexFormat)
            return hex_sb_2.toString();
        else
            return dec_sb_2.toString();
    }

    public static String listArray(UnsignedByte[] a, int from, int to) {
        return listArray(a, from, to, false);
    }

    public static String listArray(UnsignedByte[] a, int len) {
        return listArray(a, 0, len);
    }

    public static String listArray(UnsignedByte[] a, int len, boolean hexFormat) {
        return listArray(a, 0, len, hexFormat);
    }

    public static String listArray(UnsignedByte[] a, int from, int to, boolean hexFormat) {
        StringBuilder hex_sb_2 = new StringBuilder();
        StringBuilder dec_sb_2 = new StringBuilder();
        for (int i = from; (i <= to && i < a.length); i++) {
            hex_sb_2.append(String.format("%02x", a[i].getByte())).append(" ");
            dec_sb_2.append(a[i].getInt()).append(" ");
        }

        hex_sb_2.append("(").append(String.format("%2d", to - from + 1)).append(")");
        dec_sb_2.append("(").append(String.format("%2d", to - from + 1)).append(")");

        if (hexFormat)
            return hex_sb_2.toString();
        else
            return dec_sb_2.toString();
    }

    public static String listUnsignedByteList(List<UnsignedByte> a) {
        return listUnsignedByteList(a, false);
    }

    public static String listUnsignedByteList(List<UnsignedByte> a, boolean hexFormat) {
        StringBuilder hex_sb_2 = new StringBuilder();
        StringBuilder dec_sb_2 = new StringBuilder();
        Iterator iter = a.iterator();
        while (iter.hasNext()) {
            UnsignedByte ub = (UnsignedByte) iter.next();
            hex_sb_2.append(String.format("%02x", ub.getByte())).append(" ");
            dec_sb_2.append(ub.getInt()).append(" ");
        }

        hex_sb_2.append("(").append(String.format("%2d", a.size())).append(")");
        dec_sb_2.append("(").append(String.format("%2d", a.size())).append(")");

        if (hexFormat)
            return hex_sb_2.toString();
        else
            return dec_sb_2.toString();
    }

}
