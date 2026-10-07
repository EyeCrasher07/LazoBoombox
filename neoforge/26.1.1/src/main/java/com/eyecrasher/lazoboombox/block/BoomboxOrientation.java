package com.eyecrasher.lazoboombox.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Sixteen placement angles; cardinal states and their original shapes remain compatible. */
public final class BoomboxOrientation {
    public static final float DEGREES_PER_STEP = 22.5F;
    private static final VoxelShape[] SHAPES = createShapes();
    private static final VoxelShape[] OUTLINES = createOutlines();

    private BoomboxOrientation() {}

    public static int stepForYaw(float yaw) {
        return ((int) Math.floor(yaw / DEGREES_PER_STEP + 0.5F) + 8) & 15;
    }

    public static int step(BlockState state) {
        int facing;
        switch (state.getValue(BoomboxBlock.FACING)) {
            case EAST:
                facing = 4;
                break;
            case SOUTH:
                facing = 8;
                break;
            case WEST:
                facing = 12;
                break;
            default:
                facing = 0;
        }
        return facing + state.getValue(BoomboxBlock.ROTATION);
    }

    public static BlockState withStep(BlockState state, int step) {
        step &= 15;
        Direction facing;
        switch (step / 4) {
            case 1:
                facing = Direction.EAST;
                break;
            case 2:
                facing = Direction.SOUTH;
                break;
            case 3:
                facing = Direction.WEST;
                break;
            default:
                facing = Direction.NORTH;
        }
        return state.setValue(BoomboxBlock.FACING, facing)
                .setValue(BoomboxBlock.ROTATION, step % 4);
    }

    public static VoxelShape shape(BlockState state) {
        return SHAPES[step(state)];
    }

    /** One cuboid for vanilla selection and particles; collision keeps its rotated geometry. */
    public static VoxelShape outline(BlockState state) {
        return OUTLINES[step(state)];
    }

    private static VoxelShape[] createOutlines() {
        VoxelShape[] outlines = new VoxelShape[16];
        outlines[0] = SHAPES[0];
        outlines[4] = SHAPES[4];
        for (int step = 1; step < 8; step++) {
            if (step == 4) continue;
            double angle = Math.toRadians(step * DEGREES_PER_STEP);
            double cosine = Math.abs(Math.cos(angle)), sine = Math.abs(Math.sin(angle));
            double halfX = 8 * cosine + 3 * sine;
            double halfZ = 8 * sine + 3 * cosine;
            // Reusing the 64 collision strips here draws their edges and emits particles per strip.
            outlines[step] = Block.box(8 - halfX, 0, 8 - halfZ, 8 + halfX, 10, 8 + halfZ);
        }
        for (int step = 8; step < 16; step++) outlines[step] = outlines[step - 8];
        return outlines;
    }

    private static VoxelShape[] createShapes() {
        VoxelShape[] shapes = new VoxelShape[16];
        shapes[0] = Block.box(0, 0, 5, 16, 10, 11);
        shapes[4] = Block.box(5, 0, 0, 11, 10, 16);
        for (int step = 1; step < 8; step++) if (step != 4) shapes[step] = rotatedShape(step);
        for (int step = 8; step < 16; step++) shapes[step] = shapes[step - 8];
        return shapes;
    }

    private static VoxelShape rotatedShape(int step) {
        double angle = Math.toRadians(step * DEGREES_PER_STEP);
        double cosine = Math.cos(angle), sine = Math.sin(angle);
        double[][] corners = {{-8, -3}, {8, -3}, {8, 3}, {-8, 3}};
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        for (double[] corner : corners) {
            double x = corner[0], z = corner[1];
            corner[0] = 8 + x * cosine - z * sine;
            corner[1] = 8 + x * sine + z * cosine;
            minX = Math.min(minX, corner[0]);
            maxX = Math.max(maxX, corner[0]);
        }
        VoxelShape result = Shapes.empty();
        // Thin vertical strips follow the rotated body rather than an oversized bounding box.
        for (int strip = 0; strip < 64; strip++) {
            double left = minX + (maxX - minX) * strip / 64;
            double right = minX + (maxX - minX) * (strip + 1) / 64;
            double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < 4; i++) {
                double[] a = corners[i], b = corners[(i + 1) % 4];
                if (a[0] >= left && a[0] <= right) {
                    minZ = Math.min(minZ, a[1]);
                    maxZ = Math.max(maxZ, a[1]);
                }
                if (a[0] != b[0])
                    for (double x : new double[] {left, right}) {
                        double fraction = (x - a[0]) / (b[0] - a[0]);
                        if (fraction >= 0 && fraction <= 1) {
                            double z = a[1] + fraction * (b[1] - a[1]);
                            minZ = Math.min(minZ, z);
                            maxZ = Math.max(maxZ, z);
                        }
                    }
            }
            if (minZ < maxZ) result = Shapes.or(result, Block.box(left, 0, minZ, right, 10, maxZ));
        }
        return result;
    }
}
