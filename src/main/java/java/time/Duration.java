package java.time;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.format.DateTimeParseException;
import java.time.temporal.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.LocalTime.*;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoUnit.*;

/**
 * 一个周期,
 * 秒和纳秒
 */
public final class Duration implements TemporalAmount, Comparable<Duration>, Serializable {

    /**
     * Constant for a duration of zero.
     */
    public static final Duration ZERO = new Duration(0, 0);
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 3078945930695997490L;
    /**
     * Constant for nanos per second.
     */
    private static final BigInteger BI_NANOS_PER_SECOND = BigInteger.valueOf(NANOS_PER_SECOND);
    /**
     * The pattern for parsing.
     */
    private static final Pattern PATTERN =
            Pattern.compile("([-+]?)P(?:([-+]?[0-9]+)D)?" +
                            "(T(?:([-+]?[0-9]+)H)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)(?:[.,]([0-9]{0,9}))?S)?)?",
                    Pattern.CASE_INSENSITIVE);

    /**
     * 秒
     */
    private final long seconds;

    /**
     * 一秒内的调整时间
     */
    private final int nanos;

    /**
     * 计算 seconds 及一秒内的调整
     */
    public static Duration ofDays(long days) {
        return create(Math.multiplyExact(days, SECONDS_PER_DAY), 0);
    }

    public static Duration ofHours(long hours) {
        return create(Math.multiplyExact(hours, SECONDS_PER_HOUR), 0);
    }

    public static Duration ofMinutes(long minutes) {
        return create(Math.multiplyExact(minutes, SECONDS_PER_MINUTE), 0);
    }

    public static Duration ofSeconds(long seconds) {
        return create(seconds, 0);
    }

    public static Duration ofSeconds(long seconds, long nanoAdjustment) {
        long secs = Math.addExact(seconds, Math.floorDiv(nanoAdjustment, NANOS_PER_SECOND));
        int nos = (int) Math.floorMod(nanoAdjustment, NANOS_PER_SECOND);
        return create(secs, nos);
    }

    public static Duration ofMillis(long millis) {
        long secs = millis / 1000;
        int mos = (int) (millis % 1000);
        if (mos < 0) {
            mos += 1000;
            secs--;
        }
        return create(secs, mos * 1000_000);
    }

    public static Duration ofNanos(long nanos) {
        long secs = nanos / NANOS_PER_SECOND;
        int nos = (int) (nanos % NANOS_PER_SECOND);
        if (nos < 0) {
            nos += NANOS_PER_SECOND;
            secs--;
        }
        return create(secs, nos);
    }

    public static Duration of(long amount, TemporalUnit unit) {
        return ZERO.plus(amount, unit);
    }

    public static Duration from(TemporalAmount amount) {
        Objects.requireNonNull(amount, "amount");
        Duration duration = ZERO;
        for (TemporalUnit unit : amount.getUnits()) {
            duration = duration.plus(amount.get(unit), unit);
        }
        return duration;
    }

    public static Duration parse(CharSequence text) {
        Objects.requireNonNull(text, "text");
        Matcher matcher = PATTERN.matcher(text);
        if (matcher.matches()) {
            // check for letter T but no time sections
            if ("T".equals(matcher.group(3)) == false) {
                boolean negate = "-".equals(matcher.group(1));
                String dayMatch = matcher.group(2);
                String hourMatch = matcher.group(4);
                String minuteMatch = matcher.group(5);
                String secondMatch = matcher.group(6);
                String fractionMatch = matcher.group(7);
                if (dayMatch != null || hourMatch != null || minuteMatch != null || secondMatch != null) {
                    long daysAsSecs = parseNumber(text, dayMatch, SECONDS_PER_DAY, "days");
                    long hoursAsSecs = parseNumber(text, hourMatch, SECONDS_PER_HOUR, "hours");
                    long minsAsSecs = parseNumber(text, minuteMatch, SECONDS_PER_MINUTE, "minutes");
                    long seconds = parseNumber(text, secondMatch, 1, "seconds");
                    int nanos = parseFraction(text, fractionMatch, seconds < 0 ? -1 : 1);
                    try {
                        return create(negate, daysAsSecs, hoursAsSecs, minsAsSecs, seconds, nanos);
                    } catch (ArithmeticException ex) {
                        throw (DateTimeParseException) new DateTimeParseException("Text cannot be parsed to a Duration: overflow", text, 0).initCause(ex);
                    }
                }
            }
        }
        throw new DateTimeParseException("Text cannot be parsed to a Duration", text, 0);
    }

    private static long parseNumber(CharSequence text, String parsed, int multiplier, String errorText) {
        // regex limits to [-+]?[0-9]+
        if (parsed == null) {
            return 0;
        }
        try {
            long val = Long.parseLong(parsed);
            return Math.multiplyExact(val, multiplier);
        } catch (NumberFormatException | ArithmeticException ex) {
            throw (DateTimeParseException) new DateTimeParseException("Text cannot be parsed to a Duration: " + errorText, text, 0).initCause(ex);
        }
    }

    private static int parseFraction(CharSequence text, String parsed, int negate) {
        // regex limits to [0-9]{0,9}
        if (parsed == null || parsed.length() == 0) {
            return 0;
        }
        try {
            parsed = (parsed + "000000000").substring(0, 9);
            return Integer.parseInt(parsed) * negate;
        } catch (NumberFormatException | ArithmeticException ex) {
            throw (DateTimeParseException) new DateTimeParseException("Text cannot be parsed to a Duration: fraction", text, 0).initCause(ex);
        }
    }

    private static Duration create(boolean negate, long daysAsSecs, long hoursAsSecs, long minsAsSecs, long secs, int nanos) {
        long seconds = Math.addExact(daysAsSecs, Math.addExact(hoursAsSecs, Math.addExact(minsAsSecs, secs)));
        if (negate) {
            return ofSeconds(seconds, nanos).negated();
        }
        return ofSeconds(seconds, nanos);
    }

    //-----------------------------------------------------------------------

    public static Duration between(Temporal startInclusive, Temporal endExclusive) {
        try {
            return ofNanos(startInclusive.until(endExclusive, NANOS));
        } catch (DateTimeException | ArithmeticException ex) {
            long secs = startInclusive.until(endExclusive, SECONDS);
            long nanos;
            try {
                nanos = endExclusive.getLong(NANO_OF_SECOND) - startInclusive.getLong(NANO_OF_SECOND);
                if (secs > 0 && nanos < 0) {
                    secs++;
                } else if (secs < 0 && nanos > 0) {
                    secs--;
                }
            } catch (DateTimeException ex2) {
                nanos = 0;
            }
            return ofSeconds(secs, nanos);
        }
    }

    private static Duration create(long seconds, int nanoAdjustment) {
        if ((seconds | nanoAdjustment) == 0) {
            return ZERO;
        }
        return new Duration(seconds, nanoAdjustment);
    }

    /**
     * Constructs an instance of {@code Duration} using seconds and nanoseconds.
     *
     * @param seconds the length of the duration in seconds, positive or negative
     * @param nanos   the nanoseconds within the second, from 0 to 999,999,999
     */
    private Duration(long seconds, int nanos) {
        super();
        this.seconds = seconds;
        this.nanos = nanos;
    }

    @Override
    public long get(TemporalUnit unit) {
        if (unit == SECONDS) {
            return seconds;
        } else if (unit == NANOS) {
            return nanos;
        } else {
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
    }

    /**
     * Gets the set of units supported by this duration.
     * <p>
     * The supported units are {@link ChronoUnit#SECONDS SECONDS},
     * and {@link ChronoUnit#NANOS NANOS}.
     * They are returned in the order seconds, nanos.
     * <p>
     * This set can be used in conjunction with {@link #get(TemporalUnit)}
     * to access the entire state of the duration.
     *
     * @return a list containing the seconds and nanos units, not null
     */
    @Override
    public List<TemporalUnit> getUnits() {
        return DurationUnits.UNITS;
    }

    /**
     * Private class to delay initialization of this list until needed.
     * The circular dependency between Duration and ChronoUnit prevents
     * the simple initialization in Duration.
     */
    private static class DurationUnits {
        static final List<TemporalUnit> UNITS =
                Collections.unmodifiableList(Arrays.<TemporalUnit>asList(SECONDS, NANOS));
    }

    //-----------------------------------------------------------------------

    /**
     * Checks if this duration is zero length.
     * <p>
     * A {@code Duration} represents a directed distance between two points on
     * the time-line and can therefore be positive, zero or negative.
     * This method checks whether the length is zero.
     *
     * @return true if this duration has a total length equal to zero
     */
    public boolean isZero() {
        return (seconds | nanos) == 0;
    }

    /**
     * Checks if this duration is negative, excluding zero.
     * <p>
     * A {@code Duration} represents a directed distance between two points on
     * the time-line and can therefore be positive, zero or negative.
     * This method checks whether the length is less than zero.
     *
     * @return true if this duration has a total length less than zero
     */
    public boolean isNegative() {
        return seconds < 0;
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the number of seconds in this duration.
     * <p>
     * The length of the duration is stored using two fields - seconds and nanoseconds.
     * The nanoseconds part is a value from 0 to 999,999,999 that is an adjustment to
     * the length in seconds.
     * The total duration is defined by calling this method and {@link #getNano()}.
     * <p>
     * A {@code Duration} represents a directed distance between two points on the time-line.
     * A negative duration is expressed by the negative sign of the seconds part.
     * A duration of -1 nanosecond is stored as -1 seconds plus 999,999,999 nanoseconds.
     *
     * @return the whole seconds part of the length of the duration, positive or negative
     */
    public long getSeconds() {
        return seconds;
    }

    /**
     * Gets the number of nanoseconds within the second in this duration.
     * <p>
     * The length of the duration is stored using two fields - seconds and nanoseconds.
     * The nanoseconds part is a value from 0 to 999,999,999 that is an adjustment to
     * the length in seconds.
     * The total duration is defined by calling this method and {@link #getSeconds()}.
     * <p>
     * A {@code Duration} represents a directed distance between two points on the time-line.
     * A negative duration is expressed by the negative sign of the seconds part.
     * A duration of -1 nanosecond is stored as -1 seconds plus 999,999,999 nanoseconds.
     *
     * @return the nanoseconds within the second part of the length of the duration, from 0 to 999,999,999
     */
    public int getNano() {
        return nanos;
    }

    //-----------------------------------------------------------------------

    /**
     * Returns a copy of this duration with the specified amount of seconds.
     * <p>
     * This returns a duration with the specified seconds, retaining the
     * nano-of-second part of this duration.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param seconds the seconds to represent, may be negative
     * @return a {@code Duration} based on this period with the requested seconds, not null
     */
    public Duration withSeconds(long seconds) {
        return create(seconds, nanos);
    }

    /**
     * Returns a copy of this duration with the specified nano-of-second.
     * <p>
     * This returns a duration with the specified nano-of-second, retaining the
     * seconds part of this duration.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param nanoOfSecond the nano-of-second to represent, from 0 to 999,999,999
     * @return a {@code Duration} based on this period with the requested nano-of-second, not null
     * @throws DateTimeException if the nano-of-second is invalid
     */
    public Duration withNanos(int nanoOfSecond) {
        NANO_OF_SECOND.checkValidIntValue(nanoOfSecond);
        return create(seconds, nanoOfSecond);
    }

    //-----------------------------------------------------------------------

    /**
     * Returns a copy of this duration with the specified duration added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param duration the duration to add, positive or negative, not null
     * @return a {@code Duration} based on this duration with the specified duration added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plus(Duration duration) {
        return plus(duration.getSeconds(), duration.getNano());
    }

    /**
     * Returns a copy of this duration with the specified duration added.
     * <p>
     * The duration amount is measured in terms of the specified unit.
     * Only a subset of units are accepted by this method.
     * The unit must either have an {@linkplain TemporalUnit#isDurationEstimated() exact duration} or
     * be {@link ChronoUnit#DAYS} which is treated as 24 hours. Other units throw an exception.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param amountToAdd the amount to add, measured in terms of the unit, positive or negative
     * @param unit        the unit that the amount is measured in, must have an exact duration, not null
     * @return a {@code Duration} based on this duration with the specified duration added, not null
     * @throws UnsupportedTemporalTypeException if the unit is not supported
     * @throws ArithmeticException              if numeric overflow occurs
     */
    public Duration plus(long amountToAdd, TemporalUnit unit) {
        Objects.requireNonNull(unit, "unit");
        if (unit == DAYS) {
            return plus(Math.multiplyExact(amountToAdd, SECONDS_PER_DAY), 0);
        }
        if (unit.isDurationEstimated()) {
            throw new UnsupportedTemporalTypeException("Unit must not have an estimated duration");
        }
        if (amountToAdd == 0) {
            return this;
        }
        if (unit instanceof ChronoUnit) {
            switch ((ChronoUnit) unit) {
                case NANOS:
                    return plusNanos(amountToAdd);
                case MICROS:
                    return plusSeconds((amountToAdd / (1000_000L * 1000)) * 1000).plusNanos((amountToAdd % (1000_000L * 1000)) * 1000);
                case MILLIS:
                    return plusMillis(amountToAdd);
                case SECONDS:
                    return plusSeconds(amountToAdd);
            }
            return plusSeconds(Math.multiplyExact(unit.getDuration().seconds, amountToAdd));
        }
        Duration duration = unit.getDuration().multipliedBy(amountToAdd);
        return plusSeconds(duration.getSeconds()).plusNanos(duration.getNano());
    }

    //-----------------------------------------------------------------------

    /**
     * Returns a copy of this duration with the specified duration in standard 24 hour days added.
     * <p>
     * The number of days is multiplied by 86400 to obtain the number of seconds to add.
     * This is based on the standard definition of a day as 24 hours.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param daysToAdd the days to add, positive or negative
     * @return a {@code Duration} based on this duration with the specified days added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plusDays(long daysToAdd) {
        return plus(Math.multiplyExact(daysToAdd, SECONDS_PER_DAY), 0);
    }

    /**
     * Returns a copy of this duration with the specified duration in hours added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param hoursToAdd the hours to add, positive or negative
     * @return a {@code Duration} based on this duration with the specified hours added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plusHours(long hoursToAdd) {
        return plus(Math.multiplyExact(hoursToAdd, SECONDS_PER_HOUR), 0);
    }

    /**
     * Returns a copy of this duration with the specified duration in minutes added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param minutesToAdd the minutes to add, positive or negative
     * @return a {@code Duration} based on this duration with the specified minutes added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plusMinutes(long minutesToAdd) {
        return plus(Math.multiplyExact(minutesToAdd, SECONDS_PER_MINUTE), 0);
    }

    /**
     * Returns a copy of this duration with the specified duration in seconds added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param secondsToAdd the seconds to add, positive or negative
     * @return a {@code Duration} based on this duration with the specified seconds added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plusSeconds(long secondsToAdd) {
        return plus(secondsToAdd, 0);
    }

    /**
     * Returns a copy of this duration with the specified duration in milliseconds added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param millisToAdd the milliseconds to add, positive or negative
     * @return a {@code Duration} based on this duration with the specified milliseconds added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plusMillis(long millisToAdd) {
        return plus(millisToAdd / 1000, (millisToAdd % 1000) * 1000_000);
    }

    /**
     * Returns a copy of this duration with the specified duration in nanoseconds added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param nanosToAdd the nanoseconds to add, positive or negative
     * @return a {@code Duration} based on this duration with the specified nanoseconds added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plusNanos(long nanosToAdd) {
        return plus(0, nanosToAdd);
    }

    /**
     * Returns a copy of this duration with the specified duration added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param secondsToAdd the seconds to add, positive or negative
     * @param nanosToAdd   the nanos to add, positive or negative
     * @return a {@code Duration} based on this duration with the specified seconds added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    private Duration plus(long secondsToAdd, long nanosToAdd) {
        if ((secondsToAdd | nanosToAdd) == 0) {
            return this;
        }
        long epochSec = Math.addExact(seconds, secondsToAdd);
        epochSec = Math.addExact(epochSec, nanosToAdd / NANOS_PER_SECOND);
        nanosToAdd = nanosToAdd % NANOS_PER_SECOND;
        long nanoAdjustment = nanos + nanosToAdd;  // safe int+NANOS_PER_SECOND
        return ofSeconds(epochSec, nanoAdjustment);
    }

    //-----------------------------------------------------------------------

    /**
     * Returns a copy of this duration with the specified duration subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param duration the duration to subtract, positive or negative, not null
     * @return a {@code Duration} based on this duration with the specified duration subtracted, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minus(Duration duration) {
        long secsToSubtract = duration.getSeconds();
        int nanosToSubtract = duration.getNano();
        if (secsToSubtract == Long.MIN_VALUE) {
            return plus(Long.MAX_VALUE, -nanosToSubtract).plus(1, 0);
        }
        return plus(-secsToSubtract, -nanosToSubtract);
    }

    /**
     * Returns a copy of this duration with the specified duration subtracted.
     * <p>
     * The duration amount is measured in terms of the specified unit.
     * Only a subset of units are accepted by this method.
     * The unit must either have an {@linkplain TemporalUnit#isDurationEstimated() exact duration} or
     * be {@link ChronoUnit#DAYS} which is treated as 24 hours. Other units throw an exception.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param amountToSubtract the amount to subtract, measured in terms of the unit, positive or negative
     * @param unit             the unit that the amount is measured in, must have an exact duration, not null
     * @return a {@code Duration} based on this duration with the specified duration subtracted, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minus(long amountToSubtract, TemporalUnit unit) {
        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit));
    }

    //-----------------------------------------------------------------------

    /**
     * Returns a copy of this duration with the specified duration in standard 24 hour days subtracted.
     * <p>
     * The number of days is multiplied by 86400 to obtain the number of seconds to subtract.
     * This is based on the standard definition of a day as 24 hours.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param daysToSubtract the days to subtract, positive or negative
     * @return a {@code Duration} based on this duration with the specified days subtracted, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minusDays(long daysToSubtract) {
        return (daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract));
    }

    /**
     * Returns a copy of this duration with the specified duration in hours subtracted.
     * <p>
     * The number of hours is multiplied by 3600 to obtain the number of seconds to subtract.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param hoursToSubtract the hours to subtract, positive or negative
     * @return a {@code Duration} based on this duration with the specified hours subtracted, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minusHours(long hoursToSubtract) {
        return (hoursToSubtract == Long.MIN_VALUE ? plusHours(Long.MAX_VALUE).plusHours(1) : plusHours(-hoursToSubtract));
    }

    /**
     * Returns a copy of this duration with the specified duration in minutes subtracted.
     * <p>
     * The number of hours is multiplied by 60 to obtain the number of seconds to subtract.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param minutesToSubtract the minutes to subtract, positive or negative
     * @return a {@code Duration} based on this duration with the specified minutes subtracted, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minusMinutes(long minutesToSubtract) {
        return (minutesToSubtract == Long.MIN_VALUE ? plusMinutes(Long.MAX_VALUE).plusMinutes(1) : plusMinutes(-minutesToSubtract));
    }

    /**
     * Returns a copy of this duration with the specified duration in seconds subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param secondsToSubtract the seconds to subtract, positive or negative
     * @return a {@code Duration} based on this duration with the specified seconds subtracted, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minusSeconds(long secondsToSubtract) {
        return (secondsToSubtract == Long.MIN_VALUE ? plusSeconds(Long.MAX_VALUE).plusSeconds(1) : plusSeconds(-secondsToSubtract));
    }

    public Duration minusMillis(long millisToSubtract) {
        return (millisToSubtract == Long.MIN_VALUE ? plusMillis(Long.MAX_VALUE).plusMillis(1) : plusMillis(-millisToSubtract));
    }

    public Duration minusNanos(long nanosToSubtract) {
        return (nanosToSubtract == Long.MIN_VALUE ? plusNanos(Long.MAX_VALUE).plusNanos(1) : plusNanos(-nanosToSubtract));
    }

    public Duration multipliedBy(long multiplicand) {
        if (multiplicand == 0) {
            return ZERO;
        }
        if (multiplicand == 1) {
            return this;
        }
        return create(toSeconds().multiply(BigDecimal.valueOf(multiplicand)));
    }

    public Duration dividedBy(long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        if (divisor == 1) {
            return this;
        }
        return create(toSeconds().divide(BigDecimal.valueOf(divisor), RoundingMode.DOWN));
    }

    private BigDecimal toSeconds() {
        return BigDecimal.valueOf(seconds).add(BigDecimal.valueOf(nanos, 9));
    }

    private static Duration create(BigDecimal seconds) {
        BigInteger nanos = seconds.movePointRight(9).toBigIntegerExact();
        BigInteger[] divRem = nanos.divideAndRemainder(BI_NANOS_PER_SECOND);
        if (divRem[0].bitLength() > 63) {
            throw new ArithmeticException("Exceeds capacity of Duration: " + nanos);
        }
        return ofSeconds(divRem[0].longValue(), divRem[1].intValue());
    }

    public Duration negated() {
        return multipliedBy(-1);
    }


    public Duration abs() {
        return isNegative() ? negated() : this;
    }

    @Override
    public Temporal addTo(Temporal temporal) {
        if (seconds != 0) {
            temporal = temporal.plus(seconds, SECONDS);
        }
        if (nanos != 0) {
            temporal = temporal.plus(nanos, NANOS);
        }
        return temporal;
    }

    @Override
    public Temporal subtractFrom(Temporal temporal) {
        if (seconds != 0) {
            temporal = temporal.minus(seconds, SECONDS);
        }
        if (nanos != 0) {
            temporal = temporal.minus(nanos, NANOS);
        }
        return temporal;
    }

    public long toDays() {
        return seconds / SECONDS_PER_DAY;
    }

    public long toHours() {
        return seconds / SECONDS_PER_HOUR;
    }


    public long toMinutes() {
        return seconds / SECONDS_PER_MINUTE;
    }

    public long toMillis() {
        long millis = Math.multiplyExact(seconds, 1000);
        millis = Math.addExact(millis, nanos / 1000_000);
        return millis;
    }

    /**
     * 返回这个周期的纳秒值
     */
    public long toNanos() {
        long totalNanos = Math.multiplyExact(seconds, NANOS_PER_SECOND);
        totalNanos = Math.addExact(totalNanos, nanos);
        return totalNanos;
    }

    @Override
    public int compareTo(Duration otherDuration) {
        int cmp = Long.compare(seconds, otherDuration.seconds);
        if (cmp != 0) {
            return cmp;
        }
        return nanos - otherDuration.nanos;
    }

    @Override
    public boolean equals(Object otherDuration) {
        if (this == otherDuration) {
            return true;
        }
        if (otherDuration instanceof Duration) {
            Duration other = (Duration) otherDuration;
            return this.seconds == other.seconds &&
                    this.nanos == other.nanos;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ((int) (seconds ^ (seconds >>> 32))) + (51 * nanos);
    }

    @Override
    public String toString() {
        if (this == ZERO) {
            return "PT0S";
        }
        long hours = seconds / SECONDS_PER_HOUR;
        int minutes = (int) ((seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE);
        int secs = (int) (seconds % SECONDS_PER_MINUTE);
        StringBuilder buf = new StringBuilder(24);
        buf.append("PT");
        if (hours != 0) {
            buf.append(hours).append('H');
        }
        if (minutes != 0) {
            buf.append(minutes).append('M');
        }
        if (secs == 0 && nanos == 0 && buf.length() > 2) {
            return buf.toString();
        }
        if (secs < 0 && nanos > 0) {
            if (secs == -1) {
                buf.append("-0");
            } else {
                buf.append(secs + 1);
            }
        } else {
            buf.append(secs);
        }
        if (nanos > 0) {
            int pos = buf.length();
            if (secs < 0) {
                buf.append(2 * NANOS_PER_SECOND - nanos);
            } else {
                buf.append(nanos + NANOS_PER_SECOND);
            }
            while (buf.charAt(buf.length() - 1) == '0') {
                buf.setLength(buf.length() - 1);
            }
            buf.setCharAt(pos, '.');
        }
        buf.append('S');
        return buf.toString();
    }

    private Object writeReplace() {
        return new Ser(Ser.DURATION_TYPE, this);
    }

    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(DataOutput out) throws IOException {
        out.writeLong(seconds);
        out.writeInt(nanos);
    }

    static Duration readExternal(DataInput in) throws IOException {
        long seconds = in.readLong();
        int nanos = in.readInt();
        return Duration.ofSeconds(seconds, nanos);
    }

}
