/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator.view;

import iterator.Explorer;
import iterator.model.IFS;
import iterator.model.Transform;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Rendered IFS viewer.
 */
public class Viewer extends JPanel implements ActionListener, KeyListener {
    /** serialVersionUID */
    private static final long serialVersionUID = -1;

    public static final Logger LOG = LoggerFactory.getLogger(Viewer.class);

    public static final Color[] COLORS = new Color[] {
        Color.GREEN, Color.BLUE, Color.RED, Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.BLACK, Color.YELLOW
    };

    private final Explorer controller;

    private IFS ifs;
    private BufferedImage image;
    private Timer timer  = new Timer(10, this);
    private double x, y;
    private Random random = new Random();

    public Viewer(EventBus bus, Explorer controller) {
        super();

        this.controller = controller;

        bus.register(this);
    }

    @Subscribe
    public void update(IFS ifs) {
        this.ifs = ifs;
        reset();
    }

    @Subscribe
    public void size(Dimension size) {
        reset();
    }

    @Override
    public void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_BILINEAR), 0, 0);
        g.dispose();
    }

    private void reset() {
        if (isVisible()) {
	        image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
	        Graphics2D g = image.createGraphics();
	        g.setColor(Color.WHITE);
	        g.fillRect(0, 0, getWidth(), getHeight());
	        g.dispose();
	        x = random.nextInt(getWidth());
	        y = random.nextInt(getHeight());
        }
    }

    public void save(File file) {
        try {
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    public void iterate(int n) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        for (int i = 0; i < n; i++) {
            int j = random.nextInt(ifs.getTransforms().size());
            Transform t = ifs.getTransforms().get(j);
            Color c = controller.isColour() ? COLORS[j % COLORS.length] : Color.BLACK;
	        g.setPaint(new Color(c.getRed(), c.getGreen(), c.getBlue(), 4));
            double[] src = new double[] { x, y };
            double[] dst = new double[2];
            t.getTransform().transform(src, 0, dst, 0, 1);
            x = dst[0]; y = dst[1];
            Rectangle rect = new Rectangle((int) Math.floor(x + 0.5d), (int) Math.floor(y + 0.5d), 2, 2);
            g.fill(rect);
        }
        g.dispose();
    }

    /** @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent) */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (ifs != null && !ifs.getTransforms().isEmpty() && image != null) {
            iterate(10000);
	        repaint();
        }
    }

    public void start() {
        reset();
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) {
    }

    /** @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent) */
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && isVisible()) {
            if (timer.isRunning()) {
                timer.stop();
            } else {
                timer.start();
            }
        }
    }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) {
    }
}
