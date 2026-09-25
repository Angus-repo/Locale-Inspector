package android.content.res;
import android.os.LocaleList;
/** JVM test fixture only; never packaged into the APK. */
public final class Configuration {
    private final LocaleList locales;
    public Configuration(LocaleList locales) { this.locales = locales; }
    public LocaleList getLocales() { return locales; }
}
