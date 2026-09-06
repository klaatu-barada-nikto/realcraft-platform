package com.realcraft.platform.street.core.geo;

import com.realcraft.platform.street.model.ChunkCoord;

import java.util.ArrayList;
import java.util.List;

/**
 * 地理坐标计算工具：经纬度 ↔ ECEF 转换、ENU 局部切平面基向量、区块网格划分。
 *
 * <p>体素化在任务中心的局部 ENU（东-北-天）切平面上进行：东对应 Minecraft X 轴、
 * 天对应 Y 轴（高度）、北对应 Z 轴，从而将全球 ECEF 几何映射为区块内相对整数坐标。</p>
 */
public final class GeoMathUtil {

    private static final double EARTH_RADIUS = 6378137.0;
    private static final double ECCENTRICITY_SQ = 6.69437999014e-3;

    private GeoMathUtil() {
    }

    /**
     * WGS84 经纬度（度）+ 高度（米）转 ECEF 坐标，返回 [x, y, z]（米）。
     */
    public static double[] geodeticToEcef(double latDeg, double lonDeg, double height) {
        double lat = Math.toRadians(latDeg);
        double lon = Math.toRadians(lonDeg);
        double sinLat = Math.sin(lat);
        double cosLat = Math.cos(lat);
        double n = EARTH_RADIUS / Math.sqrt(1 - ECCENTRICITY_SQ * sinLat * sinLat);
        double x = (n + height) * cosLat * Math.cos(lon);
        double y = (n + height) * cosLat * Math.sin(lon);
        double z = (n * (1 - ECCENTRICITY_SQ) + height) * sinLat;
        return new double[]{x, y, z};
    }

    /**
     * 计算以 (latDeg, lonDeg) 为原点的 ENU 基向量（东、北、天），返回 3x3 数组，各行依次为 east/north/up。
     */
    public static double[][] enuBasis(double latDeg, double lonDeg) {
        double lat = Math.toRadians(latDeg);
        double lon = Math.toRadians(lonDeg);
        double sinLat = Math.sin(lat);
        double cosLat = Math.cos(lat);
        double sinLon = Math.sin(lon);
        double cosLon = Math.cos(lon);
        double[] east = {-sinLon, cosLon, 0};
        double[] north = {-sinLat * cosLon, -sinLat * sinLon, cosLat};
        double[] up = {cosLat * cosLon, cosLat * sinLon, sinLat};
        return new double[][]{east, north, up};
    }

    /**
     * ECEF 点相对原点 ECEF 的 ENU 偏移，返回 [east, north, up]（米）。
     */
    public static double[] ecefToEnu(double[] ecef, double[] originEcef, double[][] basis) {
        double dx = ecef[0] - originEcef[0];
        double dy = ecef[1] - originEcef[1];
        double dz = ecef[2] - originEcef[2];
        return new double[]{
                dot(basis[0], dx, dy, dz),
                dot(basis[1], dx, dy, dz),
                dot(basis[2], dx, dy, dz)
        };
    }

    /**
     * 将半径范围内的区域按 tileSize（默认 16 米）划分为二维网格，生成区块列表。
     * 区块 (0,0) 中心对齐任务中心，仅保留区块中心落入半径圆内的区块。
     */
    public static List<ChunkCoord> computeGrid(double latDeg, double lonDeg, double radiusMeters, double tileSize) {
        int n = (int) Math.ceil(radiusMeters / tileSize);
        List<ChunkCoord> chunks = new ArrayList<>();
        for (int cz = -n; cz <= n; cz++) {
            for (int cx = -n; cx <= n; cx++) {
                double centerEast = cx * tileSize;
                double centerNorth = cz * tileSize;
                if (Math.hypot(centerEast, centerNorth) <= radiusMeters) {
                    double originEast = (cx - 0.5) * tileSize;
                    double originNorth = (cz - 0.5) * tileSize;
                    chunks.add(new ChunkCoord(cx, cz, originEast, originNorth));
                }
            }
        }
        return chunks;
    }

    private static double dot(double[] v, double x, double y, double z) {
        return v[0] * x + v[1] * y + v[2] * z;
    }
}