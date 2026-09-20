package afdpainter.gui;

import java.awt.Color;

/**
 * Paleta de colores de la aplicación.
 * Restricción del usuario: NO usar azul ni café/marrón.
 */
public class Palette {
    public static final Color CANVAS_BG     = new Color(248, 248, 245);
    public static final Color STATE_FILL    = Color.WHITE;
    public static final Color STATE_BORDER  = new Color(55, 55, 55);
    public static final Color TRANSITION    = new Color(110, 110, 110);
    public static final Color TEXT          = new Color(25, 25, 25);

    public static final Color CURRENT_STATE = new Color(230, 126, 34);  // naranja
    public static final Color ACTIVE_EDGE   = new Color(230, 126, 34);  // naranja
    public static final Color ACCEPT        = new Color(39, 174, 96);   // verde
    public static final Color REJECT        = new Color(192, 57, 43);   // rojo
    public static final Color SELECTED_HINT = new Color(214, 48, 122);  // rosa/magenta
    public static final Color LABEL_BG      = new Color(255, 255, 255, 235);
}
