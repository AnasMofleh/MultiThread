package util;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.lang.reflect.Field;

public class RetinaDisplay {

    /** @return  true if this application is running on a pixel-doubled display ("Retina" in Mac lingo) */
    public static boolean detected() {
        // This is a bit of a hack.
        try {
            if (System.getProperty("java.version").startsWith("1.")) {
                // version string was 1.x.y up to Java 8 (1.8), then changed to 9.y, 10.y, 11.y, ...
    
                GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                    Field field = graphicsDevice.getClass().getDeclaredField("scale");
                    if (field != null) {
                        field.setAccessible(true);
                        Object scale = field.get(graphicsDevice);
                        if (scale instanceof Integer && ((Integer) scale).intValue() == 2) {
                            return true;
                        }
                    }
            } else {
                AffineTransform transform = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                                .getDefaultScreenDevice()
                                                .getDefaultConfiguration()
                                                .getDefaultTransform();
                if (! transform.isIdentity()) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException e) {
            // ignored (can legitimately occur from reflection tricks above)
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return false;
    }
}
