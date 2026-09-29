package com.loanservicing.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * The application's clock. Services call LocalDate.now(clock) instead of LocalDate.now().
 *
 * Why? Late fees and defaults depend on dates. In local development you can
 * "time travel" (POST /api/v1/dev/clock/advance?days=40) to test them without waiting.
 * In production the offset is simply always zero.
 */
public class AppClock extends Clock {

    private final Clock base;
    private volatile Duration offset = Duration.ZERO;

    public AppClock(Clock base) {
        this.base = base;
    }

    public void advanceDays(long days) {
        offset = offset.plusDays(days);
    }

    public void reset() {
        offset = Duration.ZERO;
    }

    public long offsetDays() {
        return offset.toDays();
    }

    @Override
    public ZoneId getZone() {
        return base.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        AppClock copy = new AppClock(base.withZone(zone));
        copy.offset = this.offset;
        return copy;
    }

    @Override
    public Instant instant() {
        return base.instant().plus(offset);
    }
}
