package com.example.demo1.algorithm;

import com.example.demo1.dto.response.BarVO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BarRankingAlgorithmTest {

    @Test
    void testCalculateDistance() {
        // Beijing (39.9042, 116.4074) to Shanghai (31.2304, 121.4737) approx 1068km
        double dist = BarRankingAlgorithm.calculateDistance(39.9042, 116.4074, 31.2304, 121.4737);
        assertTrue(dist > 1000 && dist < 1100);
        
        // Same point
        double zero = BarRankingAlgorithm.calculateDistance(39.9, 116.4, 39.9, 116.4);
        assertEquals(0.0, zero, 0.01);
    }

    @Test
    void testRankBars() {
        BarRankingAlgorithm.UserLocation userLoc = new BarRankingAlgorithm.UserLocation(40.0, 116.0);
        List<BarVO> bars = new ArrayList<>();

        // Bar A: Close (~1km), High Rating (5.0) -> Should be #1
        // 1 degree lat is approx 111km. 0.009 deg is approx 1km.
        BarVO barA = BarVO.builder().id(1L).name("A").latitude(40.009).longitude(116.0).avgRating(5.0).reviewCount(100).build();
        
        // Bar B: Far (~10km), Low Rating (3.0) -> Should be last
        BarVO barB = BarVO.builder().id(2L).name("B").latitude(40.09).longitude(116.0).avgRating(3.0).reviewCount(10).build();
        
        // Bar C: Medium (~5km), Medium Rating (4.0)
        BarVO barC = BarVO.builder().id(3L).name("C").latitude(40.045).longitude(116.0).avgRating(4.0).reviewCount(50).build();

        bars.add(barB);
        bars.add(barA);
        bars.add(barC);

        List<BarRankingAlgorithm.BarRecommendationResult> results = BarRankingAlgorithm.rankBars(bars, userLoc, null);

        assertEquals(3, results.size());
        assertEquals("A", results.get(0).getBar().getName());
        assertEquals("C", results.get(1).getBar().getName());
        assertEquals("B", results.get(2).getBar().getName());
        
        // Verify scores are populated
        assertTrue(results.get(0).getCompositeScore() > results.get(1).getCompositeScore());
    }
}
