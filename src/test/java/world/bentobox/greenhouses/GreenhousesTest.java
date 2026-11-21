package world.bentobox.greenhouses;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.CommandsManager;

class GreenhousesTest extends CommonTestSetup {

    private static final String CONFIG_YML =
            """
            greenhouses:
              game-modes: []
              startup-log: false
              snowspeed: 30.0
              snowchance: 1.0
              snowdensity: 0.1
              ecotick: 5
              planttick: 1
              blocktick: 2
              mobtick: 5
              allowflowout: false
              allowflowin: false
              allowglowstone: true
              allowpanes: true
            """;

    private static final String BIOMES_YML =
            """
            biomes:
              test_plains:
                friendlyname: "Plains"
                biome: PLAINS
                icon: GRASS_BLOCK
                priority: 0
                contents:
                  GRASS_BLOCK: 1
            """;

    @Mock
    private AddonsManager am;

    private Greenhouses addon;
    private MockedStatic<DatabaseSetup> mockDb;

    @SuppressWarnings("unchecked")
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        AbstractDatabaseHandler<Object> h = mock(AbstractDatabaseHandler.class);
        mockDb = Mockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        mockDb.when(DatabaseSetup::getDatabase).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(h);
        when(h.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));

        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        when(plugin.getAddonsManager()).thenReturn(am);
        when(am.getGameModeAddons()).thenReturn(Collections.emptyList());

        when(plugin.getFlagsManager()).thenReturn(fm);
        when(fm.getFlags()).thenReturn(Collections.emptyList());

        addon = new Greenhouses();
        File jFile = new File("addon.jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jFile))) {
            addJarEntry(jos, "config.yml", CONFIG_YML);
            addJarEntry(jos, "biomes.yml", BIOMES_YML);
        }
        File dataFolder = new File("addons/Greenhouses");
        addon.setDataFolder(dataFolder);
        addon.setFile(jFile);
        AddonDescription desc = new AddonDescription.Builder("bentobox", "Greenhouses", "1.0.0")
                .description("test").authors("tastybento").build();
        addon.setDescription(desc);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        if (mockDb != null) mockDb.closeOnDemand();
        super.tearDown();
        new File("addon.jar").delete();
        deleteAll(new File("addons"));
    }

    private static void addJarEntry(JarOutputStream jos, String name, String content) throws Exception {
        JarEntry entry = new JarEntry(name);
        jos.putNextEntry(entry);
        jos.write(content.getBytes(StandardCharsets.UTF_8));
        jos.closeEntry();
    }

    @Test
    void testGetSettingsNullBeforeLoad() {
        assertNull(addon.getSettings());
    }

    @Test
    void testOnEnable() {
        addon.onEnable();
        assertNotNull(addon);
    }

    @Test
    void testOnEnableLoadsSettings() {
        addon.onEnable();
        assertNotNull(addon.getSettings());
    }

    @Test
    void testOnEnableSettingsDefaults() {
        addon.onEnable();
        Settings s = addon.getSettings();
        assertNotNull(s);
        org.junit.jupiter.api.Assertions.assertEquals(30.0, s.getSnowSpeed(), 0.01);
        org.junit.jupiter.api.Assertions.assertEquals(5, s.getEcoTick());
        org.junit.jupiter.api.Assertions.assertTrue(s.isAllowGlowstone());
    }

    @Test
    void testOnDisable() {
        addon.onDisable();
        assertNotNull(addon);
    }

    @Test
    void testGetSettings() {
        assertNull(addon.getSettings());
        addon.onEnable();
        assertNotNull(addon.getSettings());
    }
}
