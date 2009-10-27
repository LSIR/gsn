package gsn.beans.windowing;

public class WindowingUtil {

    public static int GCD(int a, int b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        return GCDHelper(a, b);
    }

    private static int GCDHelper(int a, int b) {
        if (b == 0) {
            return a;
        }
        return GCDHelper(b, a % b);
    }
}
