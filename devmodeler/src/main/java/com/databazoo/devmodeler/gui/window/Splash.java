package com.databazoo.devmodeler.gui.window;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.DevModeler;
import com.databazoo.tools.Dbg;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Initial splash window.
 *
 * @author bobus
 */
public class Splash extends JWindow {
    private static BufferedImage img;
    private static Splash instance;
    private static int loadedParts = 0;
    private static final String[] PARTS = new String[] { "Initialize", "Prepare LAF", "Prepare main window", "Load project list", "Check drivers",
            "Check JRE version", "Reopen last project", "All done" };
    private static final Dimension SIZE = new Dimension(400, 300);
    private static final Color COLOR_RED_SPLASH = Color.decode("#9F2927");

    public static synchronized Splash get() {
        if (instance == null) {
            try {
                SwingUtilities.invokeAndWait(() -> instance = new Splash());
            } catch (InterruptedException | InvocationTargetException e) {
                Dbg.fixme("Splash creation interrupted", e);
                throw new IllegalStateException("Splash creation interrupted", e);
            }
        }
        return instance;
    }

    private Splash() {
        try {
            img = ImageIO.read(DevModeler.class.getResource("/gfx/splash.png"));
        } catch (IOException e) {
            Dbg.notImportant("Nothing we can do.", e);
        }
        getContentPane().add(new ImageComponent());
        setSize(SIZE.width, SIZE.height + 3);
        getContentPane().setBackground(Color.WHITE);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        setVisible(GCFrame.SHOW_GUI);
    }

    public void partLoaded() {
        if (img == null) {
            return;
        }
        loadedParts++;
        if (loadedParts >= PARTS.length) {
            loadedParts = PARTS.length - 1;
            Dbg.fixme("loading more parts than Splash expects");
        }
        repaint();
    }

    @Override
    public void dispose() {
        super.dispose();
        img = null;
    }

    public boolean alreadyLoaded() {
        return loadedParts > 0;
    }

    private static class ImageComponent extends Component {
        private ImageComponent() {
            setPreferredSize(new Dimension(SIZE.width, SIZE.height));
        }

        @Override
        public void paint(Graphics g) {
            g.drawImage(img, 0, 0, null);
            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(COLOR_RED_SPLASH);
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(0, SIZE.height + 1, SIZE.width * loadedParts / (PARTS.length - 1), SIZE.height + 1);
            //g2.drawString(parts[loadedParts], 4, imgSize.height-6);
            g2.drawString(PARTS[loadedParts], 250, 240);
        }
    }
}
