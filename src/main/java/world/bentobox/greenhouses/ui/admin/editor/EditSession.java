package world.bentobox.greenhouses.ui.admin.editor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.greenhouses.greenhouse.RecipeDraft;

/**
 * One admin's edit of one recipe: the draft being changed, the revision it was taken at, and
 * whether it has unsaved changes. Nothing reaches biomes.yml until it is saved.
 * <p>
 * Each admin has at most one session. It outlives the editor's inventory being closed, so an
 * admin can wander off and come back to their changes, and ends when they save, discard, or
 * leave the server.
 *
 * @author tastybento
 */
public class EditSession {

    private static final Map<UUID, EditSession> SESSIONS = new HashMap<>();

    private final RecipeDraft draft;
    private int revision;
    private final boolean isNew;
    private boolean dirty;

    /**
     * @param draft    the draft to edit; the session edits it directly
     * @param revision the revision it was taken at, or {@code RecipeManager.NEW_RECIPE}
     * @param isNew    true if the recipe is not in biomes.yml yet
     */
    public EditSession(@NonNull RecipeDraft draft, int revision, boolean isNew) {
        this.draft = draft;
        this.revision = revision;
        this.isNew = isNew;
        this.dirty = isNew;
    }

    /**
     * @param admin an admin
     * @return their session, if they have one
     */
    public static Optional<EditSession> get(UUID admin) {
        return Optional.ofNullable(SESSIONS.get(admin));
    }

    /**
     * Starts a session for an admin, replacing any they had.
     *
     * @param admin   the admin
     * @param session the session
     * @return the session
     */
    public static EditSession start(UUID admin, EditSession session) {
        SESSIONS.put(admin, session);
        return session;
    }

    /**
     * Ends an admin's session, dropping any unsaved changes.
     *
     * @param admin the admin
     */
    public static void end(UUID admin) {
        SESSIONS.remove(admin);
    }

    /**
     * Ends every session, e.g. when the addon is disabled.
     */
    public static void endAll() {
        SESSIONS.clear();
    }

    /**
     * @return the draft being edited
     */
    public RecipeDraft getDraft() {
        return draft;
    }

    /**
     * @return the recipe key
     */
    public String getKey() {
        return draft.getKey();
    }

    /**
     * @return the revision the draft was taken at
     */
    public int getRevision() {
        return revision;
    }

    /**
     * @param revision the revision to save over, e.g. after choosing to overwrite someone else's change
     */
    public void setRevision(int revision) {
        this.revision = revision;
    }

    /**
     * @return true if the recipe is not in biomes.yml yet
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * @return true if there are unsaved changes
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Notes that the draft has been changed.
     */
    public void changed() {
        dirty = true;
    }

    /**
     * Drops sessions when admins leave.
     */
    public static class QuitListener implements Listener {
        /**
         * @param e the event
         */
        @EventHandler
        public void onQuit(PlayerQuitEvent e) {
            end(e.getPlayer().getUniqueId());
        }
    }
}
