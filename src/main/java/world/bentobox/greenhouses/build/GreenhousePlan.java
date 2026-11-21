package world.bentobox.greenhouses.build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BoundingBox;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.greenhouses.greenhouse.BiomeRecipe;

/**
 * Works out, block by block, a test greenhouse for a recipe: a glass box with a door and a
 * roof hopper and, in {@link Mode#CLEAR clear} mode, a floor and contents that meet the
 * recipe. It only plans; {@link GreenhouseBuilder} places the blocks.
 * <p>
 * Coordinates: the anchor is the centre of the floor, at the first layer of air inside (where
 * a player standing on the floor has their feet). The floor layer is one below it, the walls
 * run from the anchor up {@link Size#height()} blocks, and the flat roof sits on top. The
 * walls and roof surround the {@link Size#width()} by {@link Size#depth()} interior.
 *
 * @author tastybento
 */
public final class GreenhousePlan {

    /** Default interior width and depth */
    public static final int DEFAULT_WIDTH = 9;
    /** Default interior height */
    public static final int DEFAULT_HEIGHT = 5;
    /** Smallest interior width or depth */
    public static final int MIN_WIDTH = 3;
    /** Largest interior width or depth */
    public static final int MAX_WIDTH = 31;
    /** Smallest interior height: room for a door */
    public static final int MIN_HEIGHT = 2;
    /** Largest interior height */
    public static final int MAX_HEIGHT = 15;
    /** How many of each block that plants grow on or mobs spawn on are laid, if the recipe doesn't already need them */
    static final int HABITAT_BLOCKS = 4;

    /**
     * Whether to clear and furnish the inside, or leave what is there.
     */
    public enum Mode {
        /** Build the shell around whatever is there and only fill holes in the floor */
        KEEP,
        /** Clear the inside and lay a floor and contents that meet the recipe */
        CLEAR;

        /**
         * @param s text
         * @return the mode it names, ignoring case
         */
        public static Optional<Mode> parse(String s) {
            for (Mode m : values()) {
                if (m.name().equalsIgnoreCase(s)) {
                    return Optional.of(m);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * Interior size of the greenhouse, walls not included.
     *
     * @param width  along x
     * @param depth  along z
     * @param height from the floor to the roof, not including either
     */
    public record Size(int width, int depth, int height) {

        private static final Pattern PATTERN = Pattern.compile("(\\d{1,2})(?:x(\\d{1,2}))?(?:x(\\d{1,2}))?",
                Pattern.CASE_INSENSITIVE);

        /**
         * Checks the size is within limits.
         *
         * @throws IllegalArgumentException if not
         */
        public Size {
            if (width < MIN_WIDTH || width > MAX_WIDTH || depth < MIN_WIDTH || depth > MAX_WIDTH) {
                throw new IllegalArgumentException("Width and depth must be " + MIN_WIDTH + " to " + MAX_WIDTH);
            }
            if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
                throw new IllegalArgumentException("Height must be " + MIN_HEIGHT + " to " + MAX_HEIGHT);
            }
        }

        /**
         * Reads a size written as {@code W}, {@code WxD} or {@code WxDxH}. A missing depth is the
         * width; a missing height is the default.
         *
         * @param s text
         * @return the size, or empty if it is not a size or out of range
         */
        public static Optional<Size> parse(String s) {
            Matcher m = PATTERN.matcher(s);
            if (!m.matches()) {
                return Optional.empty();
            }
            int w = Integer.parseInt(m.group(1));
            int d = m.group(2) == null ? w : Integer.parseInt(m.group(2));
            int h = m.group(3) == null ? DEFAULT_HEIGHT : Integer.parseInt(m.group(3));
            try {
                return Optional.of(new Size(w, d, h));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }

        /**
         * @return the interior floor area
         */
        public int area() {
            return width * depth;
        }

        @Override
        public String toString() {
            return width + "x" + depth + "x" + height;
        }
    }

    /**
     * One block to place.
     *
     * @param x        x
     * @param y        y
     * @param z        z
     * @param material the block
     * @param onlyIfAir true to place it only where there is air now, e.g. to fill holes
     * @param facing   the direction a door or hopper faces, or null
     * @param upper    true for the top half of a door
     */
    public record Placement(int x, int y, int z, @NonNull Material material, boolean onlyIfAir,
            @Nullable BlockFace facing, boolean upper) {

        static Placement set(int x, int y, int z, Material m) {
            return new Placement(x, y, z, m, false, null, false);
        }

        static Placement fillAir(int x, int y, int z, Material m) {
            return new Placement(x, y, z, m, true, null, false);
        }
    }

    /**
     * Thrown when the recipe cannot be met at the requested size.
     */
    public static class TooSmallException extends Exception {
        private static final long serialVersionUID = 1L;

        TooSmallException(String message) {
            super(message);
        }
    }

    /** A floor cell */
    private record Cell(int x, int z) {
    }

    private final List<Placement> placements = new ArrayList<>();
    private final Size size;
    private final Mode mode;
    private final int x0;
    private final int z0;
    private final int anchorY;
    private final int anchorX;
    private final int anchorZ;
    private final BlockFace doorWall;
    private final Cell doorCell;
    private final Cell doorFront;
    private final Cell centre;
    private @Nullable Cell treeCell;

    private GreenhousePlan(int ax, int ay, int az, Size size, BlockFace doorWall, Mode mode) {
        this.size = size;
        this.mode = mode;
        this.anchorX = ax;
        this.anchorY = ay;
        this.anchorZ = az;
        this.x0 = ax - (size.width() - 1) / 2;
        this.z0 = az - (size.depth() - 1) / 2;
        this.doorWall = doorWall;
        this.centre = new Cell(ax, az);
        switch (doorWall) {
        case NORTH -> {
            doorCell = new Cell(ax, z0 - 1);
            doorFront = new Cell(ax, z0);
        }
        case SOUTH -> {
            doorCell = new Cell(ax, maxZ() + 1);
            doorFront = new Cell(ax, maxZ());
        }
        case WEST -> {
            doorCell = new Cell(x0 - 1, az);
            doorFront = new Cell(x0, az);
        }
        case EAST -> {
            doorCell = new Cell(maxX() + 1, az);
            doorFront = new Cell(maxX(), az);
        }
        default -> throw new IllegalArgumentException("The door must be in a NORTH, SOUTH, EAST or WEST wall");
        }
    }

    // ------------------------------------------------------------------------
    // Factories
    // ------------------------------------------------------------------------

    /**
     * Plans a test greenhouse.
     *
     * @param recipe   the recipe to meet
     * @param ax       anchor x, the centre of the floor
     * @param ay       anchor y, the first layer of air above the floor
     * @param az       anchor z
     * @param size     interior size
     * @param doorWall the wall to put the door in: NORTH, SOUTH, EAST or WEST
     * @param mode     clear and furnish, or keep what is there
     * @return the plan
     * @throws TooSmallException if the recipe's needs do not fit at this size (clear mode only)
     */
    @NonNull
    public static GreenhousePlan create(@NonNull BiomeRecipe recipe, int ax, int ay, int az, @NonNull Size size,
            @NonNull BlockFace doorWall, @NonNull Mode mode) throws TooSmallException {
        GreenhousePlan plan = new GreenhousePlan(ax, ay, az, size, doorWall, mode);
        plan.plan(recipe);
        return plan;
    }

    /**
     * Works out a size that fits the recipe: the default 9x9x5, made taller for a tree and
     * wider until everything the recipe needs fits on the floor.
     *
     * @param recipe the recipe
     * @return the size, or the largest size if even that is too small
     */
    @NonNull
    public static Size defaultSize(@NonNull BiomeRecipe recipe) {
        int height = Math.clamp(Math.max(DEFAULT_HEIGHT, trunkHeight(recipe) + 2), MIN_HEIGHT, MAX_HEIGHT);
        for (int w = DEFAULT_WIDTH; w <= MAX_WIDTH; w += 2) {
            Size s = new Size(w, w, height);
            try {
                create(recipe, 0, 64, 0, s, BlockFace.NORTH, Mode.CLEAR);
                return s;
            } catch (TooSmallException e) {
                // Try bigger
            }
        }
        return new Size(MAX_WIDTH, MAX_WIDTH, height);
    }

    // ------------------------------------------------------------------------
    // Planning
    // ------------------------------------------------------------------------

    private void plan(BiomeRecipe recipe) throws TooSmallException {
        BiomeStyle style = BiomeStyle.of(recipe.getBiome());
        int floorY = anchorY - 1;
        int roofY = getRoofY();
        // Base: hold the floor up where there is nothing under it
        forFootprint((x, z) -> placements.add(Placement.fillAir(x, floorY - 1, z, style.getBase())));
        if (mode == Mode.CLEAR) {
            // Clear the inside
            for (int y = anchorY; y < roofY; y++) {
                for (int x = x0; x <= maxX(); x++) {
                    for (int z = z0; z <= maxZ(); z++) {
                        placements.add(Placement.set(x, y, z, Material.AIR));
                    }
                }
            }
            planFloor(recipe, style, floorY);
        } else {
            // Keep what is there; just fill holes in the floor
            forInterior((x, z) -> placements.add(Placement.fillAir(x, floorY, z, style.getFiller())));
        }
        // Floor under the walls - must not be glass, or the walls would not start at the floor
        forRing((x, z) -> placements.add(mode == Mode.CLEAR ? Placement.set(x, floorY, z, style.getBase())
                : Placement.fillAir(x, floorY, z, style.getBase())));
        // Walls
        for (int y = anchorY; y < roofY; y++) {
            final int yy = y;
            forRing((x, z) -> placements.add(Placement.set(x, yy, z, Material.GLASS)));
        }
        // Roof
        forFootprint((x, z) -> placements.add(Placement.set(x, roofY, z, Material.GLASS)));
        // Door, in the middle of the chosen wall
        placements.add(new Placement(doorCell.x(), anchorY, doorCell.z(), style.getDoor(), false, doorWall, false));
        placements.add(new Placement(doorCell.x(), anchorY + 1, doorCell.z(), style.getDoor(), false, doorWall, true));
        // Hopper in the middle of the roof
        placements.add(new Placement(anchorX, roofY, anchorZ, Material.HOPPER, false, BlockFace.DOWN, false));
    }

    private void planFloor(BiomeRecipe recipe, BiomeStyle style, int floorY) throws TooSmallException {
        Map<Material, Integer> required = recipe.getRequiredBlocks();
        int area = size.area();
        // Spots that must stay plain floor: where the admin stands and in front of the door
        Set<Cell> reserved = new java.util.HashSet<>(List.of(centre, doorFront));
        // Tree
        List<Material> logs = required.keySet().stream().filter(GreenhousePlan::isLog).toList();
        List<Material> leaves = required.keySet().stream().filter(GreenhousePlan::isLeaves).toList();
        int trunk = trunkHeight(recipe);
        if (trunk > 0) {
            treeCell = pickTreeCell();
            if (treeCell == null || trunk + 1 > size.height()) {
                throw new TooSmallException("No room for a tree " + (trunk + 1) + " blocks tall");
            }
            reserved.add(treeCell);
        }
        // Floor spots, furthest from the door first, so pools sit at the back
        List<Cell> pool = new ArrayList<>();
        forInterior((x, z) -> {
            Cell c = new Cell(x, z);
            if (!reserved.contains(c)) {
                pool.add(c);
            }
        });
        pool.sort(Comparator.comparingInt((Cell c) -> -distance(c, doorFront)).thenComparingInt(Cell::x)
                .thenComparingInt(Cell::z));

        Map<Cell, Material> floor = new LinkedHashMap<>();
        Material filler = pickFiller(recipe, style);
        reserved.forEach(c -> floor.put(c, filler));
        // Liquids and ice
        take(pool, floor, Material.WATER, coverageBlocks(recipe.getWaterCoverage(), area));
        take(pool, floor, Material.ICE, coverageBlocks(recipe.getIceCoverage(), area));
        take(pool, floor, Material.LAVA, coverageBlocks(recipe.getLavaCoverage(), area));
        // Required solid blocks that are not part of the tree
        Map<Material, Integer> onTop = new LinkedHashMap<>();
        for (Map.Entry<Material, Integer> e : required.entrySet()) {
            Material m = e.getKey();
            if (isLog(m) || isLeaves(m)) {
                continue;
            }
            if (isFloorBlock(m)) {
                take(pool, floor, m, e.getValue());
            } else {
                onTop.merge(m, e.getValue(), Integer::sum);
            }
        }
        // A few of each block plants grow on and mobs spawn on, if not already there
        Set<Material> habitat = EnumSet.noneOf(Material.class);
        habitat.addAll(recipe.getPlantGrowOn());
        habitat.addAll(recipe.getMobSpawnOn());
        for (Material m : habitat) {
            if (m == Material.WATER || m == Material.LAVA || m.isAir() || floor.containsValue(m)
                    || isLog(m) || isLeaves(m) || m == filler) {
                continue;
            }
            if (isFloorBlock(m)) {
                take(pool, floor, m, Math.min(HABITAT_BLOCKS, Math.max(0, pool.size() - 1)));
            } else if (!onTop.containsKey(m)) {
                onTop.put(m, HABITAT_BLOCKS);
            }
        }
        // Everything else is filler
        pool.forEach(c -> floor.put(c, filler));
        floor.forEach((c, m) -> placements.add(Placement.set(c.x(), floorY, c.z(), m)));
        // Tree
        if (treeCell != null) {
            planTree(logs, leaves, required, trunk);
        }
        // Things that sit on top of the floor, e.g. snow layers or plants
        planOnTop(onTop, floor);
    }

    private void planTree(List<Material> logs, List<Material> leaves, Map<Material, Integer> required, int trunk)
            throws TooSmallException {
        // Trunk: the required logs in order, padded with the first log
        List<Material> trunkBlocks = new ArrayList<>();
        logs.forEach(l -> trunkBlocks.addAll(Collections.nCopies(required.get(l), l)));
        Material pad = logs.isEmpty() ? logFor(leaves.getFirst()) : logs.getFirst();
        while (trunkBlocks.size() < trunk) {
            trunkBlocks.add(pad);
        }
        for (int i = 0; i < trunkBlocks.size(); i++) {
            placements.add(Placement.set(treeCell.x(), anchorY + i, treeCell.z(), trunkBlocks.get(i)));
        }
        // Canopy: on top of the trunk, then around its top, working outwards and down
        List<Material> canopy = new ArrayList<>();
        leaves.forEach(l -> canopy.addAll(Collections.nCopies(required.get(l), l)));
        if (canopy.isEmpty()) {
            return;
        }
        List<int[]> spots = canopySpots(trunk);
        if (spots.size() < canopy.size()) {
            throw new TooSmallException("No room for " + canopy.size() + " leaves");
        }
        for (int i = 0; i < canopy.size(); i++) {
            int[] s = spots.get(i);
            placements.add(Placement.set(s[0], s[1], s[2], canopy.get(i)));
        }
    }

    /**
     * @return places leaves can go, best first
     */
    private List<int[]> canopySpots(int trunk) {
        List<int[]> spots = new ArrayList<>();
        int top = anchorY + trunk - 1;
        int tx = treeCell.x();
        int tz = treeCell.z();
        addSpot(spots, tx, top + 1, tz);
        for (int r = 1; r <= 2; r++) {
            for (int y = top + 1; y >= Math.max(anchorY + 1, top - 1); y--) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) == r) {
                            addSpot(spots, tx + dx, y, tz + dz);
                        }
                    }
                }
            }
        }
        return spots;
    }

    private void addSpot(List<int[]> spots, int x, int y, int z) {
        Cell c = new Cell(x, z);
        boolean inside = x >= x0 && x <= maxX() && z >= z0 && z <= maxZ() && y >= anchorY && y < getRoofY();
        // Keep head room where the admin stands and at the door
        boolean inTheWay = (c.equals(centre) || c.equals(doorFront)) && y <= anchorY + 1;
        if (inside && !inTheWay && !c.equals(treeCell)) {
            spots.add(new int[] { x, y, z });
        }
    }

    private void planOnTop(Map<Material, Integer> onTop, Map<Cell, Material> floor) throws TooSmallException {
        if (onTop.isEmpty()) {
            return;
        }
        // Lily pads float on water; everything else sits on solid floor away from the paths
        List<Cell> solid = new ArrayList<>();
        List<Cell> water = new ArrayList<>();
        floor.forEach((c, m) -> {
            if (m == Material.WATER) {
                water.add(c);
            } else if (m.isSolid() && !c.equals(centre) && !c.equals(doorFront) && !c.equals(treeCell)) {
                solid.add(c);
            }
        });
        for (Map.Entry<Material, Integer> e : onTop.entrySet()) {
            List<Cell> where = e.getKey() == Material.LILY_PAD ? water : solid;
            if (where.size() < e.getValue()) {
                throw new TooSmallException("No room for " + e.getValue() + " " + e.getKey());
            }
            for (int i = 0; i < e.getValue(); i++) {
                Cell c = where.removeFirst();
                placements.add(Placement.set(c.x(), anchorY, c.z(), e.getKey()));
            }
        }
    }

    private static void take(List<Cell> pool, Map<Cell, Material> floor, Material m, int count)
            throws TooSmallException {
        if (count > pool.size()) {
            throw new TooSmallException("Needs " + count + " " + m.name().toLowerCase(Locale.ENGLISH)
                    + " but only " + pool.size() + " floor spots are left");
        }
        for (int i = 0; i < count; i++) {
            floor.put(pool.removeFirst(), m);
        }
    }

    /**
     * Picks a spot for the tree away from the door and the centre, and in from the walls if
     * there is room.
     */
    private @Nullable Cell pickTreeCell() {
        Cell best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int x = x0; x <= maxX(); x++) {
            for (int z = z0; z <= maxZ(); z++) {
                Cell c = new Cell(x, z);
                if (distance(c, centre) <= 1 || c.equals(doorFront)) {
                    continue;
                }
                boolean inFromWalls = x > x0 && x < maxX() && z > z0 && z < maxZ();
                int score = distance(c, doorFront) * 2 + (inFromWalls ? 100 : 0) - distance(c, centre);
                if (score > bestScore) {
                    bestScore = score;
                    best = c;
                }
            }
        }
        return best;
    }

    private Material pickFiller(BiomeRecipe recipe, BiomeStyle style) {
        // Prefer a block that plants grow on or mobs spawn on, so the greenhouse is lively
        return recipe.getPlantGrowOn().stream().filter(GreenhousePlan::isFloorBlock).findFirst()
                .or(() -> recipe.getMobSpawnOn().stream().filter(GreenhousePlan::isFloorBlock).findFirst())
                .orElse(style.getFiller());
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    /**
     * @param recipe the recipe
     * @return how tall the tree must be, or 0 for no tree
     */
    static int trunkHeight(BiomeRecipe recipe) {
        Map<Material, Integer> required = recipe.getRequiredBlocks();
        int logs = required.entrySet().stream().filter(e -> isLog(e.getKey())).mapToInt(Map.Entry::getValue).sum();
        boolean hasLeaves = required.keySet().stream().anyMatch(GreenhousePlan::isLeaves);
        return Math.max(logs, hasLeaves ? 2 : 0);
    }

    static int coverageBlocks(int coverage, int area) {
        return coverage <= 0 ? 0 : (int) Math.ceil(coverage * area / 100D);
    }

    static boolean isLog(Material m) {
        String n = m.name();
        return n.endsWith("_LOG") || n.endsWith("_STEM") || n.endsWith("_WOOD") || n.endsWith("_HYPHAE");
    }

    static boolean isLeaves(Material m) {
        String n = m.name();
        return n.endsWith("_LEAVES") || n.endsWith("_WART_BLOCK");
    }

    /**
     * @return true if the block can be part of the floor itself, rather than sitting on it
     */
    static boolean isFloorBlock(Material m) {
        return m.isBlock() && m.isSolid() && m != Material.SNOW && !m.name().endsWith("_CARPET");
    }

    /**
     * @return a log to hold up leaves that came without one
     */
    static Material logFor(Material leaves) {
        String n = leaves.name();
        if (n.endsWith("_WART_BLOCK")) {
            return n.startsWith("WARPED") ? Material.WARPED_STEM : Material.CRIMSON_STEM;
        }
        Material log = Material.matchMaterial(n.replace("_LEAVES", "_LOG"));
        return log == null ? Material.OAK_LOG : log;
    }

    private static int distance(Cell a, Cell b) {
        return Math.max(Math.abs(a.x() - b.x()), Math.abs(a.z() - b.z()));
    }

    @FunctionalInterface
    private interface CellAction {
        void accept(int x, int z) throws TooSmallException;
    }

    private void forInterior(CellAction a) throws TooSmallException {
        for (int x = x0; x <= maxX(); x++) {
            for (int z = z0; z <= maxZ(); z++) {
                a.accept(x, z);
            }
        }
    }

    private void forFootprint(CellAction a) throws TooSmallException {
        for (int x = x0 - 1; x <= maxX() + 1; x++) {
            for (int z = z0 - 1; z <= maxZ() + 1; z++) {
                a.accept(x, z);
            }
        }
    }

    private void forRing(CellAction a) throws TooSmallException {
        for (int x = x0 - 1; x <= maxX() + 1; x++) {
            for (int z = z0 - 1; z <= maxZ() + 1; z++) {
                if (x < x0 || x > maxX() || z < z0 || z > maxZ()) {
                    a.accept(x, z);
                }
            }
        }
    }

    private int maxX() {
        return x0 + size.width() - 1;
    }

    private int maxZ() {
        return z0 + size.depth() - 1;
    }

    // ------------------------------------------------------------------------
    // Results
    // ------------------------------------------------------------------------

    /**
     * @return the blocks to place, in order; later ones replace earlier ones at the same spot
     */
    @NonNull
    public List<Placement> getPlacements() {
        return Collections.unmodifiableList(placements);
    }

    /**
     * @return the box taken up, from the base under the floor to the roof, walls included
     */
    @NonNull
    public BoundingBox getBounds() {
        return new BoundingBox(x0 - 1D, anchorY - 2D, z0 - 1D, maxX() + 2D, getRoofY() + 1D, maxZ() + 2D);
    }

    /**
     * @return the y of the roof
     */
    public int getRoofY() {
        return anchorY + size.height();
    }

    /**
     * @return the y of the floor
     */
    public int getFloorY() {
        return anchorY - 1;
    }

    /**
     * @return the interior size
     */
    @NonNull
    public Size getSize() {
        return size;
    }

    /**
     * @return the mode
     */
    @NonNull
    public Mode getMode() {
        return mode;
    }

    /**
     * @return the outside corners of the walls at floor level, x and z: min, then max
     */
    public int[] getCorners() {
        return new int[] { x0 - 1, z0 - 1, maxX() + 1, maxZ() + 1 };
    }
}
