package omni.toolkit.functions;

import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;

import omni.toolkit.OTHelper;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Currency;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

@OTCategory
public class OTLocale {

    private Locale getLocaleFromString(String locale) {
        /* Return locale from string */
        return locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale.replace('_', '-'));
    }

    private int getDateStyleFromString(String style) {
        /* Match string to style */
        if (style != null && !style.equals("DEFAULT")) {
            switch (style) {
                case "SHORT":
                    return DateFormat.SHORT;
                case "MEDIUM":
                    return DateFormat.MEDIUM;
                case "LONG":
                    return DateFormat.LONG;
                case "FULL":
                    return DateFormat.FULL;
            }
        }

        /* Default style */
        return DateFormat.DEFAULT;
    }

    private FormatStyle getDateTimeStyleFromString(String style) {
        /* Match string to style */
        if (style != null && !style.equals("SHORT")) {
            switch (style) {
                case "MEDIUM":
                    return FormatStyle.MEDIUM;
                case "LONG":
                    return FormatStyle.LONG;
                case "FULL":
                    return FormatStyle.FULL;
            }
        }

        /* Default style */
        return FormatStyle.SHORT;
    }

    private FormatStyle getTimeStyleFromString(String style) {
        /* Match string to style */
        if (style != null && !style.equals("SHORT")) {
            if (style.equals("MEDIUM")) {
                return FormatStyle.MEDIUM;
            }
        }

        /* Default style */
        return FormatStyle.SHORT;
    }

    @Function
    public Double otParseDecimalByLocale(
            @Parameter @Name("text") String text,
            @Parameter(required = false) @Name("locale") String locale) {
        
        try {
            /* Null check */
            if (text.isEmpty()) {
                return null;
            }

            /* Decimal formatter from locale */
            DecimalFormat formatter = (DecimalFormat) DecimalFormat.getNumberInstance(getLocaleFromString(locale));

            /*
            * Remove every char from the string that is not a number, decimal separator or
            * a minus sign
            */
            String sep = String.valueOf(formatter.getDecimalFormatSymbols().getDecimalSeparator());
            String cleanText = text.replaceAll("[^\\d" + sep + "\\-]", "");

            /* Keep only one the first decimal separator */
            String[] parts = cleanText.split(Pattern.quote(sep), 2);
            if (parts.length > 1) {
                cleanText = parts[0] + sep + parts[1].replaceAll(Pattern.quote(sep), "");
            }

            /* Return parsed decimal */
            return formatter.parse(cleanText).doubleValue();
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    public String otFormatDecimalByLocale(
            @Parameter @Name("value") Double value,
            @Parameter(required = false) @Name("locale") String locale,
            @Parameter(required = false) @Name("decimalPlaces") Integer decimalPlaces,
            @Parameter(required = false) @Name("showSeparators") Boolean showSeparators) {
        
        /* Null check */
        if (value == null) {
            return "";
        }

        /* Decimal formatter from locale */
        DecimalFormat formatter = (DecimalFormat) DecimalFormat.getNumberInstance(getLocaleFromString(locale));

        /* Decimal places */
        if ((decimalPlaces != null) && (decimalPlaces >= 0)) {
            formatter.setMaximumFractionDigits(decimalPlaces);
            formatter.setMinimumFractionDigits(decimalPlaces);
        }

        /* Format decimal */
        String result = formatter.format(value);

        /* Remove separators if necessary, default false */
        if ((showSeparators != null) && !showSeparators) {
            String sep = String.valueOf(formatter.getDecimalFormatSymbols().getGroupingSeparator());
            result = result.replace(sep, "");
        }

        /* Return formatted decimal */
        return result.trim();
    }

    @Function
    public String otFormatCurrencyByLocale(
            @Parameter @Name("value") Double value,
            @Parameter @Name("isoCode") String isoCode,
            @Parameter(required = false) @Name("locale") String locale,
            @Parameter(required = false) @Name("decimalPlaces") Integer decimalPlaces,
            @Parameter(required = false) @Name("showSeparators") Boolean showSeparators,
            @Parameter(required = false) @Name("format") String format,
            @Parameter(required = false) @Name("indicatorAlignment") String indicatorAlignment) {

        /* Null check */
        if (value == null) {
            return "";
        }

        /* Decimal formatter from locale */
        Locale l = getLocaleFromString(locale);
        Currency c = isoCode.isEmpty() ? Currency.getInstance(l) : Currency.getInstance(isoCode);
        DecimalFormat formatter = (DecimalFormat) DecimalFormat.getCurrencyInstance(l);
        formatter.setCurrency(c);

        /* Force start */
        if ((indicatorAlignment != null) && indicatorAlignment.equals("START")
                && formatter.getPositivePrefix().isEmpty()) {
            formatter.setPositivePrefix(
                    formatter.getPositiveSuffix() + (formatter.getPositiveSuffix().startsWith(" ") ? " " : ""));
            formatter.setPositiveSuffix("");
        }

        /* Force end */
        if ((indicatorAlignment != null) && indicatorAlignment.equals("END")
                && formatter.getPositiveSuffix().isEmpty()) {
            formatter.setPositiveSuffix(
                    (formatter.getPositiveSuffix().endsWith(" ") ? " " : "") + formatter.getPositivePrefix());
            formatter.setPositivePrefix("");
        }

        formatter.setNegativePrefix(
                formatter.getPositivePrefix() + (formatter.getPositivePrefix().isEmpty() ? "" : " ") + "-");
        formatter.setNegativeSuffix(formatter.getPositiveSuffix());

        /* Decimal places */
        if ((decimalPlaces != null) && (decimalPlaces >= 0)) {
            formatter.setMaximumFractionDigits(decimalPlaces);
            formatter.setMinimumFractionDigits(decimalPlaces);
        }

        /* Format decimal */
        String result = formatter.format(value);

        /* Remove separators if necessary, default false */
        if ((showSeparators != null) && !showSeparators) {
            String sep = String.valueOf(formatter.getDecimalFormatSymbols().getGroupingSeparator());
            result = result.replace(sep, "");
        }

        /* Force symbol */
        if ((format != null) && format.equals("SYMBOL")) {
            result = result.replace(c.getCurrencyCode(), c.getSymbol());
        }

        /* Force code */
        if ((format != null) && format.equals("CODE")) {
            result = result.replace(c.getSymbol(), c.getCurrencyCode());
        }

        /* Return formatted currency */
        return result.trim();
    }

    @Function
    public String otFormatDateByLocale(
            @Parameter @Name("date") Date date,
            @Parameter(required = false) @Name("locale") String locale,
            @Parameter(required = false) @Name("style") String style) {
        
        /* Null check */
        if (date == null) {
            return "";
        }

        /* Date formatter from locale */
        DateFormat formatter = DateFormat.getDateInstance(getDateStyleFromString(style), getLocaleFromString(locale));

        /* Return formatted date */
        return formatter.format(date);
    }

    @Function
    public String otFormatDateTimeByLocale(
            @Parameter @Name("dateTime") Timestamp dateTime,
            @Parameter(required = false) @Name("locale") String locale,
            @Parameter(required = false) @Name("timezone") String timezone,
            @Parameter(required = false) @Name("dateStyle") String dateStyle,
            @Parameter(required = false) @Name("timeStyle") String timeStyle) {
        
        /* Null check */
        if (dateTime == null) {
            return "";
        }

        /* Date time formatter from locale */
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(
                getDateTimeStyleFromString(dateStyle),
                getTimeStyleFromString(timeStyle)).withLocale(getLocaleFromString(locale));

        /* Timezone */
        TimeZone tz = timezone == null || timezone.isEmpty() ? TimeZone.getDefault() : TimeZone.getTimeZone(timezone);

        /* Return formatted date time */
        return formatter.format(dateTime.toInstant().atZone(tz.toZoneId()).toLocalDateTime());
    }
}
