package world.bentobox.greenhouses.data.adapters;

import java.awt.Rectangle;

import world.bentobox.bentobox.database.objects.adapters.AdapterInterface;

/**
 * @author tastybento
 *
 */
public class RectangleAdapter implements AdapterInterface<Rectangle, String> {

    @Override
    public Rectangle deserialize(Object object) {
        if (object instanceof String) {
            String[] s = ((String)object).split(",");
            if (s.length == 4) {
                return new Rectangle(Integer.valueOf(s[0]), Integer.valueOf(s[1]), Integer.valueOf(s[2]), Integer.valueOf(s[3]));
            }
        }
        return null;
    }

    @Override
    public String serialize(Object object) {
        Rectangle r = (Rectangle)object;
        return r == null ? "" : r.x + ","+ r.y + "," + r.width + "," + r.height;
    }

}
