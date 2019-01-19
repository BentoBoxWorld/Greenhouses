package world.bentobox.greenhouses;

import java.util.UUID;

import org.bukkit.entity.Player;

import world.bentobox.greenhouses.greenhouse.Greenhouse;

/**
 * Tracks the following info on the player
 * UUID, player name, whether they are in a greenhouse or not and how many greenhouses they have
 */
public class Players {
    private UUID uuid;
    private Greenhouse inGreenhouse;
    private int numberOfGreenhouses;

    /**
     * @param uuid
     *            Constructor - initializes the state variables
     * 
     */
    public Players(final Player player) {
	this.uuid = player.getUniqueId();
	// We do not know if the player is in a greenhouse or not yet
	this.inGreenhouse = null;
	// We start with the assumption they have no greenhouses yet
	this.numberOfGreenhouses = 0;	
    }

    /**
     * @return the inGreenhouse
     */
    public Greenhouse getInGreenhouse() {
        return inGreenhouse;
    }

    /**
     * @param inGreenhouse the inGreenhouse to set
     */
    public void setInGreenhouse(Greenhouse inGreenhouse) {
        this.inGreenhouse = inGreenhouse;
    }

    /**
     * @return the numberOfGreenhouses
     */
    public int getNumberOfGreenhouses() {
        return numberOfGreenhouses;
    }

    public void incrementGreenhouses() {
	numberOfGreenhouses++;
    }
    
    public void decrementGreenhouses() {
	numberOfGreenhouses--;
	if (numberOfGreenhouses < 0) {
	    numberOfGreenhouses = 0;
	}
    }

    /**
     * @return the uuid
     */
    public UUID getUuid() {
        return uuid;
    }

}
