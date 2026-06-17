package com.gmeo.finance_tracker.common.util;

import com.gmeo.finance_tracker.common.exception.BadRequestException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class DateTimeUtils {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM")
            .withResolverStyle(ResolverStyle.STRICT);

    private DateTimeUtils() {
    }

    public static LocalDate parseMonthStart(String month) {
        try {
            YearMonth yearMonth = YearMonth.parse(month, MONTH_FORMATTER);
            if (yearMonth.getYear() < 1) {
                throw new DateTimeParseException("Invalid year", month, 0);
            }
            return yearMonth.atDay(1);
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("month must use YYYY-MM format");
        }
    }

    public static String formatMonth(LocalDate month) {
        return YearMonth.from(month).format(MONTH_FORMATTER);
    }
}
