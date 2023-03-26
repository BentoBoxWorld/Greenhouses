package world.bentobox.greenhouses;


import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.Plugin;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.Pladdon;


/**
 * @author tastybento
 */
@Plugin(name = "Greenhouses", version = "1.0")
@ApiVersion(ApiVersion.Target.v1_18)
public class GreenhousesPladdon extends Pladdon
{
    @Override
    public Addon getAddon()
    {
        return new Greenhouses();
    }
}
