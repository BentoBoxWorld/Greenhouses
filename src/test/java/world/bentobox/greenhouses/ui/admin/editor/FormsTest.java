package world.bentobox.greenhouses.ui.admin.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mockito;

import world.bentobox.bentobox.api.dialogs.DialogField;
import world.bentobox.bentobox.api.dialogs.DialogResponse;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.greenhouses.greenhouse.MobHabitat;
import world.bentobox.greenhouses.greenhouse.RecipeDraft;
import world.bentobox.greenhouses.greenhouse.RecipeDraft.MobSpec;

/**
 * Tests the recipe editor's {@link Forms}: the fields they show and how answers are applied.
 *
 * @author tastybento
 */
class FormsTest {

    private User user;
    private RecipeDraft d;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        // Translations come back as their keys
        user = mock(User.class, Mockito.withSettings().defaultAnswer(i -> i.getMethod().getName().startsWith(
                "getTranslation") ? i.getArgument(0) : Mockito.RETURNS_DEFAULTS.answer(i)));
        d = new RecipeDraft("TEST");
        d.setBiome(Biome.PLAINS);
        d.setFriendlyName("Test");
        d.setPriority(5);
        d.setWaterCoverage(50);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        Mockito.framework().clearInlineMocks();
    }

    private static DialogResponse answers(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return DialogResponse.of(m);
    }

    private static List<String> keys(List<DialogField> fields) {
        return fields.stream().map(DialogField::key).toList();
    }

    @Test
    void testGeneral() {
        Forms.General f = new Forms.General(user, d);
        assertEquals(List.of("name", "description", "priority", "permission"), keys(f.fields()));
        assertEquals(List.of(), f.apply(answers("name", " <red>Hot</red> ", "description", "Warm.", "priority", 9f,
                "permission", "gh.hot")));
        assertEquals("<red>Hot</red>", d.getFriendlyName());
        assertEquals("Warm.", d.getDescription());
        assertEquals(9, d.getPriority());
        assertEquals("gh.hot", d.getPermission());
    }

    @Test
    void testGeneralProblemsChangeNothing() {
        Forms.General f = new Forms.General(user, d);
        List<String> probs = f.apply(answers("name", "  ", "description", "x", "priority", 1f, "permission", "a b"));
        assertEquals(2, probs.size());
        assertEquals("Test", d.getFriendlyName());
        assertEquals(5, d.getPriority());
    }

    @Test
    void testCoverage() {
        Forms.Coverage f = new Forms.Coverage(user, d);
        assertEquals(List.of("water_mode", "water", "lava_mode", "lava", "ice_mode", "ice"), keys(f.fields()));
        assertEquals(List.of(), f.apply(answers("water_mode", Forms.AT_LEAST, "water", 30f, "lava_mode", Forms.NONE,
                "lava", 50f, "ice_mode", Forms.UNCHECKED, "ice", 10f)));
        assertEquals(30, d.getWaterCoverage());
        assertEquals(0, d.getLavaCoverage());
        assertEquals(RecipeDraft.UNSET, d.getIceCoverage());
    }

    @Test
    void testCoverageTooMuch() {
        Forms.Coverage f = new Forms.Coverage(user, d);
        assertEquals(1, f.apply(answers("water_mode", Forms.AT_LEAST, "water", 80f, "lava_mode", Forms.AT_LEAST,
                "lava", 30f, "ice_mode", Forms.UNCHECKED, "ice", 1f)).size());
        assertEquals(50, d.getWaterCoverage());
    }

    @Test
    void testMobLimits() {
        Forms.MobLimits f = new Forms.MobLimits(user, d);
        assertEquals(List.of("ground", "water", "lava", "air", "max"), keys(f.fields()));
        f.apply(answers("ground", 4f, "water", 12f, "lava", 0f, "air", 27f, "max", -1f));
        assertEquals(4, d.getMobLimit());
        assertEquals(12, d.getHabitatLimit(MobHabitat.WATER));
        assertEquals(0, d.getHabitatLimit(MobHabitat.LAVA));
        assertEquals(27, d.getHabitatLimit(MobHabitat.AIR));
        assertEquals(-1, d.getMaxMobs());
    }

    @Test
    void testChance() {
        AtomicReference<Double> got = new AtomicReference<>();
        Forms.Chance f = new Forms.Chance(user, "Copper", 0.005, got::set);
        assertEquals(List.of(), f.apply(answers("chance", "0,005%")));
        assertEquals(0.005, got.get());
        assertEquals(1, f.apply(answers("chance", "lots")).size());
        assertEquals(1, f.apply(answers("chance", "0")).size());
        assertEquals(1, f.apply(answers("chance", "100.1")).size());
        assertEquals(List.of(), f.apply(answers("chance", "100")));
    }

    @Test
    void testMobWithVariants() {
        d.getMobs().add(new MobSpec(EntityType.WOLF, null, 10, Material.GRASS_BLOCK));
        Forms.Mob f = new Forms.Mob(user, d, 0);
        assertEquals(List.of("chance", "variant"), keys(f.fields()));
        assertEquals(List.of(), f.apply(answers("chance", "12.5", "variant", "SNOWY")));
        assertEquals(new MobSpec(EntityType.WOLF, "SNOWY", 12.5, Material.GRASS_BLOCK), d.getMobs().getFirst());
        f.apply(answers("chance", "12.5", "variant", Forms.NONE));
        assertNull(d.getMobs().getFirst().data());
    }

    @Test
    void testMobWithoutVariants() {
        d.getMobs().add(new MobSpec(EntityType.COW, null, 10, Material.GRASS_BLOCK));
        Forms.Mob f = new Forms.Mob(user, d, 0);
        assertEquals(List.of("chance"), keys(f.fields()));
        assertFalse(f.apply(answers("chance", "-3")).isEmpty());
        assertEquals(10, d.getMobs().getFirst().chance());
    }

    @Test
    void testCount() {
        AtomicReference<Integer> got = new AtomicReference<>();
        new Forms.Count(user, "Stone", 3, got::set).apply(answers("count", 500f));
        assertEquals(256, got.get());
    }

    @Test
    void testNewKey() {
        AtomicReference<String> got = new AtomicReference<>();
        Forms.NewKey f = new Forms.NewKey(user, null, k -> k.equals("PLAINS"), got::set);
        assertEquals(1, f.apply(answers("key", "has space")).size());
        assertEquals(1, f.apply(answers("key", "PLAINS")).size());
        assertNull(got.get());
        assertEquals(List.of(), f.apply(answers("key", "Cherry_Grove-2")));
        assertEquals("Cherry_Grove-2", got.get());
    }

    @Test
    void testHasVariants() {
        assertTrue(Forms.hasVariants(EntityType.CREEPER));
        assertFalse(Forms.hasVariants(EntityType.COW));
    }
}
