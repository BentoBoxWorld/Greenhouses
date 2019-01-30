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
        if (object instanceof String && ((String)object).equals("null")) {
            return null;
        }
        return RecipeManager.getBiomeRecipies((String)object).orElse(null);
    }

    @Override
    public String serialize(Object object) {
        if (object == null) {
            return "null";
        }
        return ((BiomeRecipe)object).getName();
    }

}
