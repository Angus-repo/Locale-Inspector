package android.text;
/** JVM test fixture only; never packaged into the APK. */
public final class TextUtils {
    public static boolean isEmpty(CharSequence value) { return value == null || value.length() == 0; }
}
