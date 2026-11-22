package world.bentobox.greenhouses.listeners;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import world.bentobox.greenhouses.Greenhouses;
import world.bentobox.greenhouses.data.Greenhouse;
import world.bentobox.greenhouses.managers.GreenhouseManager;
import world.bentobox.greenhouses.managers.GreenhouseMap;

/**
 * Tests the natural spawning guard in {@link GreenhouseGuard}.
 *
 * @author tastybento
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GreenhouseGuardTest {

    @Mock
    private Greenhouses addon;
    @Mock
    private GreenhouseManager gm;
    @Mock
    private GreenhouseMap map;
    @Mock
    private Greenhouse gh;
    @Mock
    private Location inside;
    @Mock
    private Location outside;

    private GreenhouseGuard guard;

    @BeforeEach
    void setUp() {
        when(addon.getManager()).thenReturn(gm);
        when(gm.getMap()).thenReturn(map);
        when(map.getGreenhouse(inside)).thenReturn(Optional.of(gh));
        when(map.getGreenhouse(outside)).thenReturn(Optional.empty());
        // The spot is in the greenhouse, not in its walls or roof
        when(gh.isRoofOrWallBlock(inside)).thenReturn(false);
        guard = new GreenhouseGuard(addon);
    }

    private CreatureSpawnEvent spawn(Location l, SpawnReason reason) {
        CreatureSpawnEvent e = mock(CreatureSpawnEvent.class);
        when(e.getLocation()).thenReturn(l);
        when(e.getSpawnReason()).thenReturn(reason);
        return e;
    }

    @Test
    void testNaturalSpawnInsideIsCancelled() {
        CreatureSpawnEvent e = spawn(inside, SpawnReason.NATURAL);
        guard.onCreatureSpawn(e);
        verify(e).setCancelled(true);
    }

    @Test
    void testNaturalSpawnOutsideIsAllowed() {
        CreatureSpawnEvent e = spawn(outside, SpawnReason.NATURAL);
        guard.onCreatureSpawn(e);
        verify(e).setCancelled(false);
    }

    @Test
    void testOtherSpawnsInsideAreAllowed() {
        // Greenhouse mobs, spawn eggs, breeding and so on are left alone
        CreatureSpawnEvent e = spawn(inside, SpawnReason.CUSTOM);
        guard.onCreatureSpawn(e);
        verify(e, never()).setCancelled(true);
    }
}
