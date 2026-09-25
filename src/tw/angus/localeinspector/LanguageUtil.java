package tw.angus.localeinspector;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.TextUtils;
import java.util.Locale;

/**
 * Port of AiDEX CN 1.15.1 LanguageUtil.isOsChinese().
 * Same system-resource lookup, fallback, branches and exception result.
 * App-specific logging and unrelated preferences are omitted.
 */
public final class LanguageUtil {
    public boolean isOsChinese() {
        try {
            Configuration configuration = Resources.getSystem().getConfiguration();
            Locale locale = !configuration.getLocales().isEmpty() ? configuration.getLocales().get(0) : null;
            if (locale == null) {
                locale = Locale.getDefault();
            }
            if (!"zh".equalsIgnoreCase(locale.getLanguage())) {
                return false;
            }
            String script = locale.getScript();
            if (!TextUtils.isEmpty(script)) {
                return "Hans".equalsIgnoreCase(script);
            }
            String country = locale.getCountry();
            if (TextUtils.isEmpty(country)) {
                return true;
            }
            String upperCase = country.toUpperCase(Locale.ROOT);
            if (!"TW".equals(upperCase) && !"HK".equals(upperCase) && !"MO".equals(upperCase)) {
                "CN".equals(upperCase);
                return true;
            }
            return false;
        } catch (Exception exception) {
            return false;
        }
    }
}
