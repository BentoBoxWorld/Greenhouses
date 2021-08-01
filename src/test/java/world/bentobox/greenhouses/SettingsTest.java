package world.bentobox.greenhouses;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class SettingsTest {

    private Settings s;

    @Before
    public void setUp() {
        s = new Settings();
    }

    @Test
    public void testGetGameModes() {
        assertTrue(s.getGameModes().isEmpty());
    }

    @Test
    public void testGetSnowSpeed() {
        assertEquals(30D, s.getSnowSpeed(), 0D);
    }

    @Test
    public void testGetSnowChanceGlobal() {
        assertEquals(1D, s.getSnowChanceGlobal(), 0D);
    }

    @Test
    public void testGetSnowDensity() {
        assertEquals(0.1D, s.getSnowDensity(), 0D);
    }

    @Test
    public void testGetEcoTick() {
        assertEquals(5, s.getEcoTick());
    }

    @Test
    public void testGetPlantTick() {
        assertEquals(1, s.getPlantTick());
    }

    @Test
    public void testGetBlockTick() {
        assertEquals(2, s.getBlockTick());
    }

    @Test
    public void testGetMobTick() {
        assertEquals(5, s.getMobTick());
    }

    @Test
    public void testIsStartupLog() {
        assertFalse(s.isStartupLog());
    }

    @Test
    public void testSetStartupLog() {
        s.setStartupLog(true);
        assertTrue(s.isStartupLog());
    }

    @Test
    public void testIsAllowFlowOut() {
        assertFalse(s.isAllowFlowOut());
    }

    @Test
    public void testIsAllowFlowIn() {
        assertFalse(s.isAllowFlowIn());
    }

    @Test
    public void testSetGameModes() {
        s.setGameModes(Collections.singletonList("BSkyBlock"));
        assertEquals("BSkyBlock", s.getGameModes().get(0));
    }

    @Test
    public void testSetSnowSpeed() {
        s.setSnowSpeed(50);
        assertEquals(50D, s.getSnowSpeed(), 0D);
    }

    @Test
    public void testSetSnowChanceGlobal() {
        s.setSnowChanceGlobal(50);
        assertEquals(50D, s.getSnowChanceGlobal(), 0D);

    }

    @Test
    public void testSetSnowDensity() {
        s.setSnowDensity(50);
        assertEquals(50D, s.getSnowDensity(), 0D);
    }

    @Test
    public void testSetEcoTick() {
        s.setEcoTick(50);
        assertEquals(50, s.getEcoTick());
    }

    @Test
    public void testSetPlantTick() {
        s.setPlantTick(50);
        assertEquals(50, s.getPlantTick());

    }

    @Test
    public void testSetBlockTick() {
        s.setBlockTick(50);
        assertEquals(50, s.getBlockTick());

    }

    @Test
    public void testSetMobTick() {
        s.setMobTick(50);
        assertEquals(50, s.getMobTick());

    }

    @Test
    public void testSetAllowFlowOut() {
        assertFalse(s.isAllowFlowOut());
        s.setAllowFlowOut(true);
        assertTrue(s.isAllowFlowOut());
    }

    @Test
    public void testSetAllowFlowIn() {
        assertFalse(s.isAllowFlowIn());
        s.setAllowFlowIn(true);
        assertTrue(s.isAllowFlowIn());
    }

    @Test
    public void testIsAllowGlowstone() {
        assertTrue(s.isAllowGlowstone());
    }

    @Test
    public void testSetAllowGlowstone() {
        assertTrue(s.isAllowGlowstone());
        s.setAllowGlowstone(false);
        assertFalse(s.isAllowGlowstone());
    }

    @Test
    public void testIsAllowPanes() {
        assertTrue(s.isAllowPanes());
    }

    @Test
    public void testSetAllowPanes() {
        assertTrue(s.isAllowPanes());
        s.setAllowPanes(false);
        assertFalse(s.isAllowPanes());
    }

}
