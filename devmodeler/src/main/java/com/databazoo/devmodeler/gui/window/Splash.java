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
        //getContentPane().add(new ImageComponent("/gfx/splash/splash.png", Color.decode("#9F2927"), new Point(250, 240)));
        //getContentPane().setBackground(Color.WHITE);
        getContentPane().add(new ImageComponent("/gfx/splash/splash3.png", Color.decode("#f289b2"), new Point(10, 290)));
        getContentPane().setBackground(Color.decode("#2f2754"));
        //getContentPane().add(new ImageComponent("/gfx/splash/splash4.png", Color.decode("#d6a76e"), new Point(70, 290)));
        //getContentPane().setBackground(Color.WHITE);
        //getContentPane().add(new ImageComponent("/gfx/splash/splash5.png", Color.decode("#f31e3c"), new Point(420, 290)));
        //getContentPane().setBackground(Color.decode("#2a1800"));
        //getContentPane().add(new ImageComponent("/gfx/splash/splash6.png", Color.decode("#aeabfc"), null));
        //getContentPane().setBackground(Color.decode("#5e4654"));
        //getContentPane().add(new ImageComponent("/gfx/splash/splash7.png", Color.decode("#c70e1a"), new Point(30, 200)));
        //getContentPane().setBackground(Color.decode("#d7e5f2"));
        //getContentPane().add(new ImageComponent("/gfx/splash/splash8.png", Color.decode("#fa9d07"), new Point(10, 290)));
        //getContentPane().setBackground(Color.BLACK);

        setSize(SIZE.width, SIZE.height + 3);
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
        private final Point textLocation;

        private ImageComponent(String imageURI, Color progressColor, Point textLocation) {
            this.textLocation = textLocation;
            setForeground(progressColor);
            try {
                img = ImageIO.read(DevModeler.class.getResource(imageURI));
                SIZE.setSize(img.getWidth(), img.getHeight());
                setPreferredSize(SIZE);
            } catch (IOException e) {
                Dbg.notImportant("Nothing we can do.", e);
            }
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D graphics = (Graphics2D) g;
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            graphics.drawImage(img, 0, 0, null);
            graphics.setPaint(getForeground());
            graphics.setStroke(new BasicStroke(3));
            graphics.drawLine(0, SIZE.height + 1, SIZE.width * loadedParts / (PARTS.length - 1), SIZE.height + 1);
            if (textLocation != null) {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.drawString(PARTS[loadedParts], textLocation.x, textLocation.y);
            }
        }
    }
}
