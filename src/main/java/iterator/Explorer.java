/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator;

import iterator.model.IFS;
import iterator.view.Editor;
import iterator.view.Viewer;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

/**
 * IFS Explorer main class.
 *
 * @author andrew.international@gmail.com
 */
public class Explorer implements KeyListener {
    public static final Logger LOG = LoggerFactory.getLogger(Explorer.class);
    
    public static final String EXPLORER = "IFS Explorer";
    public static final String EDITOR = "Editor";
    public static final String VIEWER = "Viewer";
    
    public static final String FULLSCREEN_OPTION = "-F";
    
    private boolean fullScreen = false;
    
    private JFrame window;
    private JMenuBar menuBar;
    private Editor editor;
    private Viewer viewer;
    private JPanel view;
    private CardLayout cards;
    private String current;
    private JCheckBoxMenuItem showEditor, showViewer, showDetails;
    
    private EventBus bus = new EventBus("explorer");

    public Explorer(String...argv) {
        // Parse arguments
        if (argv.length == 1 && argv[0].equalsIgnoreCase(FULLSCREEN_OPTION)) {
            fullScreen = true;
        }

        // Create explorer window
        window = new JFrame(EXPLORER);
        
        // Setup full-screen mode if required
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = environment.getDefaultScreenDevice();
        if (fullScreen && device.isFullScreenSupported()) {
            window.setUndecorated(true);
            window.setResizable(false);
            device.setFullScreenWindow(window);
        }
    }

    @SuppressWarnings("serial")
    public void start() {
        JPanel content = new JPanel(new BorderLayout());

        menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.add(new AbstractAction("New IFS") {
            @Override
            public void actionPerformed(ActionEvent e) {
                IFS untitled = new IFS("Untitled");
                Explorer.this.bus.post(untitled);
            }
        });
        file.add(new AbstractAction("Open...") {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        file.add(new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        file.add(new AbstractAction("Save As...") {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        file.add(new AbstractAction("Preferences...") {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        file.add(new AbstractAction("Quit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        menuBar.add(file);
        JMenu system = new JMenu("Display");
        showEditor = new JCheckBoxMenuItem(new AbstractAction("Editor") {
            @Override
            public void actionPerformed(ActionEvent e) {
                show(EDITOR);
            }
        });
        showViewer = new JCheckBoxMenuItem(new AbstractAction("Viewer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                show(VIEWER);
            }
        });
        showDetails = new JCheckBoxMenuItem(new AbstractAction("Details") {
            @Override
            public void actionPerformed(ActionEvent e) {
//                show(DETAILS);
            }
        });
        system.add(showEditor);
        system.add(showViewer);
        system.add(showDetails);
        ButtonGroup displayGroup = new ButtonGroup();
        displayGroup.add(showEditor);
        displayGroup.add(showViewer);
        displayGroup.add(showDetails);
        menuBar.add(system);
        window.setJMenuBar(menuBar);

        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        WindowListener windowListener = new WindowAdapter() {
            /** @see WindowListener#windowClosed(WindowEvent) */
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);   
            }
        };
        window.addWindowListener(windowListener);
        
        editor = new Editor(bus, this);
        viewer = new Viewer(bus, this);

        cards = new CardLayout();
        view = new JPanel(cards);
        view.add(editor, EDITOR);
        view.add(viewer, VIEWER);
        content.add(view, BorderLayout.CENTER);
        show(EDITOR);
        showEditor.setSelected(true);

        window.setContentPane(content);

        if (!fullScreen) {
	        Dimension minimum = new Dimension(500, 500);
            editor.setMinimumSize(minimum);
            editor.setSize(minimum);
            viewer.setMinimumSize(minimum);
            viewer.setSize(minimum);
	        Dimension size = new Dimension(500, 500 + menuBar.getHeight());
	        window.setSize(size);
	        window.setMinimumSize(size);
	        window.addComponentListener(new ComponentAdapter() {
	            @Override
				public void componentResized(ComponentEvent e) {
	                Dimension s = window.getSize();
	                int side = Math.min(s.width, s.height - menuBar.getHeight());
	                window.setSize(side,  side + menuBar.getHeight());
	                Explorer.this.bus.post(window.getSize());
	            }
            });
        }
        
        window.setFocusable(true);
        window.requestFocusInWindow();
        window.addKeyListener(this);
        window.addKeyListener(editor);

        IFS untitled = new IFS("Untitled");
        bus.post(untitled);
        
        window.setVisible(true);
    }

    private void show(String name) {
        cards.show(view, name);
        current = name;
        if (name.equals(VIEWER)) {
	        viewer.start();
        } else {
            viewer.stop();
        }
    }
    
    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) {
    }

    /** @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent) */
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (current.equals(EDITOR)) {
                showViewer.setSelected(true);
                show(VIEWER);
            } else if (current.equals(VIEWER)) {
                showEditor.setSelected(true);
                show(EDITOR);
            }
        }
    }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) {
    }

    /**
     * Explorer
     */
    public static void main(String...argv) throws Exception {
        Explorer explorer = new Explorer(argv);
        explorer.start();
    }
}
