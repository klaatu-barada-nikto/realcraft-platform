package com.realcraft.platform.street.core.geo;

import com.realcraft.platform.street.model.ChunkCoord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoMathUtilTest {

    @Test
    void geodeticToEcefAtEquatorOrigin() {
        double[] ecef = GeoMathUtil.geodeticToEcef(0, 0, 0);
        assertEquals(6378137.0, ecef[0], 1.0);
        assertEquals(0.0, ecef[1], 1.0);
        assertEquals(0.0, ecef[2], 1.0);
    }

    @Test
    void enuBasisIsOrthonormal() {
        double[][] basis = GeoMathUtil.enuBasis(30, 60);
        for (double[] v : basis) {
            double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            assertEquals(1.0, len, 1e-9);
        }
        assertEquals(0.0, dot(basis[0], basis[1]), 1e-9);
        assertEquals(0.0, dot(basis[1], basis[2]), 1e-9);
        assertEquals(0.0, dot(basis[0], basis[2]), 1e-9);
    }

    @Test
    void ecefToEnuAtOriginIsZero() {
        double[] origin = GeoMathUtil.geodeticToEcef(0, 0, 0);
        double[][] basis = GeoMathUtil.enuBasis(0, 0);
        double[] enu = GeoMathUtil.ecefToEnu(origin, origin, basis);
        assertEquals(0.0, enu[0], 1e-6);
        assertEquals(0.0, enu[1], 1e-6);
        assertEquals(0.0, enu[2], 1e-6);
    }

    @Test
    void computeGridSingleChunkForSmallRadius() {
        List<ChunkCoord> grid = GeoMathUtil.computeGrid(0, 0, 8, 16);
        assertEquals(1, grid.size());
        assertEquals(0, grid.get(0).chunkX());
        assertEquals(0, grid.get(0).chunkZ());
        assertEquals(-8.0, grid.get(0).originEast(), 1e-9);
        assertEquals(-8.0, grid.get(0).originNorth(), 1e-9);
    }

    @Test
    void computeGridCountsChunksWithinRadius() {
        List<ChunkCoord> grid = GeoMathUtil.computeGrid(0, 0, 16, 16);
        assertEquals(5, grid.size());
    }

    @Test
    void computeGridRespectsTileSize() {
        List<ChunkCoord> grid = GeoMathUtil.computeGrid(0, 0, 40, 16);
        assertTrue(grid.size() > 5);
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }
}