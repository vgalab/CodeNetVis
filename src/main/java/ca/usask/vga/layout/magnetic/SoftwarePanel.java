package ca.usask.vga.layout.magnetic;

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

        // STYLE panel
        innerPanel.add(createStylePanel());

        // IMPORT panel
        innerPanel.add(createImportPanel());

        // SEARCH panel
        innerPanel.add(createSearchPanel());

        // FILTERING panel
        innerPanel.add(createFilterPanel());

        // LAYOUT panel
        innerPanel.add(createLayoutPanel());

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
     * Describes the "Pole selection" panel components and functionality.
     * The panel is automatically disabled when no network is loaded.
     */
    protected JPanel createSearchPanel() {
        var panel = createTitledPanel("Pole selection");
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

        addExplanation(panel, "See the \"Node Table\" tab for search results. To select poles manually, right click " +
                "a node or use the red/blue pole buttons on the toolbar.");

        return autoDisable(panel);
    }

    /**
     * Describes the "Filtering" panel components and functionality.
     * The panel is automatically disabled when no network is loaded.
     */
    protected JPanel createFilterPanel() {
        var panel = createTitledPanel("Filtering");
        panel.closeContent();

        var b1 = new JRadioButton("All dependencies", true);
        var b2 = new JRadioButton("Unique dependencies", false);

        var group = new ButtonGroup();
        group.add(b1);
        group.add(b2);

        b2.addChangeListener(e -> style.setShowUnique(b2.isSelected()));
        onSessionLoaded.add(e -> b1.setSelected(true));
        panel.add(group(b1, b2));

        var input = new JComboBox<>(new DefaultComboBoxModel<>(style.getPackageFilterOptions()));

        Consumer<Object> updateModel = (Object e) -> {
            input.setModel(new DefaultComboBoxModel<>(style.getPackageFilterOptions()));
        };

        onNewView.add(updateModel::accept);
        onSessionLoaded.add(updateModel::accept);
        onFileLoaded.add(updateModel::accept);

        input.setEditable(true);

        input.addActionListener(e -> style.setFilterPrefix(input.getSelectedItem() == null ? "" : input.getSelectedItem().toString()));

        var subgraphButton = new TooltipButton("Subgraph", "Create a subgraph to show only the selected package and nodes",
                l -> layout.createSubnetworkFromVisible());

        panel.add(groupBox(new JLabel("Prefix:"), input, subgraphButton));

        return autoDisable(panel);
    }

    /**
     * Describes the "Layout" panel components and functionality.
     * The panel is automatically disabled when no network is loaded.
     */
    protected JPanel createLayoutPanel() {
        var panel = createTitledPanel("Layout");
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

        return autoDisable(panel);
    }

    /**
     * Describes the "Style" panel components and functionality.
     * The panel is automatically disabled when no network is loaded.
     */
    protected JPanel createStylePanel() {
        var panel = createTitledPanel("Style");

        // CONTENTS
        var b0 = new JRadioButton("None");
        var b1 = new JRadioButton("Package");
        var b2 = new JRadioButton("Closest pole");
        var group = new ButtonGroup();
        group.add(b0);
        group.add(b1);
        group.add(b2);
        b0.setSelected(true);

        b0.addChangeListener(l -> {
            if (b0.isSelected()) style.setCurrentColoring(SoftwareStyle.Coloring.NONE);
        });
        b1.addChangeListener(l -> {
            if (b1.isSelected()) style.setCurrentColoring(SoftwareStyle.Coloring.PACKAGE);
        });
        b2.addChangeListener(l -> {
            if (b2.isSelected()) style.setCurrentColoring(SoftwareStyle.Coloring.CLOSEST_POLE);
        });

        panel.add(groupBox(new JLabel("Color nodes by"), Box.createHorizontalGlue(), b0, b1, b2));

        var comboBox = new JComboBox<>(SoftwareStyle.SizeEquation.getAllowedList());
        comboBox.addItemListener(e -> style.setSizeEquation((SoftwareStyle.SizeEquation) e.getItem()));
        panel.add(group(new JLabel("Node size based on"), comboBox));

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

        addExplanation(panel, "The style section allows you to change the size and color of the nodes. Coloring " +
                "by package will use the Java package information to group items in similar packages together. Coloring " +
                "by pole will set the color to the closest pole, if there are poles selected.");

        return autoDisable(panel);
    }

    /**
     * Describes the "Experimental" panel components and functionality.
     * The panel is automatically disabled when no network is loaded.
     */
    protected JPanel createExperimentalPanel() {
        var panel = createTitledPanel("Experimental");
        panel.closeContent();

        var cutConnections = new TooltipButton("Cut connections between colors",
                "Cut all connections between colors of different poles",
                l -> layout.cutCommonConnections());
        panel.add(group(cutConnections));


        var edgePartialColoring = new TooltipButton("Partial edge coloring view",
                "TODO",
                l -> layout.createPartialColoring());
        panel.add(group(edgePartialColoring));

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
     * Automatically disables the given component there is no network loaded,
     * and when an immutable partial coloring network is loaded.
     */
    private <T extends Component> T autoDisable(T component) {
        if (component instanceof JPanel)
            for (var c : ((JPanel) component).getComponents())
                autoDisable(c);
        onNewView.add(e -> component.setEnabled(style.am.getCurrentNetworkView() == null ||
                !layout.isImmutable(style.am.getCurrentNetwork())));
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
        var text = label.getText();
        text = text.split(":")[0];
        String valString = value + "";
        if (value == Math.round(value)) valString = Math.round(value) + "";
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
        panel.add(group(10, new JSeparator())); // set to 15 to show separator

        var textExplanation = new JTextArea(text);
        textExplanation.setLineWrap(true);
        textExplanation.setWrapStyleWord(true);
        textExplanation.setBackground(panel.getBackground());
        textExplanation.setFont(new Label().getFont());
        textExplanation.setEditable(false);
        panel.add(group(75, textExplanation));
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
