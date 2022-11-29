package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.io.JGitCloneRepository;
import ca.usask.vga.layout.magnetic.io.JGitMetadataInput;
import ca.usask.vga.layout.magnetic.poles.ExtraTasks;
import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Describes the full layout and functionality of the custom "software" panel.
 * The panel uses customized Swing elements, as well as functionality classes.
 * @see SoftwareImport
 * @see SoftwareLayout
 * @see SoftwareStyle
 */
public class SoftwarePanel extends JPanel implements CytoPanelComponent2, SessionLoadedListener, SetCurrentNetworkViewListener {

    public static final String title = "Software Layout", identifier = "software-panel";
    private final Icon icon = new ImageIcon(getClass().getResource("/icons/add_pole_N_icon_16.png"));

    private final SoftwareLayout layout;
    private final SoftwareStyle style;
    private final SoftwareImport importS;
    private final CySwingApplication swingApp;
    private final DialogTaskManager dtm;

    private final int ENTRY_HEIGHT = 35;

    private final List<SessionLoadedListener> onSessionLoaded = new ArrayList<>();
    private final List<SetCurrentNetworkViewListener> onNewView = new ArrayList<>();
    private final List<Consumer<String>> onFileLoaded = new ArrayList<>();

    /**
     * Initialize the panel with the given Swing application, task manager, layout, style, and import function classes.
     */
    protected SoftwarePanel(CySwingApplication swingApp, DialogTaskManager dtm, SoftwareLayout layout, SoftwareStyle style, SoftwareImport importS) {
        super();
        this.swingApp = swingApp;
        this.dtm = dtm;
        this.layout = layout;
        this.style = style;
        this.importS = importS;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel innerPanel = new JPanel() {
            public Dimension getPreferredSize() {
                return new Dimension(getParent().getSize().width, super.getPreferredSize().height);
            }
        };

        // IMPORT panel
        innerPanel.add(createImportPanel());

        // CREATE LAYOUT panel
        innerPanel.add(createMakeLayoutPanel());

        // OLD SEARCH panel
        // innerPanel.add(createSearchPanel());
        // OLD LAYOUT panel
        // innerPanel.add(createLegacyLayoutPanel());

        // FILTERING panel
        innerPanel.add(createFilterPanel());

        // STYLE panel
        innerPanel.add(createStylePanel());

        // EXPERIMENTAL panel
        innerPanel.add(createExperimentalPanel());

        // Scroll Pane
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        innerPanel.add(Box.createVerticalGlue());

        var scrollPane = new JScrollPane(innerPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane);
    }

    /**
     * Updates the layout and style panels when a new Cytoscape session is loaded.
     */
    @Override
    public void handleEvent(SessionLoadedEvent sessionLoadedEvent) {
        style.getRingsAnnotation().reset();
        style.getRadiusAnnotation().reset();
        for (var l : onSessionLoaded) l.handleEvent(sessionLoadedEvent);
    }

    /**
     * Updates the layout and style panels when a new network view is selected.
     */
    @Override
    public void handleEvent(SetCurrentNetworkViewEvent e) {
        for (var l : onNewView) l.handleEvent(e);
    }

    /**
     * Creates a custom panel with a border, margins and title.
     */
    protected CollapsiblePanel createTitledPanel(String title) {
        return new CollapsiblePanel(title);
    }

    /**
     * Describes the "Data import" panel components and functionality.
     */
    protected JPanel createImportPanel() {
        var panel = createTitledPanel("Data import");
        panel.closeContent();

        var clearCache = new TooltipButton("Clear cache", "Clear cache of all downloaded repositories");
        clearCache.addActionListener(l -> {
            importS.clearTempDir();
            clearCache.setText("Clear cache " + importS.getTempDirSize());
        });

        var gitLink = new JTextField("https://github.com/BJNick/CytoscapeMagneticLayout");
        panel.add(groupBox(new JLabel("GitHub Link:"), gitLink,
                new TooltipButton("Load", "Downloads and imports all Java classes from the repository", l -> {
                    try {
                        importS.loadFromGitHub(gitLink.getText(), (it) -> {
                            onFileLoaded(it);
                            clearCache.setText("Clear cache " + importS.getTempDirSize());
                        });
                    } catch (IllegalArgumentException e) {
                        JOptionPane.showMessageDialog(gitLink, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                })));

        /*var srcFolder = new JTextField("");
        panel.add(groupBox(new JLabel("Source code folder:"), srcFolder, addListener(new JButton("Load"),
                e -> importS.loadFromSrcFolder(srcFolder.getText(), this::onFileLoaded))));*/

        var bFile = new TooltipButton("Load from JAR file", "Imports all Java classes from the JAR file",
                e -> importS.loadFromFile(this::onFileLoaded));

        var bFolder = new TooltipButton("Load from Java SRC folder", "Imports all Java classes from the SRC folder",
                e -> importS.loadFromSrcFolder(importS.chooseSrcFolderDialogue(null), this::onFileLoaded));

        panel.add(group(bFile, bFolder));

        panel.add(group(/*new JLabel(bold("Data import")),*/ clearCache));

        addExplanation(panel, "To load a graph, paste the GitHub link to a Java project, or open it locally " +
                "from a JAR file or a Java SRC folder. " +
                "GitHub files are automatically cached, but it is possible to clear the cache to free storage. " +
                "To open a generic directed graph, use the built-in Cytoscape functionality. ");

        return panel;
    }

    /**
     * Updates the layout and style panels when a new file is loaded.
     */
    protected void onFileLoaded(String filename) {
        var format = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!filename.contains(".")) format = "java";
        layout.layoutOnLoad();
        style.onFileLoaded(format);
        for (var l : onFileLoaded) l.accept(filename);
    }

    /**
     * Describes the "Create layout" panel components and functionality.
     * This panel is merged from "Search" and "Layout" panels
     * The panel is automatically disabled when no network is loaded.
     */
    protected JPanel createMakeLayoutPanel() {
        var panel = createTitledPanel("Create layout");
        panel.closeContent();

        // the SEARCH part of the panel

        // Set poles button
        var poleCountText = "Total number of poles: ";

        var poleCount = new JLabel(poleCountText + style.pm.getPoleCount(style.am.getCurrentNetwork()));
        style.pm.addChangeListener(() -> poleCount.setText(poleCountText+style.pm.getPoleCount(style.am.getCurrentNetwork())));

        panel.add(group(poleCount, new TooltipButton("Set poles by top degree...",
                "Choose the number of poles to be set by top indegree or outdegree",
                e -> dtm.execute(new TaskIterator(new ExtraTasks.MakeTopDegreePoles(style.am, style.pm))))));

        // Search field
        var searchField = new JTextField();

        searchField.addActionListener(e -> searchNetworkFor(searchField.getText().strip()));
        onSessionLoaded.add(e -> searchField.setText(""));

        panel.add(label("Search nodes by name:", searchField));


        // the LAYOUT part of the panel
        panel.add(group(10, new JSeparator())); // set to 15 to show separator, 10 is a small gap

        var initialRadius = Math.round(style.getSuggestedRadius())/100;
        var radiusEditor = createCustomSlider(0, 100, initialRadius, 25, 5, 5);

        radiusEditor.addChangeListener(e -> layout.setPinRadius(radiusEditor.getValue()*100));
        radiusEditor.addChangeListener(e -> style.getRadiusAnnotation().setRadius(radiusEditor.getValue()*100));
        radiusEditor.addMouseListener(annotationOnMouse(style.getRadiusAnnotation()));
        fireChangeListeners(radiusEditor);

        panel.add(label("Pin radius:", "Off", radiusEditor));

        onSessionLoaded.add(e -> {
            radiusEditor.setValue(Math.round(style.getSuggestedRadius()) / 100);
            fireChangeListeners(radiusEditor);
        });

        var ringsEditor = createCustomSpinner(0, 100, 4, 1);

        ringsEditor.addChangeListener(e -> layout.setMaxRings((Integer) ringsEditor.getValue()));
        ringsEditor.addChangeListener(e -> style.getRingsAnnotation().setMaxRings((Integer) ringsEditor.getValue()));
        layout.setMaxRings((Integer) ringsEditor.getValue());

        ((JSpinner.NumberEditor)ringsEditor.getEditor()).getTextField()
                .addMouseListener(annotationOnMouse(style.getRingsAnnotation()));

        panel.add(label("Number of rings:", ringsEditor));

        var runPolarLayout = new TooltipButton("Run pole layout",
                "At least 1 pole is required to run the pole layout",
                e -> {
                    int count = style.pm.getPoleCount(style.am.getCurrentNetwork());
                    Runnable onComplete = () -> {
                        // On layout complete
                        style.getRadiusAnnotation().reposition();
                        style.getRingsAnnotation().reposition();
                    };
                    if (count == 0) {
                        layout.runLinearLayout(onComplete);
                    } else {
                        layout.runLayout(onComplete);
                    }
                }
        );

        Runnable update = () -> {
            var count = style.pm.getPoleCount(style.am.getCurrentNetwork());
            runPolarLayout.setText(count > 0
                    ? "Run pole layout (" + count + (count > 1 ? " poles"  : " pole") +  " selected)"
                    : "Run linear layout (No poles selected)");
        };

        style.pm.addChangeListener(update);
        panel.add(group(runPolarLayout));
        update.run();

        addExplanation(panel, 165, "You can select a set of nodes and pin them by degree. " +
                "To pin specific nodes, left click on a node and use the red/blue pole buttons on the toolbar. " +
                "You can search for a node and see the \"Node Table\" tab for search results. " +
                "To view nodes color-coded by the nearest pole, change coloring in the \"Layout aesthetics\" panel. " +
                "\n\n" +
                "If there are no poles selected, the layout is linear, aligning all edges left to right. " +
                "If poles are present, the edges would point towards the pole that they're closest to. " +
                "Pin radius specifies how far poles are placed in a circle. " +
                "Number of rings specifies the number of hierarchy levels around each pole.");

        return autoDisable(panel);
    }

    /**
     * Describes the old "Pole selection" panel components and functionality.
     * @deprecated The panel is automatically disabled when no network is loaded.
     */
    @Deprecated
    protected JPanel createSearchPanel() {
        var panel = createTitledPanel("Pole selection and node search");
        panel.closeContent();

        var searchField = new JTextField();

        var button = new TooltipButton("Search", "Search nodes for a specific keyword in any of the fields",
                e -> searchNetworkFor(searchField.getText().strip()));
        searchField.addActionListener(e -> searchNetworkFor(searchField.getText().strip()));
        onSessionLoaded.add(e -> searchField.setText(""));

        panel.add(groupBox(searchField, button));

        var poleCountText = "Total number of poles: ";

        var poleCount = new JLabel(poleCountText + style.pm.getPoleCount(style.am.getCurrentNetwork()));
        style.pm.addChangeListener(() -> poleCount.setText(poleCountText+style.pm.getPoleCount(style.am.getCurrentNetwork())));

        panel.add(group(poleCount, new TooltipButton("Set poles by top degree...",
                "Choose the number of poles to be set by top indegree or outdegree",
                e -> dtm.execute(new TaskIterator(new ExtraTasks.MakeTopDegreePoles(style.am, style.pm))))));

        addExplanation(panel, "See the \"Node Table\" tab for search results. " +
                "To select poles manually, left click on a node and use the red/blue pole buttons on the toolbar. " +
                "Selecting nodes by degree works well with 3 to 10 poles. " +
                "To view nodes color-coded by the nearest pole, change coloring in the \"Visual style\" panel.");

        return autoDisable(panel);
    }

    /**
     * Describes the "Filtering" panel components and functionality.
     * The panel is automatically disabled when no network is loaded.
     */
    protected JPanel createFilterPanel() {
        var panel = createTitledPanel("Data filtering");
        panel.closeContent();

        // Package selection drop down
        var input = new JComboBox<>(new DefaultComboBoxModel<>(style.getPackageFilterOptions()));

        Consumer<Object> updateModel = (Object e) -> {
            input.setModel(new DefaultComboBoxModel<>(style.getPackageFilterOptions()));
        };

        onNewView.add(updateModel::accept);
        onSessionLoaded.add(updateModel::accept);
        onFileLoaded.add(updateModel::accept);

        input.setEditable(true);

        input.addActionListener(e -> style.setFilterPrefix(input.getSelectedItem() == null ? "" : input.getSelectedItem().toString()));

        panel.add(groupBox(new JLabel("Package:"), input));

        panel.add(group(10, new JSeparator())); // set to 15 to show separator, 10 is a small gap

        // Radio buttons for dependencies
        var b1 = new JRadioButton("All dependencies", true);
        var b2 = new JRadioButton("Pole-specific dependencies", false);

        var group = new ButtonGroup();
        group.add(b1);
        group.add(b2);

        b2.addChangeListener(e -> style.setShowUnique(b2.isSelected()));
        onSessionLoaded.add(e -> b1.setSelected(true));
        panel.add(group(b1, b2));
        onNewView.add(e -> { // reapply filters on changed view
            if (e.getNetworkView() != null) {
                style.setFilterPrefix("");
                b1.setSelected(true);
                style.reapplyFilters();
            }
        });

        // Create subgraph copy button
        var subgraphButton = new TooltipButton("Create a dataset copy using current layout",
                "Lets you make changes to the copy without impacting the original",
                l -> layout.createSubnetworkFromVisible());

        panel.add(group(subgraphButton));

        addExplanation(panel, "Pole-specific dependencies hides a node if it is not connected to a pole or " +
                "if its closest pole is not unique. " +
                "Select a package to only show the nodes in that package. " +
                "To create a dataset using the currently visible graph, use the Create button. " +
                "All datasets are accessible from the \"Network\" tab.");

        return autoDisable(panel);
    }

    /**
     * Describes the old "Layout" panel components and functionality.
     * @deprecated This panel was merged into "Create Layout" panel and is no longer used on its own.
     */
    @Deprecated
    protected JPanel createLegacyLayoutPanel() {
        var panel = createTitledPanel("Run layout for the dependency graph ");
        panel.closeContent();

        var initialRadius = Math.round(style.getSuggestedRadius())/100;
        var radiusEditor = createCustomSlider(0, 100, initialRadius, 25, 5, 5);

        radiusEditor.addChangeListener(e -> layout.setPinRadius(radiusEditor.getValue()*100));
        radiusEditor.addChangeListener(e -> style.getRadiusAnnotation().setRadius(radiusEditor.getValue()*100));
        radiusEditor.addMouseListener(annotationOnMouse(style.getRadiusAnnotation()));
        fireChangeListeners(radiusEditor);

        panel.add(label("Pin radius:", radiusEditor));

        onSessionLoaded.add(e -> {
            radiusEditor.setValue(Math.round(style.getSuggestedRadius()) / 100);
            fireChangeListeners(radiusEditor);
        });

        var ringsEditor = createCustomSpinner(0, 100, 4, 1);

        ringsEditor.addChangeListener(e -> layout.setMaxRings((Integer) ringsEditor.getValue()));
        ringsEditor.addChangeListener(e -> style.getRingsAnnotation().setMaxRings((Integer) ringsEditor.getValue()));
        layout.setMaxRings((Integer) ringsEditor.getValue());

        ((JSpinner.NumberEditor)ringsEditor.getEditor()).getTextField()
                .addMouseListener(annotationOnMouse(style.getRingsAnnotation()));

        panel.add(label("Number of rings:", ringsEditor));

        var runPolarLayout = new TooltipButton("Run pole layout",
                "At least 1 pole is required to run the pole layout",
                e -> {
                    int count = style.pm.getPoleCount(style.am.getCurrentNetwork());
                    Runnable onComplete = () -> {
                        // On layout complete
                        style.getRadiusAnnotation().reposition();
                        style.getRingsAnnotation().reposition();
                    };
                    if (count == 0) {
                        layout.runLinearLayout(onComplete);
                    } else {
                        layout.runLayout(onComplete);
                    }
                }
        );

        Runnable update = () -> {
            var count = style.pm.getPoleCount(style.am.getCurrentNetwork());
            runPolarLayout.setText(count > 0
                    ? "Run pole layout (" + count + (count > 1 ? " poles"  : " pole") +  " selected)"
                    : "Run linear layout (No poles selected)");
        };

        style.pm.addChangeListener(update);
        panel.add(group(runPolarLayout));
        update.run();

        addExplanation(panel, "If there are no poles selected, the layout is linear, aligning all edges left to right. " +
                "If poles are present, the edges would point towards the pole that they're closest to. " +
                "Pin radius specifies how far poles are placed in a circle. " +
                "Number of rings specifies the number of hierarchy levels around each pole.");

        return autoDisable(panel);
    }

    /**
     * Describes the "Style" panel components and functionality.
     * The panel is automatically disabled when no network is loaded.
     */
    protected JPanel createStylePanel() {
        var panel = createTitledPanel("Layout aesthetics and style");
        panel.closeContent();

        // CONTENTS
        var comboBoxColor = new JComboBox<>(SoftwareStyle.Coloring.getAllowedList());
        comboBoxColor.addItemListener(e -> style.setCurrentColoring((SoftwareStyle.Coloring) e.getItem()));
        panel.add(group(new JLabel("Color nodes by"), comboBoxColor));
        style.pm.addInitializationListener(() -> comboBoxColor.setSelectedItem(SoftwareStyle.Coloring.CLOSEST_POLE));

        var comboBox = new JComboBox<>(SoftwareStyle.SizeEquation.getAllowedList());
        comboBox.addItemListener(e -> style.setSizeEquation((SoftwareStyle.SizeEquation) e.getItem()));
        panel.add(group(new JLabel("Node size based on"), comboBox));
        style.pm.addInitializationListener(() -> {
            if (comboBox.getSelectedIndex() == 0)
                comboBox.setSelectedItem(SoftwareStyle.SizeEquation.BIGGER_POLES);
        });

        var initialSize = Math.round(style.getInitialNodeSizeValue());
        var sizeEditor = createCustomSlider(0, 100, initialSize, 25, 5, 1);

        sizeEditor.addChangeListener(e -> style.setNodeSize(sizeEditor.getValue()));
        panel.add(label("Node size:", sizeEditor));

        onSessionLoaded.add(e -> sizeEditor.setValue(Math.round(style.getInitialNodeSizeValue())));

        var initialTransparency = Math.round(style.getInitialEdgeTransparency());
        var transparencyEditor = createCustomSlider(0, 255, initialTransparency, 60, 15, 15);

        transparencyEditor.addChangeListener(e -> style.setEdgeTransparency(transparencyEditor.getValue()));
        panel.add(label("Edge visibility:", transparencyEditor));

        onSessionLoaded.add(e -> transparencyEditor.setValue(Math.round(style.getInitialEdgeTransparency())));

        //panel.add(group(new JButton("Choose colors...")));

        addExplanation(panel, "The style section allows to change the size and color of the nodes. " +
                "Coloring by package will use the Java package information to group items in the same packages together. " +
                "Coloring by pole will set the color to the closest pole, if there are poles selected.");

        return autoDisable(panel);
    }

    /**
     * Describes the "Experimental" panel components and functionality.
     * The panel is automatically disabled when no network is loaded.
     */
    protected JPanel createExperimentalPanel() {
        var panel = createTitledPanel("Experimental features");
        panel.closeContent();

        var cutConnections = new TooltipButton("Cut connections between colors",
                "Cut all connections between colors of different poles",
                l -> layout.cutCommonConnections());
        panel.add(group(cutConnections));


        var edgePartialColoring = new TooltipButton("Partial edge coloring view",
                "Subdivides edges into two pieces, to color only the piece oriented towards the pole",
                l -> layout.createPartialColoring());
        panel.add(group(edgePartialColoring));


        var loadGitMetadata = new TooltipButton("Load Git metadata",
                "Loads the Git metadata from the current repository",
                l -> dtm.execute(JGitMetadataInput.loadGitTaskIterator(style.am.getCurrentNetwork())));
        panel.add(group(loadGitMetadata));

        // TODO: Remove. Testing only.
        var cloneGit = new TooltipButton("Clone Git","",
                l -> JGitCloneRepository.cloneTest(dtm));
        panel.add(group(cloneGit));

        return autoDisable(panel);
    }

    /**
     * Creates a JSlider with the given parameters and adds the ability to change using the scroll wheel.
     */
    protected JSlider createCustomSlider(int min, int max, int value, int majorTicks, int minorTicks, int scrollAmount) {
        var slider = new JSlider(min, max, value);
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(majorTicks);
        slider.setMinorTickSpacing(minorTicks);
        // Add ability to change using scroll wheel
        slider.addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            if (!slider.isEnabled()) return;
            if (notches < 0)
                slider.setValue(slider.getValue() + scrollAmount);
            else if (notches > 0)
                slider.setValue(slider.getValue() - scrollAmount);
        });
        return slider;
    }

    /**
     * Creates a JSpinner with the given parameters and adds the ability to change using the scroll wheel.
     */
    protected JSpinner createCustomSpinner(int min, int max, int value, int step) {
        var spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        // Add ability to change using scroll wheel
        spinner.addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            if (!spinner.isEnabled()) return;
            if (notches < 0 && spinner.getModel().getNextValue() != null)
                spinner.setValue(spinner.getModel().getNextValue());
            else if (notches > 0 && spinner.getModel().getPreviousValue() != null)
                spinner.setValue(spinner.getModel().getPreviousValue());
        });
        return spinner;
    }

    /**
     * Creates a bold HTML string that can be used in a JLabel.
     */
    private String bold(String text) {
        return "<html><b>" + text + "</b></html>";
    }

    /**
     * Adds an action listener to the given button and returns the button.
     */
    private AbstractButton addListener(AbstractButton object, ActionListener action) {
        object.addActionListener(action);
        return object;
    }

    /**
     * Appends a label with the given text that is updated when the slider value changes.
     * @param zeroText the text that is displayed when the slider is at 0.
     */
    private JPanel label(String text, String zeroText, JSlider component) {
        var label = new JLabel(text);
        component.addChangeListener(e -> updateLabelValue(label, (component.getValue() == 0) ? zeroText : ""+component.getValue()));
        updateLabelValue(label, (component.getValue() == 0) ? zeroText : ""+component.getValue());
        if (component.getPaintLabels())
            return group(50, label, component);
        return group(label, component);
    }

    /**
     * Appends a label with the given text that is updated when the slider value changes.
     */
    private JPanel label(String text, JSlider component) {
        var label = new JLabel(text);
        component.addChangeListener(e -> updateLabelValue(label, component.getValue()));
        updateLabelValue(label, component.getValue());
        if (component.getPaintLabels())
            return group(50, label, component);
        return group(label, component);
    }

    /**
     * Appends a label with the given text, for any component that is not a JSlider.
     */
    private JPanel label(String text, JComponent component) {
        return group(new JLabel(text), component);
    }

    /**
     * Automatically disables the given component there is no network loaded.
     */
    private <T extends Component> T autoDisable(T component) {
        if (component instanceof JPanel)
            for (var c : ((JPanel) component).getComponents())
                autoDisable(c);
        onNewView.add(e -> component.setEnabled(e.getNetworkView() != null));
        return component;
    }

    /**
     * Disables the given component entirely.
     */
    private <T extends Component> T disable(T component) {
        if (component instanceof JPanel)
            for (var c : ((JPanel) component).getComponents())
                disable(c);
        component.setEnabled(false);
        return component;
    }

    /**
     * Updates the text of the given label to the given value, in the format "property: value".
     */
    private void updateLabelValue(JLabel label, float value) {
        if (value == Math.round(value))
            updateLabelValue(label, ""+Math.round(value));
        else
            updateLabelValue(label, ""+value);
    }

    /**
     * Updates the text of the given label to the given value, in the format "property: value".
     */
    private void updateLabelValue(JLabel label, String value) {
        var text = label.getText();
        text = text.split(":")[0];
        String valString = value + "";
        label.setText(text + ": " + valString);
    }

    /**
     * Creates a single row of components, with the given height.
     * The panel is divided into equally sized cells.
     */
    private JPanel group(int height, Component... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, components.length, 10, 10));
        panel.setBorder(new EmptyBorder(5, 5,5,5));
        if (height >= 0) {
            panel.setMaximumSize(new Dimension(10000, height));
            panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, height));
        }
        for (Component c : components) {
            panel.add(c);
        }
        return panel;
    }

    private JPanel group(Component... components) {
        return group(ENTRY_HEIGHT, components);
    }

    /**
     * Creates a single row of components, with the given height.
     * The panel has the box layout, with extra rigid area between components.
     */
    private JPanel groupBox(int height, Component... components) {
        JPanel panel = group(height, components);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        for (int i = 0; i < components.length-1; i++) {
            panel.add(Box.createRigidArea(new Dimension(10, height)), 1+i*2);
        }
        return panel;
    }

    private JPanel groupBox(Component... components) {
        return groupBox(ENTRY_HEIGHT, components);
    }

    /**
     * Creates a mouse listener that shows the given annotation when the mouse is
     * over the component or pressed, and hides it when the mouse is released and exited.
     */
    private MouseListener annotationOnMouse(SoftwareStyle.TooltipAnnotation annotation) {
        return new MouseAdapter() {
            boolean pressed, entered;
            public void mousePressed(MouseEvent e) {
                pressed = true; annotation.setVisible(true);
            }
            public void mouseReleased(MouseEvent e) {
                pressed = false; if (!entered) annotation.setVisible(false);
            }
            public void mouseEntered(MouseEvent e) {
                entered = true; annotation.setVisible(true);
            }
            public void mouseExited(MouseEvent e) {
                entered = false; if (!pressed) annotation.setVisible(false);
            }
        };
    }

    /**
     * Finds the default search field and sets it to the given prompt.
     * Fires the action event to search and clears the field.
     */
    private void searchNetworkFor(String prompt) {
        Component[] components = swingApp.getJToolBar().getComponents();
        var result =
                Arrays.stream(components).filter(c -> c.getClass().getName().contains("EnhancedSearchPanel")).findFirst();
        if (result.isEmpty()) return;
        var searchPanel = (JPanel) result.get();

        for (Component inner : searchPanel.getComponents()) {
            if (inner instanceof JTextField) {
                var textField = (JTextField) inner;
                textField.setText("*" + prompt + "*");
                textField.postActionEvent();
                textField.setText("");
                return;
            }
        }
    }

    /**
     * Utility class that is a JButton with a tooltip field and action in the constructor.
     */
    class TooltipButton extends JButton {
        public TooltipButton(String text, String tooltip, ActionListener action) {
            super(text);
            setToolTipText(tooltip);
            addListener(this, action);
        }
        public TooltipButton(String text, String tooltip) {
            super(text);
            setToolTipText(tooltip);
        }
    }

    /**
     * A custom panel that can be opened and closed by the user. For use
     * in a list of panels each of which can be expanded.
     */
    class CollapsiblePanel extends JPanel {

        public final JPanel innerPanel, outerPanel;

        private final String OPEN_V = "\u2227", CLOSED_V = "\u2228";
        private final JLabel icon;

        public CollapsiblePanel(String title) {
            outerPanel = this;
            innerPanel = new JPanel();

            outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));
            innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));

            icon = new JLabel(OPEN_V);

            outerPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    innerPanel.setVisible(!innerPanel.isVisible());
                    icon.setText(innerPanel.isVisible() ? OPEN_V : CLOSED_V);
                }
            });


            innerPanel.addMouseListener(new MouseAdapter() {});


            if (title != null && !title.equals("")) {
                super.add(groupBox(new JLabel(bold(title)), icon));
            } else {
                super.add(groupBox(new JLabel(bold("")), icon));
            }

            outerPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0,0,10,0),
                    BorderFactory.createLineBorder(Color.lightGray)));

            super.add(innerPanel);
        }

        public void openContent() {
            innerPanel.setVisible(true);
            icon.setText(OPEN_V);
        }

        public void closeContent() {
            innerPanel.setVisible(false);
            icon.setText(CLOSED_V);
        }

        @Override
        public Component add(Component comp) {
            return innerPanel.add(comp);
        }
    }

    private JTextArea addExplanation(JPanel panel, String text) {
        return addExplanation(panel, 75, text);
    }

    private JTextArea addExplanation(JPanel panel, int height, String text) {
        panel.add(group(10, new JSeparator())); // set to 15 to show separator

        var textExplanation = new JTextArea(text);
        textExplanation.setLineWrap(true);
        textExplanation.setWrapStyleWord(true);
        textExplanation.setBackground(panel.getBackground());
        textExplanation.setFont(new Label().getFont());
        textExplanation.setEditable(false);
        panel.add(group(height, textExplanation));
        return textExplanation;
    }

    private void fireChangeListeners(JSlider component) {
        Arrays.stream(component.getChangeListeners()).forEach(l -> l.stateChanged(new ChangeEvent(component)));
    }

    private void fireChangeListeners(JSpinner component) {
        Arrays.stream(component.getChangeListeners()).forEach(l -> l.stateChanged(new ChangeEvent(component)));
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.WEST;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }
}
