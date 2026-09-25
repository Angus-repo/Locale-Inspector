package android.os;
import java.util.Locale;
/** JVM test fixture only; never packaged into the APK. */
public final class LocaleList {
    private final Locale[] values;
    public LocaleList(Locale... values) { this.values = values; }
    public boolean isEmpty() { return values.length == 0; }
    public Locale get(int index) { return values[index]; }
}
