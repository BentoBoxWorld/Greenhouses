/**
 * 
 */
package world.bentobox.greenhouses.data.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.greenhouses.greenhouse.BiomeRecipe;
import world.bentobox.greenhouses.managers.RecipeManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {RecipeManager.class} )
public class BiomeRecipeAdapterTest {

    private BiomeRecipeAdapter bra;
    private BiomeRecipe recipe;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        bra = new BiomeRecipeAdapter();
        
        PowerMockito.mockStatic(RecipeManager.class);
        recipe = mock(BiomeRecipe.class);
        Optional<BiomeRecipe> optionalRecipe = Optional.of(recipe);
        when(RecipeManager.getBiomeRecipies(Mockito.eq("recipe_name"))).thenReturn(optionalRecipe);
        when(RecipeManager.getBiomeRecipies(Mockito.eq("nothing"))).thenReturn(Optional.empty());
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.adapters.BiomeRecipeAdapter#deserialize(java.lang.Object)}.
     */
    @Test
    public void testDeserialize() {
        assertEquals(recipe, bra.deserialize("recipe_name"));
    }
    
    /**
     * Test method for {@link world.bentobox.greenhouses.data.adapters.BiomeRecipeAdapter#deserialize(java.lang.Object)}.
     */
    @Test
    public void testDeserializeNoRecipe() {
        assertNull(bra.deserialize("nothing"));
    }
    
    /**
     * Test method for {@link world.bentobox.greenhouses.data.adapters.BiomeRecipeAdapter#deserialize(java.lang.Object)}.
     */
    @Test
    public void testDeserializeNull() {
        assertNull(bra.deserialize("null"));
    }

    /**
     * Test method for {@link world.bentobox.greenhouses.data.adapters.BiomeRecipeAdapter#serialize(java.lang.Object)}.
     */
    @Test
    public void testSerialize() {
        BiomeRecipe br = mock(BiomeRecipe.class);
        when(br.getName()).thenReturn("recipe_name");
        assertEquals("recipe_name", bra.serialize(br));
    }
    
    /**
     * Test method for {@link world.bentobox.greenhouses.data.adapters.BiomeRecipeAdapter#serialize(java.lang.Object)}.
     */
    @Test
    public void testSerializeNull() {
        assertEquals("null", bra.serialize(null));
    }

}
