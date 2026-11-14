package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.validation.ValidationResult

import java.time.YearMonth
import java.util.regex.Matcher

/**
 * Validates ISO 8601 date, time and date/time values against ADL 1.4 date/time patterns
 * (C_DATE.pattern, C_TIME.pattern, C_DATE_TIME.pattern).
 *
 * Each component of an ADL pattern after the year can be:
 *  - required:  yyyy, mm, dd, hh, mm, ss (letters, case insensitive)
 *  - optional:  ??
 *  - forbidden: XX
 * e.g. 'yyyy-mm-??' allows a full date or a date without the day, and 'yyyy-??-XX' allows
 * a year or a year and month, but never the day.
 *
 * When there is no pattern (null or empty), any valid ISO 8601 value is allowed, including
 * partial values (e.g. '2024' or '2024-10' for a date, '10' or '10:30' for a time). That is the
 * constraint used for date/time nodes that the OPT doesn't constrain (e.g. injected by complete()).
 *
 * The value must be a valid ISO 8601 value in its whole: extended (2024-10-17T10:30:00) or
 * basic (20241017T103000) format (not mixed), components in range (no 2024-02-30), optional
 * seconds fraction with '.' or ',', and optional time zone (Z, +hh, +hhmm, +hh:mm). Trailing
 * characters are not allowed.
 */
class DateTimePatternValidator {

   static final String REQUIRED  = 'required'
   static final String OPTIONAL  = 'optional'
   static final String FORBIDDEN = 'forbidden'

   // ADL 1.4 patterns, the year is always required
   private static final def DATE_PATTERN      = ~/(?i)^yyyy-(mm|\?\?|XX)-(dd|\?\?|XX)$/
   private static final def TIME_PATTERN      = ~/(?i)^(hh|\?\?|XX):(mm|\?\?|XX):(ss|\?\?|XX)$/
   private static final def DATE_TIME_PATTERN = ~/(?i)^yyyy-(mm|\?\?|XX)-(dd|\?\?|XX)T(hh|\?\?|XX):(mm|\?\?|XX):(ss|\?\?|XX)$/

   // ISO 8601 values, extended and basic formats
   private static final String TZ = /(Z|[+-]\d{2}(?::?\d{2})?)?/

   private static final def DATE_EXTENDED = ~/^(\d{4})(?:-(\d{2})(?:-(\d{2}))?)?$/
   private static final def DATE_BASIC    = ~/^(\d{4})(?:(\d{2})(\d{2})?)?$/

   private static final def TIME_EXTENDED = ~("^(\\d{2})(?::(\\d{2})(?::(\\d{2})(?:[.,](\\d+))?)?)?"+ TZ +'$')
   private static final def TIME_BASIC    = ~("^(\\d{2})(?:(\\d{2})(?:(\\d{2})(?:[.,](\\d+))?)?)?"+ TZ +'$')

   private static final def DATE_TIME_EXTENDED = ~("^(\\d{4})(?:-(\\d{2})(?:-(\\d{2})(?:T(\\d{2})(?::(\\d{2})(?::(\\d{2})(?:[.,](\\d+))?)?)?"+ TZ +")?)?)?\$")
   private static final def DATE_TIME_BASIC    = ~("^(\\d{4})(?:(\\d{2})(?:(\\d{2})(?:T(\\d{2})(?:(\\d{2})(?:(\\d{2})(?:[.,](\\d+))?)?)?"+ TZ +")?)?)?\$")


   static ValidationResult validateDate(String value, String pattern)
   {
      List rules = rulesFor(pattern, DATE_PATTERN, [REQUIRED, OPTIONAL, OPTIONAL])
      if (rules == null) return unsupportedPattern(pattern, 'date')

      List components = parse(value, [DATE_EXTENDED, DATE_BASIC], 3)
      if (components == null || !validRanges(components))
      {
         return new ValidationResult(isValid: false, message: "value '${value}' is not a valid ISO 8601 date")
      }

      return check(value, pattern, components, rules, 'date')
   }

   static ValidationResult validateTime(String value, String pattern)
   {
      List rules = rulesFor(pattern, TIME_PATTERN, [REQUIRED, OPTIONAL, OPTIONAL])
      if (rules == null) return unsupportedPattern(pattern, 'time')

      // time components are at the same positions as in a date time, after the date
      List components = parse(value, [TIME_EXTENDED, TIME_BASIC], 4)
      if (components == null || !validRanges([null, null, null] + components))
      {
         return new ValidationResult(isValid: false, message: "value '${value}' is not a valid ISO 8601 time")
      }

      return check(value, pattern, components.take(3), rules, 'time')
   }

   static ValidationResult validateDateTime(String value, String pattern)
   {
      List rules = rulesFor(pattern, DATE_TIME_PATTERN, [REQUIRED, OPTIONAL, OPTIONAL, OPTIONAL, OPTIONAL, OPTIONAL])
      if (rules == null) return unsupportedPattern(pattern, 'datetime')

      List components = parse(value, [DATE_TIME_EXTENDED, DATE_TIME_BASIC], 7)
      if (components == null || !validRanges(components))
      {
         return new ValidationResult(isValid: false, message: "value '${value}' is not a valid ISO 8601 datetime")
      }

      return check(value, pattern, components.take(6), rules, 'datetime')
   }

   /**
    * Rules (required/optional/forbidden) for each component of the pattern, the year is always
    * required. Returns the defaults for an empty pattern and null for an unsupported pattern.
    */
   static List rulesFor(String pattern, def regex, List defaults)
   {
      if (!pattern?.trim()) return defaults

      Matcher m = (pattern.trim() =~ regex)
      if (!m.matches()) return null

      List rules = [REQUIRED] // year
      for (int i = 1; i <= m.groupCount(); i++)
      {
         rules << rule(m.group(i))
      }

      // time patterns don't have the year
      if (regex.is(TIME_PATTERN)) rules = rules.drop(1)

      // a component can't be required after an optional or forbidden one, and nothing can be
      // allowed after a forbidden one, since ISO 8601 partial values only omit the lower components
      for (int i = 1; i < rules.size(); i++)
      {
         if (rules[i] == REQUIRED && rules[i-1] != REQUIRED) return null
         if (rules[i] != FORBIDDEN && rules[i-1] == FORBIDDEN) return null
      }

      return rules
   }

   private static String rule(String component)
   {
      if (component == '??') return OPTIONAL
      if (component.equalsIgnoreCase('XX')) return FORBIDDEN
      return REQUIRED
   }

   // Tries each format, returns the matched groups as Integers (null for missing components),
   // the fraction (if any) is the last group and is returned as a String
   private static List parse(String value, List formats, int groups)
   {
      if (value == null) return null

      for (def format : formats)
      {
         Matcher m = (value =~ format)
         if (m.matches())
         {
            List components = []
            for (int i = 1; i <= groups; i++)
            {
               components << m.group(i)
            }

            // time zone is the group after the components and fraction (dates don't have it)
            String tz = m.groupCount() > groups ? m.group(groups + 1) : null
            if (tz && tz != 'Z' && !validTimeZone(tz)) return null

            // all the components are numbers, except the seconds fraction that is kept as a String
            boolean hasFraction = formats.size() > 0 && !formats[0].is(DATE_EXTENDED)
            return components.withIndex().collect { c, i ->
               (c != null && !(hasFraction && i == groups - 1)) ? Integer.parseInt(c) : c
            }
         }
      }

      return null
   }

   private static boolean validTimeZone(String tz)
   {
      String digits = tz.substring(1).replace(':', '')
      int hours = Integer.parseInt(digits.substring(0, 2))
      int minutes = digits.length() > 2 ? Integer.parseInt(digits.substring(2)) : 0
      return hours <= 23 && minutes <= 59
   }

   // components: year, month, day, hour, minute, second (any can be null)
   private static boolean validRanges(List c)
   {
      Integer year = c[0], month = c[1], day = c[2], hour = c[3], minute = c[4], second = c[5]

      if (month != null && (month < 1 || month > 12)) return false
      if (day != null && (day < 1 || day > YearMonth.of(year, month).lengthOfMonth())) return false
      if (hour != null && hour > 23) return false
      if (minute != null && minute > 59) return false
      if (second != null && second > 59) return false

      return true
   }

   private static ValidationResult check(String value, String pattern, List components, List rules, String kind)
   {
      for (int i = 0; i < rules.size(); i++)
      {
         if (rules[i] == REQUIRED && components[i] == null)
         {
            return new ValidationResult(isValid: false, message: "value '${value}' is missing components required by the ${kind} pattern '${pattern}'")
         }
         if (rules[i] == FORBIDDEN && components[i] != null)
         {
            return new ValidationResult(isValid: false, message: "value '${value}' has components not allowed by the ${kind} pattern '${pattern}'")
         }
      }

      return new ValidationResult(isValid: true)
   }

   private static ValidationResult unsupportedPattern(String pattern, String kind)
   {
      return new ValidationResult(isValid: false, message: "the ${kind} pattern '${pattern}' is not a valid ADL ${kind} pattern")
   }
}
