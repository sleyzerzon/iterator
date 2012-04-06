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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Closeables;

/**
 * IFS Explorer main class.
 *
 * @author andrew.international@gmail.com
 */
public class Explorer extends JFrame implements KeyListener {
    /** serialVersionUID */
    private static final long serialVersionUID = -2003170067188344917L;

    public static final Logger LOG = LoggerFactory.getLogger(Explorer.class);
    
    public static final String EXPLORER = "IFS Explorer";
    public static final String EDITOR = "Editor";
    public static final String VIEWER = "Viewer";
    
    public static final String FULLSCREEN_OPTION = "-F";
    
    private boolean fullScreen = false;
    
    private IFS ifs;

    private JMenuBar menuBar;
    private Editor editor;
    private Viewer viewer;
    private JPanel view;
    private CardLayout cards;
    private String current;
    private JCheckBoxMenuItem showEditor, showViewer, showDetails;
    private JMenuItem export, save, saveAs;
    
    private EventBus bus;

    public Explorer(String...argv) {
        super(EXPLORER);

        // Parse arguments
        if (argv.length == 1 && argv[0].equalsIgnoreCase(FULLSCREEN_OPTION)) {
            fullScreen = true;
        }

        // Setup full-screen mode if required
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = environment.getDefaultScreenDevice();
        if (fullScreen && device.isFullScreenSupported()) {
            setUndecorated(true);
            setResizable(false);
            device.setFullScreenWindow(this);
        }

        // Setup event bus
        bus = new EventBus(EXPLORER);
        bus.register(this);
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
                FileNameExtensionFilter filter = new FileNameExtensionFilter("XML Files", "xml");
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(filter);
                int result = chooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    IFS loaded = load(chooser.getSelectedFile());
                    loaded.setSize(getSize());
	                Explorer.this.bus.post(loaded);
                }
            }
        });
        save = new JMenuItem(new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                File saveAs = new File(ifs.getName() + ".xml");
                save(saveAs);
	            save.setEnabled(false);
            }
        });
        save.setEnabled(false);
        saveAs = new JMenuItem(new AbstractAction("Save As...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter("XML Files", "xml");
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(filter);
                chooser.setSelectedFile(new File(ifs.getName() + ".xml"));
                int result = chooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File saveAs = chooser.getSelectedFile();
                    ifs.setName(saveAs.getName().replace(".xml", ""));
                    save(saveAs);
	                Explorer.this.bus.post(ifs);
                }
            }
        });
        saveAs.setEnabled(false);
        export = new JMenuItem(new AbstractAction("Export...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG Image Files", "png");
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(filter);
                chooser.setSelectedFile(new File(ifs.getName() + ".png"));
		        int result = chooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    viewer.save(chooser.getSelectedFile());
                }
            }
        });
        file.add(save);
        file.add(saveAs);
        file.add(export);
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
        showDetails.setEnabled(false);
        ButtonGroup displayGroup = new ButtonGroup();
        displayGroup.add(showEditor);
        displayGroup.add(showViewer);
        displayGroup.add(showDetails);
        menuBar.add(system);
        setJMenuBar(menuBar);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            /** @see WindowListener#windowClosed(WindowEvent) */
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);   
            }
        });
        
        editor = new Editor(bus, this);
        viewer = new Viewer(bus, this);

        cards = new CardLayout();
        view = new JPanel(cards);
        view.add(editor, EDITOR);
        view.add(viewer, VIEWER);
        content.add(view, BorderLayout.CENTER);
        show(EDITOR);
        showEditor.setSelected(true);

        setContentPane(content);

        if (!fullScreen) {
	        Dimension minimum = new Dimension(500, 500);
            editor.setMinimumSize(minimum);
            editor.setSize(minimum);
            viewer.setMinimumSize(minimum);
            viewer.setSize(minimum);
	        Dimension size = new Dimension(500, 500 + menuBar.getHeight());
	        setSize(size);
	        setMinimumSize(size);
	        addComponentListener(new ComponentAdapter() {
	            @Override
				public void componentResized(ComponentEvent e) {
	                Dimension s = getSize();
	                int side = Math.min(s.width, s.height - menuBar.getHeight());
	                setSize(side,  side + menuBar.getHeight());
	                Explorer.this.bus.post(getSize());
	            }
            });
        }
        
        setFocusable(true);
        requestFocusInWindow();
        addKeyListener(this);
        addKeyListener(editor);

        IFS untitled = new IFS("Untitled");
        bus.post(untitled);
        
        setVisible(true);
    }

    private void show(String name) {
        cards.show(view, name);
        current = name;
        if (name.equals(VIEWER)) {
	        export.setEnabled(true);
	        viewer.start();
        } else {
	        export.setEnabled(false);
            viewer.stop();
        }
    }
    
    @Subscribe
    public void update(IFS ifs) {
        this.ifs = ifs;
        setTitle(ifs.getName());
        if (!ifs.getTransforms().isEmpty()) {
            save.setEnabled(true);
            saveAs.setEnabled(true);
        }
     }

    public void save(File file) {
       try {
          JAXBContext context = JAXBContext.newInstance(IFS.class);
          Marshaller marshaller = context.createMarshaller();
          marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
          FileWriter writer = new FileWriter(file);
          marshaller.marshal(ifs, writer);
          Closeables.closeQuietly(writer);
       } catch (Exception e) {
          throw Throwables.propagate(e);
       }
    }

    public IFS load(File file) {
       try {
          JAXBContext context = JAXBContext.newInstance(IFS.class);
          FileReader reader = new FileReader(file);
          Unmarshaller unmarshaller = context.createUnmarshaller();
          IFS ifs = (IFS) unmarshaller.unmarshal(reader);
          Closeables.closeQuietly(reader);
          return ifs;
       } catch (Exception e) {
          throw Throwables.propagate(e);
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
    public static void main(final String...argv) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
		        Explorer explorer = new Explorer(argv);
		        explorer.start();
            }
        });
    }
}
