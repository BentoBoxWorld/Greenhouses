package world.bentobox.greenhouses;

import java.util.ArrayList;
import java.util.List;

import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.api.configuration.StoreAt;


/**
 * @author tastybento
 * Where all the settings are
 */
@StoreAt(filename="config.yml", path="addons/Greenhouses") // Explicitly call out what name this should have.
@ConfigComment("Greenhouses Configuration [version]")
@ConfigComment("")
public class Settings implements ConfigObject {

    // General
    @ConfigComment("BentoBox GameModes that will use Greenhouses")
    @ConfigEntry(path = "greenhouses.game-modes")
    private List<String> gameModes = new ArrayList<>();

    @ConfigComment("Show loaded recipe details during startup of server")
    @ConfigEntry(path = "greenhouses.startup-log")
    private boolean startupLog = false;

    @ConfigComment("Weather and ecosystem settings")
    @ConfigComment("How often it should snow in the g/h when the weather is raining, in seconds")
    @ConfigEntry(path = "greenhouses.snowspeed")
    private double snowSpeed = 30;

    @ConfigComment("Chance of any snow falling in a greenhouse when the snow tick occurs")
    @ConfigComment("(1.0 = always, 0.0 = never)")
    @ConfigEntry(path = "greenhouses.snowchance")
    private double snowChanceGlobal = 1;
    @ConfigComment("How many blocks should get snow 1 = all of them, 0 = none, 0.1 = 1 in 10")
    @ConfigEntry(path = "greenhouses.snowdensity")
    private double snowDensity = 0.1;

    @ConfigComment("Biome activity")
    @ConfigComment("How often should greenhouse biomes be checked to make sure they are still valid")
    @ConfigEntry(path = "greenhouses.ecotick")
    private int ecoTick = 5;
    @ConfigComment("How often should plants potentially grow in minutes if bonemeal is in the hopper")
    @ConfigEntry(path = "greenhouses.planttick")
    private int plantTick = 1;
    @ConfigComment("How often should blocks potentially convert, in minutes ")
    @ConfigComment("Example: dirt-> sand in desert greenhouse")
    @ConfigEntry(path = "greenhouses.blocktick")
    private int blockTick = 2;
    @ConfigComment("How often should mobs be potentially spawned in a greenhouse, in minutes")
    @ConfigEntry(path = "greenhouses.mobtick")
    private int mobTick = 5;


    @ConfigComment("Default settings for greenhouse actions")
    @ConfigComment("Allow lava or water to flow out of a greenhouse, e.g. through the door, floor")
    @ConfigEntry(path = "greenhouses.allowflowout")
    private boolean allowFlowOut;
    @ConfigComment("Allow lava or water to flow into a greenhouse, e.g., through the door")
    @ConfigEntry(path = "greenhouses.allowflowin")
    private boolean allowFlowIn;

    @ConfigComment("Allow glowstone to be used as well as glass in roof and walls")
    @ConfigEntry(path = "greenhouses.allowglowstone")
    private boolean allowGlowstone = true;

    /**
     * @return the gameModes
     */
    public List<String> getGameModes() {
        return gameModes;
    }
    /**
     * @return the snowSpeed
     */
    public double getSnowSpeed() {
        return snowSpeed;
    }
    /**
     * @return the snowChanceGlobal
     */
    public double getSnowChanceGlobal() {
        return snowChanceGlobal;
    }
    /**
     * @return the snowDensity
     */
    public double getSnowDensity() {
        return snowDensity;
    }
    /**
     * @return the ecoTick
     */
    public int getEcoTick() {
        return ecoTick;
    }
    /**
     * @return the plantTick
     */
    public int getPlantTick() {
        return plantTick;
    }
    /**
     * @return the blockTick
     */
    public int getBlockTick() {
        return blockTick;
    }
    /**
     * @return the mobTick
     */
    public int getMobTick() {
        return mobTick;
    }
    /**
     * @return the startupLog
     */
    public boolean isStartupLog() {
        return startupLog;
    }
    /**
     * @param startupLog the startupLog to set
     */
    public void setStartupLog(boolean startupLog) {
        this.startupLog = startupLog;
    }
    /**
     * @return the allowFlowOut
     */
    public boolean isAllowFlowOut() {
        return allowFlowOut;
    }
    /**
     * @return the allowFlowIn
     */
    public boolean isAllowFlowIn() {
        return allowFlowIn;
    }
    /**
     * @param gameModes the gameModes to set
     */
    public void setGameModes(List<String> gameModes) {
        this.gameModes = gameModes;
    }
    /**
     * @param snowSpeed the snowSpeed to set
     */
    public void setSnowSpeed(double snowSpeed) {
        this.snowSpeed = snowSpeed;
    }
    /**
     * @param snowChanceGlobal the snowChanceGlobal to set
     */
    public void setSnowChanceGlobal(double snowChanceGlobal) {
        this.snowChanceGlobal = snowChanceGlobal;
    }
    /**
     * @param snowDensity the snowDensity to set
     */
    public void setSnowDensity(double snowDensity) {
        this.snowDensity = snowDensity;
    }
    /**
     * @param ecoTick the ecoTick to set
     */
    public void setEcoTick(int ecoTick) {
        this.ecoTick = ecoTick;
    }
    /**
     * @param plantTick the plantTick to set
     */
    public void setPlantTick(int plantTick) {
        this.plantTick = plantTick;
    }
    /**
     * @param blockTick the blockTick to set
     */
    public void setBlockTick(int blockTick) {
        this.blockTick = blockTick;
    }
    /**
     * @param mobTick the mobTick to set
     */
    public void setMobTick(int mobTick) {
        this.mobTick = mobTick;
    }
    /**
     * @param allowFlowOut the allowFlowOut to set
     */
    public void setAllowFlowOut(boolean allowFlowOut) {
        this.allowFlowOut = allowFlowOut;
    }
    /**
     * @param allowFlowIn the allowFlowIn to set
     */
    public void setAllowFlowIn(boolean allowFlowIn) {
        this.allowFlowIn = allowFlowIn;
    }
    /**
     * @return the allowGlowstone
     */
    public boolean isAllowGlowstone() {
        return allowGlowstone;
    }
    /**
     * @param allowGlowstone the allowGlowstone to set
     */
    public void setAllowGlowstone(boolean allowGlowstone) {
        this.allowGlowstone = allowGlowstone;
    }

}