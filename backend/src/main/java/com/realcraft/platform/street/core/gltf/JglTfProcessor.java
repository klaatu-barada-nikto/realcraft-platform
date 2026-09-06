package com.realcraft.platform.street.core.gltf;

import com.realcraft.platform.common.BusinessException;
import com.realcraft.platform.common.ErrorCode;
import com.realcraft.platform.domain.VoxelBlock;
import com.realcraft.platform.street.config.StreetProperties;
import com.realcraft.platform.street.core.geo.GeoMathUtil;
import com.realcraft.platform.street.model.ChunkCoord;
import com.realcraft.platform.street.model.StreetTask;
import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AccessorIntData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.BufferViewModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.ImageModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.io.GltfModelReader;
import de.javagl.jgltf.model.v2.MaterialModelV2;

import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JglTfProcessor {

    public static final String DEFAULT_BLOCK_ID = "minecraft:stone";
    private static final int GL_TRIANGLES = 4;

    private final StreetProperties props;

    public JglTfProcessor(StreetProperties props) {
        this.props = props;
    }

    public List<VoxelBlock> voxelize(byte[] glb, StreetTask task, ChunkCoord coord) {
        GltfModel model = readModel(glb);
        double[] originEcef = GeoMathUtil.geodeticToEcef(task.centerLat(), task.centerLon(), 0);
        double[][] basis = GeoMathUtil.enuBasis(task.centerLat(), task.centerLon());
        List<Triangle> triangles = collectTriangles(model, originEcef, basis);
        return voxelizeChunk(triangles, coord);
    }

    private GltfModel readModel(byte[] glb) {
        try (InputStream in = new ByteArrayInputStream(glb)) {
            return new GltfModelReader().readWithoutReferences(in);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNPROCESSABLE, "GLB 解析失败");
        }
    }

    private List<Triangle> collectTriangles(GltfModel model, double[] originEcef, double[][] basis) {
        List<Triangle> result = new ArrayList<>();
        Map<TextureModel, BufferedImage> textureImages = buildTextureImages(model);
        List<SceneModel> scenes = model.getSceneModels();
        if (scenes != null && !scenes.isEmpty()) {
            for (SceneModel scene : scenes) {
                for (NodeModel root : scene.getNodeModels()) {
                    processNode(root, new Matrix4d(), originEcef, basis, textureImages, result);
                }
            }
        } else {
            for (NodeModel node : model.getNodeModels()) {
                processNode(node, new Matrix4d(), originEcef, basis, textureImages, result);
            }
        }
        return result;
    }

    private void processNode(NodeModel node, Matrix4d parent, double[] originEcef, double[][] basis,
                             Map<TextureModel, BufferedImage> textureImages, List<Triangle> result) {
        Matrix4d global = new Matrix4d(parent).mul(nodeLocalMatrix(node));
        for (MeshModel mesh : node.getMeshModels()) {
            for (MeshPrimitiveModel primitive : mesh.getMeshPrimitiveModels()) {
                if (primitive.getMode() != GL_TRIANGLES) {
                    continue;
                }
                extractPrimitive(primitive, global, originEcef, basis, textureImages, result);
            }
        }
        for (NodeModel child : node.getChildren()) {
            processNode(child, global, originEcef, basis, textureImages, result);
        }
    }

    private Matrix4d nodeLocalMatrix(NodeModel node) {
        float[] matrix = node.getMatrix();
        if (matrix != null && matrix.length == 16) {
            return new Matrix4d().set(
                    matrix[0], matrix[4], matrix[8], matrix[12],
                    matrix[1], matrix[5], matrix[9], matrix[13],
                    matrix[2], matrix[6], matrix[10], matrix[14],
                    matrix[3], matrix[7], matrix[11], matrix[15]);
        }
        Matrix4d m = new Matrix4d();
        float[] t = node.getTranslation();
        if (t != null && (t[0] != 0 || t[1] != 0 || t[2] != 0)) {
            m.translate(t[0], t[1], t[2]);
        }
        float[] r = node.getRotation();
        if (r != null && r.length == 4 && (r[0] != 0 || r[1] != 0 || r[2] != 0 || r[3] != 1)) {
            m.rotate(new Quaterniond(r[0], r[1], r[2], r[3]));
        }
        float[] s = node.getScale();
        if (s != null && (s[0] != 1 || s[1] != 1 || s[2] != 1)) {
            m.scale(s[0], s[1], s[2]);
        }
        return m;
    }

    private void extractPrimitive(MeshPrimitiveModel primitive, Matrix4d global, double[] originEcef, double[][] basis,
                                  Map<TextureModel, BufferedImage> textureImages, List<Triangle> result) {
        AccessorModel position = primitive.getAttributes().get("POSITION");
        if (position == null) {
            return;
        }
        AccessorData positionData = position.getAccessorData();
        if (!(positionData instanceof AccessorFloatData floatPos)) {
            return;
        }
        int vertexCount = position.getCount();
        double[][] local = new double[vertexCount][3];
        for (int i = 0; i < vertexCount; i++) {
            Vector3d world = global.transformPosition(floatPos.get(i, 0), floatPos.get(i, 1), floatPos.get(i, 2), new Vector3d());
            double[] enu = GeoMathUtil.ecefToEnu(new double[]{world.x, world.y, world.z}, originEcef, basis);
            local[i][0] = enu[0];
            local[i][1] = enu[2];
            local[i][2] = enu[1];
        }

        AccessorFloatData floatUv = null;
        AccessorModel texcoord = primitive.getAttributes().get("TEXCOORD_0");
        if (texcoord != null && texcoord.getAccessorData() instanceof AccessorFloatData uv) {
            floatUv = uv;
        }

        BufferedImage texture = null;
        MaterialModel material = primitive.getMaterialModel();
        if (material instanceof MaterialModelV2 v2) {
            TextureModel baseColor = v2.getBaseColorTexture();
            if (baseColor != null) {
                texture = textureImages.get(baseColor);
            }
        }

        AccessorModel indices = primitive.getIndices();
        if (indices == null) {
            for (int i = 0; i + 2 < vertexCount; i += 3) {
                result.add(triangle(local, i, i + 1, i + 2, floatUv, texture));
            }
        } else {
            AccessorData indexData = indices.getAccessorData();
            if (!(indexData instanceof AccessorIntData intIdx)) {
                return;
            }
            int indexCount = indices.getCount();
            for (int i = 0; i + 2 < indexCount; i += 3) {
                result.add(triangle(local, intIdx.get(i, 0), intIdx.get(i + 1, 0), intIdx.get(i + 2, 0), floatUv, texture));
            }
        }
    }

    private Triangle triangle(double[][] verts, int a, int b, int c, AccessorFloatData uv, BufferedImage texture) {
        float[] uv0 = uv != null ? new float[]{uv.get(a, 0), uv.get(a, 1)} : null;
        float[] uv1 = uv != null ? new float[]{uv.get(b, 0), uv.get(b, 1)} : null;
        float[] uv2 = uv != null ? new float[]{uv.get(c, 0), uv.get(c, 1)} : null;
        return new Triangle(verts[a], verts[b], verts[c], uv0, uv1, uv2, texture);
    }

    private List<VoxelBlock> voxelizeChunk(List<Triangle> triangles, ChunkCoord coord) {
        List<VoxelBlock> blocks = new ArrayList<>();
        if (triangles.isEmpty()) {
            return blocks;
        }
        int tileSize = props.getTileSize();
        int minY = props.getMinY();
        int maxY = props.getMaxY();
        double chunkMinX = coord.originEast();
        double chunkMinZ = coord.originNorth();
        double chunkMaxX = chunkMinX + tileSize;
        double chunkMaxZ = chunkMinZ + tileSize;

        List<Triangle> clipped = new ArrayList<>();
        for (Triangle tri : triangles) {
            if (triangleAabbIntersects(tri, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ)) {
                clipped.add(tri);
            }
        }
        if (clipped.isEmpty()) {
            return blocks;
        }

        for (int x = 0; x < tileSize; x++) {
            for (int z = 0; z < tileSize; z++) {
                for (int y = minY; y < maxY; y++) {
                    double vMinX = chunkMinX + x;
                    double vMinY = y;
                    double vMinZ = chunkMinZ + z;
                    for (Triangle tri : clipped) {
                        if (aabbTriangleIntersects(
                                vMinX, vMinY, vMinZ, vMinX + 1, vMinY + 1, vMinZ + 1,
                                tri.v0()[0], tri.v0()[1], tri.v0()[2],
                                tri.v1()[0], tri.v1()[1], tri.v1()[2],
                                tri.v2()[0], tri.v2()[1], tri.v2()[2])) {
                            blocks.add(new VoxelBlock(resolveBlockId(tri, vMinX, vMinY, vMinZ), x, y, z));
                            break;
                        }
                    }
                }
            }
        }
        return blocks;
    }

    private String resolveBlockId(Triangle tri, double vMinX, double vMinY, double vMinZ) {
        if (tri.texture() == null || tri.uv0() == null) {
            return DEFAULT_BLOCK_ID;
        }
        double cx = vMinX + 0.5;
        double cy = vMinY + 0.5;
        double cz = vMinZ + 0.5;
        double[] bary = barycentric(tri, cx, cy, cz);
        double b0 = clamp01(bary[0]);
        double b1 = clamp01(bary[1]);
        double b2 = clamp01(bary[2]);
        double sum = b0 + b1 + b2;
        if (sum <= 0) {
            return DEFAULT_BLOCK_ID;
        }
        b0 /= sum;
        b1 /= sum;
        b2 /= sum;
        double u = b0 * tri.uv0()[0] + b1 * tri.uv1()[0] + b2 * tri.uv2()[0];
        double v = b0 * tri.uv0()[1] + b1 * tri.uv1()[1] + b2 * tri.uv2()[1];
        int rgb = sampleRgb(tri.texture(), u, v);
        if (rgb < 0) {
            return DEFAULT_BLOCK_ID;
        }
        return mapColorToBlockId((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    private boolean triangleAabbIntersects(Triangle tri, double minX, double minZ, double maxX, double maxZ) {
        double txMin = Math.min(tri.v0()[0], Math.min(tri.v1()[0], tri.v2()[0]));
        double txMax = Math.max(tri.v0()[0], Math.max(tri.v1()[0], tri.v2()[0]));
        double tzMin = Math.min(tri.v0()[2], Math.min(tri.v1()[2], tri.v2()[2]));
        double tzMax = Math.max(tri.v0()[2], Math.max(tri.v1()[2], tri.v2()[2]));
        return txMax >= minX && txMin <= maxX && tzMax >= minZ && tzMin <= maxZ;
    }

    private boolean aabbTriangleIntersects(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            double t0x, double t0y, double t0z,
            double t1x, double t1y, double t1z,
            double t2x, double t2y, double t2z) {
        double cx = (minX + maxX) * 0.5;
        double cy = (minY + maxY) * 0.5;
        double cz = (minZ + maxZ) * 0.5;
        double hx = (maxX - minX) * 0.5;
        double hy = (maxY - minY) * 0.5;
        double hz = (maxZ - minZ) * 0.5;

        double v0x = t0x - cx;
        double v0y = t0y - cy;
        double v0z = t0z - cz;
        double v1x = t1x - cx;
        double v1y = t1y - cy;
        double v1z = t1z - cz;
        double v2x = t2x - cx;
        double v2y = t2y - cy;
        double v2z = t2z - cz;

        double tMinX = Math.min(v0x, Math.min(v1x, v2x));
        double tMaxX = Math.max(v0x, Math.max(v1x, v2x));
        if (tMaxX < -hx || tMinX > hx) {
            return false;
        }
        double tMinY = Math.min(v0y, Math.min(v1y, v2y));
        double tMaxY = Math.max(v0y, Math.max(v1y, v2y));
        if (tMaxY < -hy || tMinY > hy) {
            return false;
        }
        double tMinZ = Math.min(v0z, Math.min(v1z, v2z));
        double tMaxZ = Math.max(v0z, Math.max(v1z, v2z));
        if (tMaxZ < -hz || tMinZ > hz) {
            return false;
        }

        double e0x = v1x - v0x;
        double e0y = v1y - v0y;
        double e0z = v1z - v0z;
        double e1x = v2x - v1x;
        double e1y = v2y - v1y;
        double e1z = v2z - v1z;
        double e2x = v0x - v2x;
        double e2y = v0y - v2y;
        double e2z = v0z - v2z;

        return !separatingAxis(v0x, v0y, v0z, v1x, v1y, v1z, v2x, v2y, v2z, 0, -e0z, e0y, hx, hy, hz)
                && !separatingAxis(v0x, v0y, v0z, v1x, v1y, v1z, v2x, v2y, v2z, 0, -e1z, e1y, hx, hy, hz)
                && !separatingAxis(v0x, v0y, v0z, v1x, v1y, v1z, v2x, v2y, v2z, 0, -e2z, e2y, hx, hy, hz)
                && !separatingAxis(v0x, v0y, v0z, v1x, v1y, v1z, v2x, v2y, v2z, e0z, 0, -e0x, hx, hy, hz)
                && !separatingAxis(v0x, v0y, v0z, v1x, v1y, v1z, v2x, v2y, v2z, e1z, 0, -e1x, hx, hy, hz)
                && !separatingAxis(v0x, v0y, v0z, v1x, v1y, v1z, v2x, v2y, v2z, e2z, 0, -e2x, hx, hy, hz)
                && !separatingAxis(v0x, v0y, v0z, v1x, v1y, v1z, v2x, v2y, v2z, -e0y, e0x, 0, hx, hy, hz)
                && !separatingAxis(v0x, v0y, v0z, v1x, v1y, v1z, v2x, v2y, v2z, -e1y, e1x, 0, hx, hy, hz)
                && !separatingAxis(v0x, v0y, v0z, v1x, v1y, v1z, v2x, v2y, v2z, -e2y, e2x, 0, hx, hy, hz);
    }

    private static boolean separatingAxis(
            double v0x, double v0y, double v0z,
            double v1x, double v1y, double v1z,
            double v2x, double v2y, double v2z,
            double ax, double ay, double az,
            double hx, double hy, double hz) {
        double p0 = v0x * ax + v0y * ay + v0z * az;
        double p1 = v1x * ax + v1y * ay + v1z * az;
        double p2 = v2x * ax + v2y * ay + v2z * az;
        double r = hx * Math.abs(ax) + hy * Math.abs(ay) + hz * Math.abs(az);
        double min = Math.min(p0, Math.min(p1, p2));
        double max = Math.max(p0, Math.max(p1, p2));
        return max < -r || min > r;
    }

    private double[] barycentric(Triangle tri, double px, double py, double pz) {
        double[] p0 = tri.v0();
        double[] p1 = tri.v1();
        double[] p2 = tri.v2();
        double v0x = p1[0] - p0[0];
        double v0y = p1[1] - p0[1];
        double v0z = p1[2] - p0[2];
        double v1x = p2[0] - p0[0];
        double v1y = p2[1] - p0[1];
        double v1z = p2[2] - p0[2];
        double nx = v0y * v1z - v0z * v1y;
        double ny = v0z * v1x - v0x * v1z;
        double nz = v0x * v1y - v0y * v1x;
        double ax = Math.abs(nx);
        double ay = Math.abs(ny);
        double az = Math.abs(nz);
        double p0u;
        double p0v;
        double p1u;
        double p1v;
        double p2u;
        double p2v;
        double pu;
        double pv;
        if (ax >= ay && ax >= az) {
            p0u = p0[1]; p0v = p0[2]; p1u = p1[1]; p1v = p1[2]; p2u = p2[1]; p2v = p2[2]; pu = py; pv = pz;
        } else if (ay >= az) {
            p0u = p0[0]; p0v = p0[2]; p1u = p1[0]; p1v = p1[2]; p2u = p2[0]; p2v = p2[2]; pu = px; pv = pz;
        } else {
            p0u = p0[0]; p0v = p0[1]; p1u = p1[0]; p1v = p1[1]; p2u = p2[0]; p2v = p2[1]; pu = px; pv = py;
        }
        double denom = (p1v - p2v) * (p0u - p2u) + (p2u - p1u) * (p0v - p2v);
        if (Math.abs(denom) < 1e-12) {
            return new double[]{1.0 / 3, 1.0 / 3, 1.0 / 3};
        }
        double b0 = ((p1v - p2v) * (pu - p2u) + (p2u - p1u) * (pv - p2v)) / denom;
        double b1 = ((p2v - p0v) * (pu - p2u) + (p0u - p2u) * (pv - p2v)) / denom;
        double b2 = 1 - b0 - b1;
        return new double[]{b0, b1, b2};
    }

    private int sampleRgb(BufferedImage image, double u, double v) {
        int w = image.getWidth();
        int h = image.getHeight();
        if (w <= 0 || h <= 0) {
            return -1;
        }
        int texX = (int) Math.floor(u * w) % w;
        int texY = (int) Math.floor(v * h) % h;
        if (texX < 0) {
            texX += w;
        }
        if (texY < 0) {
            texY += h;
        }
        return image.getRGB(texX, texY);
    }

    private Map<TextureModel, BufferedImage> buildTextureImages(GltfModel model) {
        Map<TextureModel, BufferedImage> map = new HashMap<>();
        List<TextureModel> textures = model.getTextureModels();
        if (textures == null) {
            return map;
        }
        for (TextureModel texture : textures) {
            ImageModel image = texture.getImageModel();
            if (image == null) {
                continue;
            }
            BufferedImage img = loadImage(image);
            if (img != null) {
                map.put(texture, img);
            }
        }
        return map;
    }

    private BufferedImage loadImage(ImageModel image) {
        if (image.getUri() != null) {
            return null;
        }
        BufferViewModel bufferView = image.getBufferViewModel();
        if (bufferView == null) {
            return null;
        }
        try {
            ByteBuffer copy = bufferView.getBufferViewData().duplicate();
            byte[] bytes = new byte[copy.remaining()];
            copy.get(bytes);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return null;
        }
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }

    public static String mapColorToBlockId(int r, int g, int b) {
        String best = DEFAULT_BLOCK_ID;
        int bestDist = Integer.MAX_VALUE;
        for (BlockColorEntry entry : COLOR_TABLE) {
            int dr = r - entry.r();
            int dg = g - entry.g();
            int db = b - entry.b();
            int dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                best = entry.blockId();
            }
        }
        return best;
    }

    private record Triangle(double[] v0, double[] v1, double[] v2,
                            float[] uv0, float[] uv1, float[] uv2, BufferedImage texture) {
    }

    private record BlockColorEntry(String blockId, int r, int g, int b) {
    }

    private static final BlockColorEntry[] COLOR_TABLE = {
            new BlockColorEntry("minecraft:stone", 125, 125, 125),
            new BlockColorEntry("minecraft:dirt", 134, 96, 67),
            new BlockColorEntry("minecraft:grass_block", 95, 159, 53),
            new BlockColorEntry("minecraft:oak_planks", 162, 130, 78),
            new BlockColorEntry("minecraft:bricks", 150, 97, 83),
            new BlockColorEntry("minecraft:sandstone", 216, 203, 155),
            new BlockColorEntry("minecraft:snow_block", 240, 248, 248),
            new BlockColorEntry("minecraft:obsidian", 20, 18, 29),
            new BlockColorEntry("minecraft:glass", 200, 220, 220),
            new BlockColorEntry("minecraft:terracotta", 152, 94, 67),
            new BlockColorEntry("minecraft:white_concrete", 207, 213, 214),
            new BlockColorEntry("minecraft:gray_concrete", 55, 58, 62),
            new BlockColorEntry("minecraft:dark_oak_planks", 67, 43, 27),
            new BlockColorEntry("minecraft:spruce_planks", 115, 85, 49),
            new BlockColorEntry("minecraft:birch_planks", 196, 179, 123),
            new BlockColorEntry("minecraft:quartz_block", 235, 229, 222),
            new BlockColorEntry("minecraft:nether_bricks", 44, 21, 26),
            new BlockColorEntry("minecraft:end_stone", 219, 222, 158),
            new BlockColorEntry("minecraft:water", 63, 118, 228),
            new BlockColorEntry("minecraft:oak_leaves", 59, 112, 41)
    };
}