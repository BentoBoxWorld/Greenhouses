package world.bentobox.greenhouses.data.adapters;

import world.bentobox.bentobox.database.objects.adapters.AdapterInterface;
import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.RecipeManager;

/**
 * @author tastybento
 *
 */
public class BiomeRecipeAdapter implements AdapterInterface<BiomeRecipe, String> {

    @Override
    public BiomeRecipe deserialize(Object object) {
        return RecipeManager.getBiomeRecipies((String)object).orElse(null);
    }

    @Override
    public String serialize(Object object) {
        return ((BiomeRecipe)object).getName();
    }

}
