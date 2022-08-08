package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.poles.ExtraTasks;
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

public class SoftwarePanel extends JPanel implements CytoPanelComponent2, SessionLoadedListener {

    public static final String title = "Software Layout", identifier = "software-panel";
    private final Icon icon = new ImageIcon(getClass().getResource("/icons/add_pole_N_icon.png"));

    private final CySwingApplication swingApp;
    private final SoftwareLayout layout;
    private final SoftwareStyle style;
    private final DialogTaskManager dtm;

    private final int ENTRY_HEIGHT = 35;

    private List<SessionLoadedListener> onSessionLoaded;

    protected SoftwarePanel(CySwingApplication swingApp, DialogTaskManager dtm, SoftwareLayout layout, SoftwareStyle style) {
        super();
        this.swingApp = swingApp;
        this.dtm = dtm;
        this.layout = layout;
        this.style = style;
        onSessionLoaded = new ArrayList<>();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel innerPanel = new JPanel() {
            public Dimension getPreferredSize() {
                return new Dimension(getParent().getSize().width, super.getPreferredSize().height);
            }
        };

        // Temporary DATA panel
        JPanel panel = createTitledPanel("Data");
        panel.add(group(new JLabel("GitHub Link:"), new JTextField("www.example.com")));
        innerPanel.add(panel);


        // SEARCH panel
        innerPanel.add(createSearchPanel());

        // FILTERING panel
        innerPanel.add(createFilterPanel());

        // LAYOUT panel
        innerPanel.add(createLayoutPanel());


        // STYLE panel
        innerPanel.add(createStylePanel());


        // Scroll Pane
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        var scrollPane = new JScrollPane(innerPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane);
    }

    @Override
    public void handleEvent(SessionLoadedEvent sessionLoadedEvent) {
        style.getRingsAnnotation().reset();
        style.getRadiusAnnotation().reset();
        for (var l : onSessionLoaded) l.handleEvent(sessionLoadedEvent);
    }

    protected JPanel createTitledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        if (title != null && !title.equals(""))
            panel.add(group(new JLabel(bold(title))));
        //panel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        panel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0,0,10,0),
                BorderFactory.createLineBorder(Color.lightGray)));
        return panel;
    }

    protected JPanel createSearchPanel() {
        var panel = createTitledPanel(null);

        panel.add(groupBox(new JLabel(bold("Pole selection")),
                new JLabel("See the \"Node Table\" tab for search results")));

        var searchField = new JTextField();

        var button = new JButton("Search");
        searchField.addActionListener(e -> searchNetworkFor(searchField.getText().strip()));
        button.addActionListener(e -> searchNetworkFor(searchField.getText().strip()));
        onSessionLoaded.add(e -> searchField.setText(""));

        panel.add(groupBox(searchField, button));

        var poleCountText = "Total number of poles: ";

        var poleCount = new JLabel(poleCountText + style.pm.getPoleCount(style.am.getCurrentNetwork()));
        style.pm.addChangeListener(() -> poleCount.setText(poleCountText+style.pm.getPoleCount(style.am.getCurrentNetwork())));

        panel.add(group(poleCount, addListener(new JButton("Set poles by top degree..."), e ->
                dtm.execute(new TaskIterator(new ExtraTasks.MakeTopDegreePoles(style.am, style.pm))))));

        return panel;
    }

    protected JPanel createFilterPanel() {
        JPanel panel = createTitledPanel("Filtering");

        var b1 = new JRadioButton("All dependencies", true);
        var b2 = new JRadioButton("Unique dependencies", false);

        var group = new ButtonGroup();
        group.add(b1);
        group.add(b2);

        b2.addChangeListener(e -> style.setShowUnique(b2.isSelected()));
        onSessionLoaded.add(e -> b1.setSelected(true));

        panel.add(group(b1, b2));

        return panel;
    }

    protected JPanel createLayoutPanel() {
        JPanel panel = createTitledPanel("Layout");

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

        panel.add(group(addListener(new JButton("Run layout algorithm"), e ->
                layout.runLayout(() -> {
                    // On layout complete
                    style.getRadiusAnnotation().reposition();
                    style.getRingsAnnotation().reposition();
                }))));

        return panel;
    }

    protected JPanel createStylePanel() {
        JPanel panel = createTitledPanel("Style");

        // CONTENTS
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

        var togglePoleColors = new JCheckBox();
        togglePoleColors.setSelected(true);
        togglePoleColors.setHorizontalTextPosition(SwingConstants.LEFT);
        togglePoleColors.addActionListener(l -> style.setShowPoleColors(togglePoleColors.isSelected()));
        panel.add(group(new JLabel("Show poles in different colors"), togglePoleColors));

        //panel.add(group(new JButton("Choose colors...")));

        return panel;
    }

    protected JSlider createCustomSlider(int min, int max, int value, int majorTicks, int minorTicks, int scrollAmount) {
        var slider = new JSlider(min, max, value);
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(majorTicks);
        slider.setMinorTickSpacing(minorTicks);
        // Add ability to change using scroll wheel
        slider.addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            if (notches < 0)
                slider.setValue(slider.getValue() + scrollAmount);
            else if (notches > 0)
                slider.setValue(slider.getValue() - scrollAmount);
        });
        return slider;
    }

    protected JSpinner createCustomSpinner(int min, int max, int value, int step) {
        var spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        // Add ability to change using scroll wheel
        spinner.addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            if (notches < 0 && spinner.getModel().getNextValue() != null)
                spinner.setValue(spinner.getModel().getNextValue());
            else if (notches > 0 && spinner.getModel().getPreviousValue() != null)
                spinner.setValue(spinner.getModel().getPreviousValue());
        });
        return spinner;
    }

    private String bold(String text) {
        return "<html><b>" + text + "</b></html>";
    }

    private AbstractButton addListener(AbstractButton object, ActionListener action) {
        object.addActionListener(action);
        return object;
    }

    private JPanel label(String text, JComponent component) {
        return group(new JLabel(text), component);
    }

    private JPanel label(String text, JSlider component) {
        var label = new JLabel(text);
        component.addChangeListener(e -> updateLabelValue(label, component.getValue()));
        updateLabelValue(label, component.getValue());
        if (component.getPaintLabels())
            return group(50, label, component);
        return group(label, component);
    }

    private void updateLabelValue(JLabel label, float value) {
        var text = label.getText();
        text = text.split(":")[0];
        String valString = value + "";
        if (value == Math.round(value)) valString = Math.round(value) + "";
        label.setText(text + ": " + valString);
    }

    private JPanel group(JComponent... components) {
        return group(ENTRY_HEIGHT, components);
    }

    private JPanel group(int height, JComponent... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, components.length, 10, 10));
        panel.setBorder(new EmptyBorder(5, 5,5,5));
        panel.setMaximumSize(new Dimension(10000, height));
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, height));
        for (JComponent c : components) {
            panel.add(c);
        }
        return panel;
    }

    private JPanel groupBox(int height, JComponent... components) {
        JPanel panel = group(height, components);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        for (int i = 0; i < components.length-1; i++) {
            panel.add(Box.createRigidArea(new Dimension(10, height)), 1+i*2);
        }
        return panel;
    }

    private JPanel groupBox(JComponent... components) {
        return groupBox(ENTRY_HEIGHT, components);
    }

    private void fireChangeListeners(JSlider component) {
        Arrays.stream(component.getChangeListeners()).forEach(l -> l.stateChanged(new ChangeEvent(component)));
    }

    private void fireChangeListeners(JSpinner component) {
        Arrays.stream(component.getChangeListeners()).forEach(l -> l.stateChanged(new ChangeEvent(component)));
    }

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
