package world.bentobox.greenhouses;


import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.Pladdon;


/**
 * @author tastybento
 */
public class GreenhousesPladdon extends Pladdon
{
    private Addon addon;
    @Override
    public Addon getAddon()
    {
        if (addon == null) {
            addon = new Greenhouses();
        }
        return addon;
    }
}
