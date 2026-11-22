package world.bentobox.greenhouses.ui.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BoundingBox;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.build.GreenhouseBuilder;
import world.bentobox.greenhouses.build.GreenhousePlan;
import world.bentobox.greenhouses.build.GreenhousePlan.Mode;
import world.bentobox.greenhouses.build.GreenhousePlan.Size;
import world.bentobox.greenhouses.build.GreenhousePlan.TooSmallException;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.ui.GreenhouseResults;

/**
 * Builds a ready-made test greenhouse for a recipe:
 * {@code build <recipe> [<x> <y> <z>] [<size>] [keep|clear]}.
 * <p>
 * The coordinates are the centre of the floor, where a player standing on it has their feet,
 * and take {@code ~} like vanilla commands. The size is the inside, {@code W}, {@code WxD} or
 * {@code WxDxH}; left out, it is sized to fit the recipe. Either way the inside is furnished
 * to meet the recipe; in {@code keep} mode (the default) anything already standing inside, such
 * as a tree, is left alone and worked around, while {@code clear} mode clears the inside first. Either way the greenhouse is then made with the
 * recipe, just as {@code make} would.
 *
 * @author tastybento
 */
class AdminBuildCommand extends ConfirmableCommand {

    private static final String KEY = "greenhouses.commands.admin.build.";
    private static final String RELATIVE = "~";

    /** What execute() builds, worked out by canExecute() */
    private BiomeRecipe recipe;
    private GreenhousePlan plan;
    private World world;
    private Location centre;

    /**
     * @param parent - parent command
     */
    public AdminBuildCommand(CompositeCommand parent) {
        super(parent, "build");
    }

    @Override
    public void setup() {
        AdminUtil.setup(this, true);
        this.setOnlyPlayer(true);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }
        Greenhouses addon = getAddon();
        // Recipe
        recipe = findRecipe(addon, args.getFirst()).orElse(null);
        if (recipe == null) {
            user.sendMessage("greenhouses.commands.user.make.unknown-recipe");
            user.sendMessage("greenhouses.commands.user.make.try-these");
            addon.getRecipes().getBiomeRecipes().forEach(br -> user
                    .sendMessage("greenhouses.commands.user.make.recipe-format", TextVariables.NAME, br.getName()));
            return false;
        }
        // Coordinates, defaulting to where the admin is standing
        Location here = user.getLocation();
        int i = 1;
        int x = here.getBlockX();
        int y = here.getBlockY();
        int z = here.getBlockZ();
        if (hasCoordinates(args)) {
            if (args.size() < i + 3 || !args.subList(i, i + 3).stream().allMatch(AdminBuildCommand::isCoordinate)) {
                user.sendMessage(KEY + "bad-coordinates");
                return false;
            }
            OptionalInt ox = coordinate(args.get(i), x);
            OptionalInt oy = coordinate(args.get(i + 1), y);
            OptionalInt oz = coordinate(args.get(i + 2), z);
            if (ox.isEmpty() || oy.isEmpty() || oz.isEmpty()) {
                user.sendMessage(KEY + "bad-coordinates");
                return false;
            }
            x = ox.getAsInt();
            y = oy.getAsInt();
            z = oz.getAsInt();
            i += 3;
        }
        // Size and mode, in either order
        Size size = null;
        Mode mode = Mode.KEEP;
        for (; i < args.size(); i++) {
            String a = args.get(i);
            Optional<Mode> m = Mode.parse(a);
            Optional<Size> s = Size.parse(a);
            if (m.isPresent()) {
                mode = m.get();
            } else if (s.isPresent()) {
                size = s.get();
            } else {
                user.sendMessage(KEY + "bad-argument", TextVariables.DESCRIPTION, a);
                return false;
            }
        }
        if (size == null) {
            size = GreenhousePlan.defaultSize(recipe);
        }
        // World
        world = here.getWorld();
        if (!Util.getWorld(world).equals(getWorld())) {
            user.sendMessage("general.errors.wrong-world");
            return false;
        }
        // Plan
        try {
            World w = here.getWorld();
            plan = GreenhousePlan.create(recipe, x, y, z, size, doorWall(here), mode,
                    (bx, by, bz) -> w == null || w.getBlockAt(bx, by, bz).isEmpty());
        } catch (TooSmallException e) {
            user.sendMessage(KEY + "too-small", "[size]", size.toString(), TextVariables.DESCRIPTION, e.getMessage());
            return false;
        }
        if (plan.getFloorY() - 1 < world.getMinHeight() || plan.getRoofY() >= world.getMaxHeight()) {
            user.sendMessage(KEY + "out-of-world");
            return false;
        }
        centre = new Location(world, x + 0.5D, y, z + 0.5D);
        return checkSite(user, addon);
    }

    /**
     * Checks the greenhouse would sit on one island and not overlap another greenhouse.
     */
    private boolean checkSite(User user, Greenhouses addon) {
        int[] c = plan.getCorners();
        Optional<Island> island = getIslands().getIslandAt(centre);
        if (island.isEmpty()) {
            user.sendMessage(KEY + "not-on-island");
            return false;
        }
        for (int[] corner : new int[][] { { c[0], c[1] }, { c[0], c[3] }, { c[2], c[1] }, { c[2], c[3] } }) {
            Location l = new Location(world, corner[0], centre.getY(), corner[1]);
            if (!getIslands().getIslandAt(l).equals(island)) {
                user.sendMessage(KEY + "not-on-island");
                return false;
            }
        }
        Greenhouse footprint = new Greenhouse();
        BoundingBox bb = plan.getBounds();
        footprint.setLocation(new Location(world, bb.getMinX(), bb.getMinY(), bb.getMinZ()));
        footprint.setBoundingBox(bb);
        Optional<Greenhouse> other = addon.getManager().getMap().getOverlappingGreenhouse(footprint);
        if (other.isPresent()) {
            user.sendMessage(KEY + "overlapping", "[id]", AdminUtil.shortId(other.get()));
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (plan.getMode() == Mode.CLEAR) {
            // Clearing replaces everything inside, so ask first
            BiomeRecipe r = recipe;
            GreenhousePlan p = plan;
            World w = world;
            Location c = centre;
            askConfirmation(user, user.getTranslation(KEY + "confirm-clear", "[size]", p.getSize().toString()),
                    () -> build(user, r, p, w, c));
        } else {
            build(user, recipe, plan, world, centre);
        }
        return true;
    }

    /**
     * Builds without asking for confirmation, for the admin hub, whose build dialog is the
     * confirmation. Checks the permission and arguments just as the command does.
     *
     * @param user the admin
     * @param args the command's arguments
     * @return true if it was built
     */
    boolean buildNow(User user, List<String> args) {
        if (!user.hasPermission(getPermission())) {
            user.sendMessage("general.errors.no-permission", TextVariables.PERMISSION, getPermission());
            return false;
        }
        if (!canExecute(user, getLabel(), args)) {
            return false;
        }
        build(user, recipe, plan, world, centre);
        return true;
    }

    private void build(User user, BiomeRecipe r, GreenhousePlan p, World w, Location c) {
        int changed = GreenhouseBuilder.build(w, p, r.hasPlants());
        user.sendMessage(KEY + "built", "[size]", p.getSize().toString(), TextVariables.NUMBER,
                String.valueOf(changed), "[recipe]", r.getName());
        Greenhouses addon = getAddon();
        addon.getManager().tryToMakeGreenhouse(c, r)
                .thenAccept(result -> GreenhouseResults.report(user, r, result, getPlugin()));
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> all) {
        // BentoBox passes everything after the top-level label, so this command's own arguments
        // start after its parents' labels and its own: e.g. "greenhouses build <recipe> ..."
        List<String> args = all.subList(Math.min(getLevel(), all.size()), all.size());
        int n = args.size();
        String last = n == 0 ? "" : args.get(n - 1);
        Greenhouses addon = getAddon();
        if (n <= 1) {
            List<String> names = addon.getRecipes().getBiomeRecipes().stream().map(BiomeRecipe::getName).toList();
            return Optional.of(Util.tabLimit(new ArrayList<>(names), last));
        }
        Location l = user.getLocation();
        List<String> options = new ArrayList<>();
        // Positions 2-4 are coordinates if the first of them is one (or is being typed)
        boolean coords = n <= 2 || isCoordinate(args.get(1));
        if (coords && n <= 4 && l != null) {
            int axis = n - 2;
            int value = axis == 0 ? l.getBlockX() : axis == 1 ? l.getBlockY() : l.getBlockZ();
            options.add(RELATIVE);
            options.add(String.valueOf(value));
        }
        if (n >= 2) {
            options.add("keep");
            options.add("clear");
            findRecipe(addon, args.getFirst()).ifPresent(r -> options.add(GreenhousePlan.defaultSize(r).toString()));
        }
        return Optional.of(Util.tabLimit(options, last));
    }

    private static Optional<BiomeRecipe> findRecipe(Greenhouses addon, String name) {
        return addon.getRecipes().getBiomeRecipes().stream().filter(r -> r.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * @param s text
     * @return true if it looks like a coordinate: {@code ~}, {@code ~n} or a whole number
     */
    static boolean isCoordinate(String s) {
        return s.startsWith(RELATIVE) || s.matches("-?\\d+");
    }

    /**
     * Coordinates follow the recipe if the next argument starts with {@code ~}, or the next
     * three are all numbers. So a size must be written {@code WxD} or {@code WxDxH}; a lone
     * number there is taken as the start of the coordinates.
     *
     * @param args the arguments, recipe first
     * @return true if coordinates were given
     */
    static boolean hasCoordinates(List<String> args) {
        if (args.size() < 2) {
            return false;
        }
        if (args.get(1).startsWith(RELATIVE)) {
            return true;
        }
        return args.size() >= 4 && args.subList(1, 4).stream().allMatch(AdminBuildCommand::isCoordinate);
    }

    /**
     * Reads one coordinate.
     *
     * @param s    {@code ~}, {@code ~n} or a whole number
     * @param here the admin's own coordinate, for {@code ~}
     * @return the coordinate, or empty if it is not one
     */
    static OptionalInt coordinate(String s, int here) {
        try {
            if (s.equals(RELATIVE)) {
                return OptionalInt.of(here);
            }
            if (s.startsWith(RELATIVE)) {
                return OptionalInt.of(here + Integer.parseInt(s.substring(1)));
            }
            return OptionalInt.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    /**
     * @param here where the admin is
     * @return the wall the admin is facing, which gets the door
     */
    static BlockFace doorWall(@Nullable Location here) {
        if (here == null) {
            return BlockFace.NORTH;
        }
        float yaw = (here.getYaw() % 360 + 360) % 360;
        // Minecraft yaw: 0 = south, 90 = west, 180 = north, 270 = east
        if (yaw >= 45 && yaw < 135) {
            return BlockFace.WEST;
        }
        if (yaw >= 135 && yaw < 225) {
            return BlockFace.NORTH;
        }
        if (yaw >= 225 && yaw < 315) {
            return BlockFace.EAST;
        }
        return BlockFace.SOUTH;
    }
}
