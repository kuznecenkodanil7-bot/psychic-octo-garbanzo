package ru.raidmine.minibaritone;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Very small A* path finder for walking on normal blocks.
 */
public final class PathFinder {
    private static final int[][] HORIZONTAL_DIRS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };
    private static final int[] Y_TRIES = {0, 1, -1};
    private static final int TARGET_SEARCH_RADIUS = 5;

    private PathFinder() {
    }

    public static Result findPath(World world, BlockPos rawStart, BlockPos rawTarget, int maxNodes) {
        BlockPos start = findNearestSafe(world, rawStart, 2);
        if (start == null) {
            return Result.fail("Не могу найти безопасную стартовую точку рядом с игроком.");
        }

        BlockPos target = isSafeStand(world, rawTarget) ? rawTarget : findNearestSafe(world, rawTarget, TARGET_SEARCH_RADIUS);
        if (target == null) {
            return Result.fail("Цель " + format(rawTarget) + " небезопасна: там нет места для игрока или нет пола.");
        }

        PriorityQueue<Node> open = new PriorityQueue<>();
        Map<BlockPos, Double> bestG = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        Node first = new Node(start, null, 0.0D, heuristic(start, target));
        open.add(first);
        bestG.put(start, 0.0D);

        int visited = 0;
        while (!open.isEmpty() && visited < maxNodes) {
            Node current = open.poll();
            if (!closed.add(current.pos)) {
                continue;
            }
            visited++;

            if (current.pos.equals(target) || current.pos.getManhattanDistance(target) <= 1) {
                return Result.success(reconstruct(current), target);
            }

            for (BlockPos next : neighbors(world, current.pos)) {
                if (closed.contains(next)) {
                    continue;
                }

                double step = movementCost(current.pos, next);
                double nextG = current.g + step;
                double oldBest = bestG.getOrDefault(next, Double.MAX_VALUE);
                if (nextG >= oldBest) {
                    continue;
                }

                bestG.put(next, nextG);
                double f = nextG + heuristic(next, target);
                open.add(new Node(next, current, nextG, f));
            }
        }

        return Result.fail("Путь не найден. Попробуй точку ближе или без стен/пропастей. Проверено узлов: " + visited + ".");
    }

    private static List<BlockPos> neighbors(World world, BlockPos pos) {
        List<BlockPos> out = new ArrayList<>(8);
        for (int[] dir : HORIZONTAL_DIRS) {
            for (int dy : Y_TRIES) {
                BlockPos next = pos.add(dir[0], dy, dir[1]);
                if (isSafeStand(world, next) && canStepBetween(world, pos, next)) {
                    out.add(next);
                    break;
                }
            }
        }
        return out;
    }

    private static boolean canStepBetween(World world, BlockPos from, BlockPos to) {
        int dy = to.getY() - from.getY();
        if (dy > 1 || dy < -1) {
            return false;
        }

        // When stepping up, the space above the current block must be empty.
        if (dy > 0 && !isPassable(world, from.up(2))) {
            return false;
        }

        // Avoid walking into a one-block drop that has no head room at the landing node.
        return isPassable(world, to) && isPassable(world, to.up());
    }

    private static BlockPos findNearestSafe(World world, BlockPos center, int radius) {
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (int r = 0; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        if (Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z)) != r) {
                            continue;
                        }
                        BlockPos candidate = center.add(x, y, z);
                        if (!isSafeStand(world, candidate)) {
                            continue;
                        }
                        int distance = candidate.getManhattanDistance(center);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            best = candidate;
                        }
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private static boolean isSafeStand(World world, BlockPos feet) {
        if (feet.getY() <= world.getBottomY() || feet.getY() >= world.getTopYInclusive()) {
            return false;
        }
        return isPassable(world, feet)
            && isPassable(world, feet.up())
            && isSolidFloor(world, feet.down());
    }

    private static boolean isPassable(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getCollisionShape(world, pos, ShapeContext.absent()).isEmpty();
    }

    private static boolean isSolidFloor(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.getCollisionShape(world, pos, ShapeContext.absent()).isEmpty()
            && state.getFluidState().isEmpty();
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        return a.getManhattanDistance(b);
    }

    private static double movementCost(BlockPos a, BlockPos b) {
        int dy = Math.abs(a.getY() - b.getY());
        return 1.0D + (dy * 0.65D);
    }

    private static List<BlockPos> reconstruct(Node end) {
        List<BlockPos> path = new ArrayList<>();
        Node node = end;
        while (node != null) {
            path.add(node.pos);
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static final class Node implements Comparable<Node> {
        private final BlockPos pos;
        private final Node parent;
        private final double g;
        private final double f;

        private Node(BlockPos pos, Node parent, double g, double f) {
            this.pos = pos;
            this.parent = parent;
            this.g = g;
            this.f = f;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f, other.f);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Node node)) {
                return false;
            }
            return Objects.equals(pos, node.pos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos);
        }
    }

    public record Result(boolean success, String message, List<BlockPos> path, BlockPos resolvedTarget) {
        public static Result success(List<BlockPos> path, BlockPos resolvedTarget) {
            return new Result(true, "OK", path, resolvedTarget);
        }

        public static Result fail(String message) {
            return new Result(false, message, List.of(), null);
        }
    }
}
