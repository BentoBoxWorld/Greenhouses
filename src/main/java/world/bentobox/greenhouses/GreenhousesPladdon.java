package world.bentobox.greenhouses;


import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.Pladdon;


/**
 * @author tastybento
 */
public class GreenhousesPladdon extends Pladdon
{
    @Override
    public Addon getAddon()
    {
        return new Greenhouses();
    }
}
