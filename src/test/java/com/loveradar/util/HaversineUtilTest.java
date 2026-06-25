package com.loveradar.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HaversineUtilTest {

    @Test
    void distanceBetweenIdenticalPointsIsZero() {
        double distance = HaversineUtil.distanceInMeters(13.0827, 80.2707, 13.0827, 80.2707);
        assertEquals(0.0, distance, 0.001);
    }

    @Test
    void distanceBetweenKnownPointsIsApproximatelyCorrect() {
        // Roughly 1.11 km separation for ~0.01 degree latitude difference
        double distance = HaversineUtil.distanceInMeters(13.0827, 80.2707, 13.0927, 80.2707);
        assertTrue(distance > 1000 && distance < 1200,
                "Expected distance between 1000m and 1200m but was " + distance);
    }

    @Test
    void approximateDistanceLabelBucketsCorrectly() {
        assertEquals("under 100m", HaversineUtil.approximateDistanceLabel(80));
        assertEquals("under 250m", HaversineUtil.approximateDistanceLabel(200));
        assertEquals("under 500m", HaversineUtil.approximateDistanceLabel(450));
        assertEquals("under 1km", HaversineUtil.approximateDistanceLabel(900));
        assertEquals("a few kilometers away", HaversineUtil.approximateDistanceLabel(3000));
    }

    @Test
    void roundToApproxCellRoundsToTwoDecimals() {
        assertEquals(13.08, HaversineUtil.roundToApproxCell(13.0827));
        assertEquals(80.27, HaversineUtil.roundToApproxCell(80.2707));
    }
}
