package android.content.res;
import android.os.LocaleList;
import java.util.Locale;
/** JVM test fixture only; never packaged into the APK. */
public final class Resources {
    private static final Resources SYSTEM = new Resources();
    private static Configuration configuration = new Configuration(new LocaleList());
    public static boolean fail;
    public static Resources getSystem() {
        if (fail) throw new IllegalStateException("Test: system resources unavailable");
        return SYSTEM;
    }
    public Configuration getConfiguration() { return configuration; }
    public static void setTestLocales(Locale... locales) {
        configuration = new Configuration(new LocaleList(locales));
    }
}
