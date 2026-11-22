package world.bentobox.greenhouses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SettingsTest {

    private Settings s;

    @BeforeEach
    void setUp() {
        s = new Settings();
    }

    @Test
    void testGetGameModes() {
        assertTrue(s.getGameModes().isEmpty());
    }

    @Test
    void testGetSnowSpeed() {
        assertEquals(30D, s.getSnowSpeed(), 0D);
    }

    @Test
    void testGetSnowChanceGlobal() {
        assertEquals(1D, s.getSnowChanceGlobal(), 0D);
    }

    @Test
    void testGetSnowDensity() {
        assertEquals(0.1D, s.getSnowDensity(), 0D);
    }

    @Test
    void testGetEcoTick() {
        assertEquals(5, s.getEcoTick());
    }

    @Test
    void testGetPlantTick() {
        assertEquals(1, s.getPlantTick());
    }

    @Test
    void testGetBlockTick() {
        assertEquals(2, s.getBlockTick());
    }

    @Test
    void testGetMobTick() {
        assertEquals(5, s.getMobTick());
    }

    @Test
    void testIsStartupLog() {
        assertFalse(s.isStartupLog());
    }

    @Test
    void testSetStartupLog() {
        s.setStartupLog(true);
        assertTrue(s.isStartupLog());
    }

    @Test
    void testIsAllowFlowOut() {
        assertFalse(s.isAllowFlowOut());
    }

    @Test
    void testIsAllowFlowIn() {
        assertFalse(s.isAllowFlowIn());
    }

    @Test
    void testSetGameModes() {
        s.setGameModes(Collections.singletonList("BSkyBlock"));
        assertEquals("BSkyBlock", s.getGameModes().get(0));
    }

    @Test
    void testSetSnowSpeed() {
        s.setSnowSpeed(50);
        assertEquals(50D, s.getSnowSpeed(), 0D);
    }

    @Test
    void testSetSnowChanceGlobal() {
        s.setSnowChanceGlobal(50);
        assertEquals(50D, s.getSnowChanceGlobal(), 0D);
    }

    @Test
    void testSetSnowDensity() {
        s.setSnowDensity(50);
        assertEquals(50D, s.getSnowDensity(), 0D);
    }

    @Test
    void testSetEcoTick() {
        s.setEcoTick(50);
        assertEquals(50, s.getEcoTick());
    }

    @Test
    void testSetPlantTick() {
        s.setPlantTick(50);
        assertEquals(50, s.getPlantTick());
    }

    @Test
    void testSetBlockTick() {
        s.setBlockTick(50);
        assertEquals(50, s.getBlockTick());
    }

    @Test
    void testSetMobTick() {
        s.setMobTick(50);
        assertEquals(50, s.getMobTick());
    }

    @Test
    void testSetAllowFlowOut() {
        assertFalse(s.isAllowFlowOut());
        s.setAllowFlowOut(true);
        assertTrue(s.isAllowFlowOut());
    }

    @Test
    void testSetAllowFlowIn() {
        assertFalse(s.isAllowFlowIn());
        s.setAllowFlowIn(true);
        assertTrue(s.isAllowFlowIn());
    }

    @Test
    void testIsAllowGlowstone() {
        assertTrue(s.isAllowGlowstone());
    }

    @Test
    void testSetAllowGlowstone() {
        assertTrue(s.isAllowGlowstone());
        s.setAllowGlowstone(false);
        assertFalse(s.isAllowGlowstone());
    }

    @Test
    void testIsAllowPanes() {
        assertTrue(s.isAllowPanes());
    }

    @Test
    void testSetAllowPanes() {
        assertTrue(s.isAllowPanes());
        s.setAllowPanes(false);
        assertFalse(s.isAllowPanes());
    }
}
