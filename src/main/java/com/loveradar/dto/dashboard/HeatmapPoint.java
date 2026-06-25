package com.loveradar.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a coarse-grained ("rounded") location cell and how many
 * proximity alerts have occurred near it. Coordinates are rounded to
 * roughly a ~1.1km grid so exact routes/locations are never exposed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapPoint {
    private double latCell;
    private double lngCell;
    private long count;
}
