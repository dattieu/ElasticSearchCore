package com.wse.common.elasticsearch.helper;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;

public final class DateFormatter {

    private static final String OUTPUT_DATE_FORMAT = "yyyy-MM-dd";
    private static final String INPUT_DATE_FORMAT = "E MMM dd HH:mm:ss Z yyyy";
    
    public static final Date parseDate(String dateString, String inputDateFormat) {
        DateFormat format = new SimpleDateFormat(getInputDateFormat(inputDateFormat));
        try {
            return format.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }
    
    public static final String fromDateToJoda(String dateString) {
        return toDateString(parse(dateString));
    }
    
    public static final String fromDateToJoda(String dateString, String inputDateFormat, String outputDateFormat) {
        return toDateString(parse(dateString, inputDateFormat), outputDateFormat);
    }
    
    public static final DateTime parse(String dateString, String inputDateFormat) {
        DateTimeFormatter jodaFormatter = DateTimeFormat.forPattern(getInputDateFormat(inputDateFormat));
        try {
            return jodaFormatter.parseDateTime(dateString);
        }
        catch (IllegalArgumentException exception) {
            DateFormat dateFormatter = new SimpleDateFormat(getInputDateFormat(inputDateFormat));
            try {
                return new DateTime(dateFormatter.parse(dateString));
            } catch (ParseException e) {
                return null;
            }
        }
    }
    
    public static final DateTime parse(String dateString) {
        return parse(dateString, INPUT_DATE_FORMAT);
    }
    
    public static final String toDateString(DateTime date, String outputDateFormat) {
        return date.toString(getOutputDateFormat(outputDateFormat));
    }
    
    public static final String toDateString(DateTime date) {
        return toDateString(date, getOutputDateFormat(OUTPUT_DATE_FORMAT));
    }
    
    private static final String getOutputDateFormat(String dateFormat) {
        return StringUtils.isEmpty(dateFormat) ? OUTPUT_DATE_FORMAT : dateFormat;
    }
    
    private static final String getInputDateFormat(String dateFormat) {
        return StringUtils.isEmpty(dateFormat) ? INPUT_DATE_FORMAT : dateFormat;
    }
}
