package tw.angus.localeinspector;
import android.content.res.Resources;
import java.util.Locale;

/** Branch regression tests run the actual shipped LanguageUtil class with JVM Android fixtures. */
public final class LanguageUtilTest {
    private static int count;
    private static final LanguageUtil util = new LanguageUtil();

    public static void main(String[] args) {
        check("zh-Hans-CN", true, "script");
        check("zh-Hant-CN", false, "script");
        check("zh-Hans-TW", true, "script");
        check("zh-Hans-HK", true, "script");
        check("zh-Hans-MO", true, "script");
        check("zh-Hant-SG", false, "script");
        check("zh-Latn-CN", false, "script");
        check("zh-CN", true, "country");
        check("zh-TW", false, "country");
        check("zh-HK", false, "country");
        check("zh-MO", false, "country");
        check("zh-SG", true, "country");
        check("zh-US", true, "country");
        check("zh", true, "country-empty");
        check("en-CN", false, "language");
        check("en-Hans-CN", false, "language");
        check("und", false, "language");

        Resources.setTestLocales(Locale.forLanguageTag("zh-Hant-TW"), Locale.forLanguageTag("zh-Hans-CN"));
        expect("Only first system locale is used", util.isOsChinese(), false);
        Locale saved = Locale.getDefault();
        try {
            Resources.setTestLocales();
            Locale.setDefault(Locale.forLanguageTag("zh-Hans-CN"));
            expect("Empty system list falls back to default Hans", util.isOsChinese(), true);
            Locale.setDefault(Locale.forLanguageTag("zh-Hant-CN"));
            expect("Empty system list falls back to default Hant", util.isOsChinese(), false);
        } finally { Locale.setDefault(saved); }
        Resources.fail = true;
        try { expect("Resource exception returns false", util.isOsChinese(), false); }
        finally { Resources.fail = false; }
        System.out.println("PASS: " + count + " cases; Android framework lookups use JVM fixtures (not device tests).");
    }

    private static void check(String tag, boolean expected, String branch) {
        Locale locale = Locale.forLanguageTag(tag);
        Resources.setTestLocales(locale);
        expect(tag, util.isOsChinese(), expected);
        LocaleRule.Result reason = LocaleRule.evaluate(locale);
        if (reason.passed != expected || !reason.branch.equals(branch)) {
            throw new AssertionError("Explanation disagrees with method: " + tag);
        }
    }

    private static void expect(String label, boolean actual, boolean expected) {
        if (actual != expected) throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        count++;
        System.out.println("PASS " + label + " => " + actual);
    }
}
