package tw.angus.localeinspector;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int BG = 0xfff2f5f7;
    private static final int INK = 0xff17354a;
    private static final int MUTED = 0xff526878;
    private static final int BORDER = 0xffdce4e9;
    private LinearLayout page;
    private String report = "";
    private TextView methodResultView;
    private LinearLayout statusCard;
    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(24), dp(22), dp(32));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);
    }

    @Override public void onResume() {
        super.onResume();
        refresh();
    }

    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        refresh();
    }

    private void refresh() {
        if (page == null) return;
        page.removeAllViews();
        methodResultView = null;
        statusCard = null;
        add(page, text("LOCALE INSPECTOR  /  1.0", 11, MUTED, true), 0);
        add(page, text("系統語系檢查", 29, INK, true), 7);
        add(page, text("LanguageUtil 所需的系統 Locale 原始值", 14, MUTED, false), 5);
        try {
            // Same lookup order as the inspected AiDEX LanguageUtil, API 24+.
            Configuration configuration = Resources.getSystem().getConfiguration();
            LocaleList locales = configuration.getLocales();
            Locale locale = !locales.isEmpty() ? locales.get(0) : null;
            boolean fallback = locale == null;
            if (locale == null) locale = Locale.getDefault();
            LocaleRule.Result result = LocaleRule.evaluate(locale);
            String captured = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT).format(new Date());

            LinearLayout values = card(Color.WHITE);
            value(values, "LANGUAGE  /  語言", locale.getLanguage(), false);
            divider(values);
            value(values, "SCRIPT  /  文字系統", locale.getScript(), false);
            divider(values);
            value(values, "COUNTRY  /  國家或地區碼", locale.getCountry(), false);
            add(page, values, 22);

            statusCard = card(0xffe8eef4);
            add(statusCard, text("LanguageUtil.isOsChinese()", 13, INK, true), 0);
            methodResultView = text("尚未執行", 29, INK, true);
            methodResultView.setTextIsSelectable(true);
            add(statusCard, methodResultView, 7);
            add(statusCard, text("目前欄位的判斷分支：\n" + result.explanation, 14, MUTED, false), 10);
            Button check = button("執行 isOsChinese()", true);
            check.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) { runLanguageCheck(); }
            });
            add(statusCard, check, 15);
            add(page, statusCard, 16);

            LinearLayout tag = card(Color.WHITE);
            value(tag, "LANGUAGE TAG  /  完整標籤", locale.toLanguageTag(), true);
            add(tag, text("來源：" + (fallback ? "Locale.getDefault()（備援）" : "系統 Locale 清單的第一筆"), 12, MUTED, false), 11);
            add(page, tag, 12);
            add(page, text("更新於 " + captured, 11, MUTED, false), 12);

            StringBuilder allLocales = new StringBuilder();
            for (int i = 0; i < locales.size(); i++) {
                if (i > 0) allLocales.append('\n');
                allLocales.append(i + 1).append(". ").append(rawLocale(locales.get(i)));
            }
            Locale defaultLocale = Locale.getDefault();
            LocaleList appLocales = getResources().getConfiguration().getLocales();
            Locale appLocale = appLocales.isEmpty() ? null : appLocales.get(0);

            report = "Locale Inspector 1.0.0\n"
                + "更新時間：" + captured + "\n"
                + "Android：" + Build.VERSION.RELEASE + " / API " + Build.VERSION.SDK_INT + "\n"
                + "來源：" + (fallback ? "Locale.getDefault()" : "Resources.getSystem().getConfiguration().getLocales().get(0)") + "\n\n"
                + "language = " + quoted(locale.getLanguage()) + "\n"
                + "script = " + quoted(locale.getScript()) + "\n"
                + "country = " + quoted(locale.getCountry()) + "\n"
                + "languageTag = " + quoted(locale.toLanguageTag()) + "\n\n"
                + "isOsChinese = 尚未執行\n"
                + "檢查分支 = " + result.branch + "\n"
                + "原因：" + result.explanation + "\n\n"
                + "系統 Locale 清單：\n" + (allLocales.length() == 0 ? "（空）" : allLocales.toString()) + "\n\n"
                + "Locale.getDefault()：\n" + rawLocale(defaultLocale) + "\n\n"
                + "本工具 App 的資源 Locale（僅供比較）：\n" + rawLocale(appLocale) + "\n\n"
                + "依 AiDEX CN 1.15.1 LanguageUtil 判斷；只涵蓋語言檢查。\n";

            actions();
            add(page, text("讀值說明", 15, INK, true), 25);
            add(page, text("Script、Country 顯示實際回傳值，空字串不會被自動補成 Hans 或 CN。Script 有值時，原函式會直接判斷它，不再檢查 Country。", 13, MUTED, false), 7);
            add(page, text("這裡只檢查語言條件，無法代表其他配對檢查的結果。", 13, MUTED, false), 7);
            LinearLayout more = card(Color.WHITE);
            add(more, text("其他 Locale（供比較）", 13, INK, true), 0);
            add(more, text("Locale.getDefault()\n" + rawLocale(defaultLocale), 12, MUTED, false), 10);
            add(more, text("本工具 App 資源 Locale\n" + rawLocale(appLocale), 12, MUTED, false), 10);
            add(more, text("系統 Locale 清單\n" + (allLocales.length() == 0 ? "（空）" : allLocales.toString()), 12, MUTED, false), 10);
            add(page, more, 16);
        } catch (Exception exception) {
            report = "Locale Inspector：讀取失敗\n" + exception.getClass().getName() + ": " + exception.getMessage();
            LinearLayout error = card(0xffffecdf);
            add(error, text("無法讀取系統 Locale", 22, 0xff9b421b, true), 0);
            add(error, text(report, 14, INK, false), 8);
            add(page, error, 20);
            actions();
        }
    }

    private void runLanguageCheck() {
        // Refresh the visible raw values, then execute the ported original method.
        refresh();
        boolean passed = languageUtil.isOsChinese();
        if (methodResultView != null) {
            methodResultView.setText(Boolean.toString(passed));
            methodResultView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            methodResultView.setTextColor(passed ? 0xff146b50 : 0xff9b421b);
            statusCard.setBackground(background(passed ? 0xffe3f3eb : 0xffffecdf, Color.TRANSPARENT));
            methodResultView.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        } else {
            Toast.makeText(this, "isOsChinese() = " + passed, Toast.LENGTH_LONG).show();
        }
        report = report.replace("isOsChinese = 尚未執行", "isOsChinese = " + passed);
    }

    private void actions() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button refresh = button("重新讀取", true);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { refresh(); }
        });
        Button copy = button("複製結果", false);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("系統語系檢查", report));
                    Toast.makeText(MainActivity.this, "已複製語系檢查結果", Toast.LENGTH_SHORT).show();
                }
            }
        });
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -2, 1);
        left.setMarginEnd(dp(6));
        row.addView(refresh, left);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, -2, 1);
        right.setMarginStart(dp(6));
        row.addView(copy, right);
        add(page, row, 17);
        Button settings = button("開啟系統語言設定", false);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                try {
                    startActivity(new Intent(Settings.ACTION_LOCALE_SETTINGS));
                } catch (ActivityNotFoundException exception) {
                    Toast.makeText(MainActivity.this, "請到手機設定搜尋「語言」", Toast.LENGTH_LONG).show();
                }
            }
        });
        add(page, settings, 10);
    }

    private void value(LinearLayout parent, String label, String raw, boolean compact) {
        add(parent, text(label, 11, MUTED, true), 0);
        TextView value = text(raw.isEmpty() ? "\"\"（空字串）" : raw, compact ? 21 : 29, INK, true);
        value.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        value.setTextIsSelectable(true);
        add(parent, value, 4);
    }

    private void divider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
        params.setMargins(0, dp(14), 0, dp(14));
        parent.addView(divider, params);
    }

    private LinearLayout card(int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(19), dp(17), dp(19), dp(17));
        card.setBackground(background(color, Color.TRANSPARENT));
        return card;
    }

    private TextView text(String value, float sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setLineSpacing(dp(3), 1.0f);
        if (bold) view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return view;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(15);
        button.setTextColor(primary ? Color.WHITE : INK);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(10), dp(13), dp(10), dp(13));
        button.setMinHeight(dp(50));
        button.setMinimumHeight(dp(50));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(primary ? 0x33ffffff : 0x2217354a),
            background(primary ? INK : Color.WHITE, primary ? INK : BORDER), null));
        return button;
    }

    private GradientDrawable background(int color, int border) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(16));
        if (border != Color.TRANSPARENT) drawable.setStroke(dp(1), border);
        return drawable;
    }

    private void add(LinearLayout parent, View child, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(top);
        parent.addView(child, params);
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private static String quoted(String value) { return "\"" + value + "\""; }
    private static String rawLocale(Locale locale) {
        if (locale == null) return "（無）";
        return locale.toLanguageTag() + "  [language=" + quoted(locale.getLanguage())
            + ", script=" + quoted(locale.getScript()) + ", country=" + quoted(locale.getCountry()) + "]";
    }
}
