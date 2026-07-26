package world.bentobox.greenhouses.ui.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.managers.GreenhouseManager.UnloadedGreenhouse;

/**
 * Lists greenhouses, optionally filtered to one player's island. Records that failed to load
 * are listed separately so that they can be acted on.
 *
 * @author tastybento
 */
class AdminListCommand extends CompositeCommand {

    private static final int PER_PAGE = 10;

    private List<Greenhouse> toShow;
    private int page;

    /**
     * @param parent - parent command
     */
    public AdminListCommand(CompositeCommand parent) {
        super(parent, "list");
    }

    @Override
    public void setup() {
        AdminUtil.setup(this, true);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        Greenhouses addon = this.getAddon();
        page = 1;
        String playerArg = null;
        // list, list <page>, list <player>, list <player> <page>
        if (!args.isEmpty()) {
            if (isNumber(args.get(0))) {
                page = Integer.parseInt(args.get(0));
            } else {
                playerArg = args.get(0);
                if (args.size() > 1 && isNumber(args.get(1))) {
                    page = Integer.parseInt(args.get(1));
                }
            }
        }
        if (playerArg == null) {
            toShow = new ArrayList<>(addon.getManager().getMap().getGreenhouses());
        } else {
            UUID uuid = getPlayers().getUUID(playerArg);
            if (uuid == null) {
                user.sendMessage("general.errors.unknown-player", TextVariables.NAME, playerArg);
                return false;
            }
            toShow = new ArrayList<>(getIslands().getIslands(getWorld(), uuid).stream()
                    .flatMap(i -> addon.getManager().getMap().getGreenhouses(i).stream()).toList());
        }
        if (page < 1) {
            page = 1;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Greenhouses addon = this.getAddon();
        List<UnloadedGreenhouse> unloaded = addon.getManager().getUnloaded();
        if (toShow.isEmpty() && unloaded.isEmpty()) {
            user.sendMessage("greenhouses.commands.admin.list.none");
            return true;
        }
        int pages = Math.max(1, (int) Math.ceil((double) toShow.size() / PER_PAGE));
        if (page > pages) {
            page = pages;
        }
        user.sendMessage("greenhouses.commands.admin.list.title", TextVariables.NUMBER, String.valueOf(toShow.size()),
                "[page]", String.valueOf(page), "[pages]", String.valueOf(pages));
        toShow.stream().skip((long) (page - 1) * PER_PAGE).limit(PER_PAGE)
        .forEach(gh -> user.sendMessage("greenhouses.commands.admin.list.entry", "[id]", AdminUtil.shortId(gh),
                "[recipe]", String.valueOf(gh.getBiomeRecipeName()), "[owner]", AdminUtil.ownerName(addon, gh),
                "[world]", AdminUtil.worldName(gh), "[xyz]", AdminUtil.xyz(gh), "[broken]",
                gh.isBroken() ? user.getTranslation("greenhouses.commands.admin.list.broken") : ""));
        if (pages > 1) {
            user.sendMessage("greenhouses.commands.admin.list.more", "[label]", this.getTopLabel(), "[page]",
                    String.valueOf(Math.min(page + 1, pages)));
        }
        // Records in the database that could not be loaded are always shown - they are the ones
        // an admin most needs to know about, and they appear on no island listing.
        if (!unloaded.isEmpty()) {
            user.sendMessage("greenhouses.commands.admin.list.unloaded-title", TextVariables.NUMBER,
                    String.valueOf(unloaded.size()));
            unloaded.forEach(u -> user.sendMessage("greenhouses.commands.admin.list.unloaded-entry", "[id]",
                    AdminUtil.shortId(u.greenhouse()), "[reason]", u.reason().name(), "[world]",
                    AdminUtil.worldName(u.greenhouse()), "[xyz]", AdminUtil.xyz(u.greenhouse())));
        }
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.size() == 2) {
            return Optional.of(Util.tabLimit(Util.getOnlinePlayerList(user), args.get(1)));
        }
        return Optional.empty();
    }

    private boolean isNumber(String s) {
        return s.chars().allMatch(Character::isDigit) && !s.isEmpty();
    }

}
