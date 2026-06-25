package com.loveradar.util;

/**
 * Utility for calculating great-circle distances between two
 * latitude/longitude points using the Haversine formula.
 */
public final class HaversineUtil {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private HaversineUtil() {
    }

    /**
     * Calculates the distance, in meters, between two GPS coordinates.
     */
    public static double distanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Rounds a coordinate to a coarse "cell" used for privacy-preserving
     * approximate location display (~1.1km grid at the equator).
     */
    public static double roundToApproxCell(double coordinate) {
        return Math.round(coordinate * 100.0) / 100.0;
    }

    /**
     * Buckets a raw distance in meters into a human-friendly approximate
     * range so exact distances are never exposed.
     */
    public static String approximateDistanceLabel(double distanceMeters) {
        if (distanceMeters < 50) {
            return "very close (under 50m)";
        } else if (distanceMeters < 100) {
            return "under 100m";
        } else if (distanceMeters < 250) {
            return "under 250m";
        } else if (distanceMeters < 500) {
            return "under 500m";
        } else if (distanceMeters < 1000) {
            return "under 1km";
        } else if (distanceMeters < 5000) {
            return "a few kilometers away";
        } else {
            return "far away";
        }
    }
}
