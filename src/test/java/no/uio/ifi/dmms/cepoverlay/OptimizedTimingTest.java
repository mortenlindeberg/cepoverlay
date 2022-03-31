package no.uio.ifi.dmms.cepoverlay;

import org.junit.Test;

public class OptimizedTimingTest {

    @Test
    public void testOptimizedTiming() {
        /* All numbers in milliseconds, except rate which is in hertz */

        long nextContextChange = 1200;
        long timestamp = 1000;
        int futureWindow = 200;
        double rate = 100;
        int migrationTime = 200;
        int windowSize = 1000;
        long lastFlush = 800;

        double stateSizeScore = stateSizeScore(1300, lastFlush, windowSize);
        System.out.println("State score: "+stateSizeScore);


        double placementPenalty = placementPenalty(1300, nextContextChange);
        System.out.println("Placement penalty: "+placementPenalty);

        long flushPenalty = flushPenalty(1300, migrationTime, lastFlush, windowSize);
        System.out.println("Flush penalty: "+flushPenalty);


    }

    private long flushPenalty( int scheduledMigration, int migrationTime, long lastFlush, int windowSize) {
        long effectiveFlush = lastFlush;
        while (scheduledMigration > (effectiveFlush + windowSize))
            effectiveFlush = effectiveFlush + windowSize;
        long mt = scheduledMigration;
        long me = (scheduledMigration + migrationTime);

        if (effectiveFlush > mt && effectiveFlush < me)
            return (me-scheduledMigration);
        else return 0;

    }

    public double stateSizeScore(long scheduledMigration, long lastFlush, int windowSize) {
        long effectiveFlush = lastFlush;

        while (scheduledMigration > (effectiveFlush + windowSize))
            effectiveFlush = effectiveFlush + windowSize;
        System.out.println("Effective flush: " +effectiveFlush+" "+scheduledMigration+" "+effectiveFlush+" "+windowSize);
        return  1 - ((double)(scheduledMigration - effectiveFlush) / windowSize);
    }

    public double placementPenalty(long scheduledMigration, long nextContextChange) {
        return (double) Math.abs(nextContextChange - scheduledMigration);
    }
}
