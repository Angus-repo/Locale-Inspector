package tw.angus.localeinspector;

import java.util.Locale;

/** Pure Java equivalent of AiDEX CN 1.15.1 LanguageUtil.isOsChinese(). */
public final class LocaleRule {
    private LocaleRule() { }

    public static final class Result {
        public final boolean passed;
        public final String branch;
        public final String explanation;

        Result(boolean passed, String branch, String explanation) {
            this.passed = passed;
            this.branch = branch;
            this.explanation = explanation;
        }
    }

    public static Result evaluate(Locale locale) {
        if (locale == null) {
            return new Result(false, "error", "無法取得 Locale。");
        }
        if (!"zh".equalsIgnoreCase(locale.getLanguage())) {
            return new Result(false, "language", "Language 不是 zh，語言檢查未通過；尚未檢查 Script 與 Country。");
        }
        String script = locale.getScript();
        if (!script.isEmpty()) {
            boolean passed = "Hans".equalsIgnoreCase(script);
            return new Result(passed, "script", passed
                ? "Language 為 zh，Script 為 Hans；直接通過，不檢查 Country。"
                : "Language 為 zh，但 Script 為 " + script + "，不是 Hans；直接回傳 false，不檢查 Country。");
        }
        String country = locale.getCountry();
        if (country.isEmpty()) {
            return new Result(true, "country-empty", "Language 為 zh，Script 與 Country 都是空字串；依原函式通過。");
        }
        String upperCase = country.toUpperCase(Locale.ROOT);
        boolean passed = !"TW".equals(upperCase) && !"HK".equals(upperCase) && !"MO".equals(upperCase);
        // The original standalone "CN".equals(upperCase) discards its result.
        return new Result(passed, "country", passed
            ? "Language 為 zh，Script 是空字串，Country 不在 TW／HK／MO；通過。"
            : "Language 為 zh，Script 是空字串，Country 為 " + upperCase + "；未通過。");
    }
}
