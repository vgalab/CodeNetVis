package ca.usask.vga.layout.magnetic;

import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class SoftwarePanel extends JPanel implements CytoPanelComponent2 {

    public static final String title = "Software Layout", identifier = "software-panel";
    private final Icon icon = new ImageIcon(getClass().getResource("/icons/add_pole_N_icon.png"));

    private final SoftwareLayout layout;
    private final SoftwareStyle style;

    protected SoftwarePanel(SoftwareLayout layout, SoftwareStyle style) {
        super();
        this.layout = layout;
        this.style = style;

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


        // Temporary POLE panel
        panel = createTitledPanel("Pole selection");
        panel.add(group(new JLabel("Search:"), new JTextField("print")));
        panel.add(group(new JLabel("Found:"), new JLabel("java.io.PrintStream")));
        //panel.add(new JList<>(new String[]{"PrintStream", "PrintMode", "PrintFiles"}));
        panel.add(group(new JButton("Add poles"), new JButton("Remove poles")));
        panel.add(group(new JLabel("Poles:"), new JLabel("String, Object...")));
        innerPanel.add(panel);

        // Temporary FILTERING panel
        panel = createTitledPanel("Filtering");
        panel.add(group(new JRadioButton("No dependencies")));  // NOTE: Need button group
        panel.add(group(new JRadioButton("Unique dependencies")));
        innerPanel.add(panel);


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

    protected JPanel createTitledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(group(new JLabel(bold(title))));
        //panel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        panel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0,0,10,0),
                BorderFactory.createLineBorder(Color.lightGray)));
        return panel;
    }

    protected JPanel createLayoutPanel() {
        JPanel panel = createTitledPanel("Layout");

        var radiusEditor = new JSlider();

        int MAX = 100;

        radiusEditor.setMinimum(0);
        radiusEditor.setMaximum(MAX);
        radiusEditor.setValue(MAX/4);
        radiusEditor.setPaintTicks(true);
        radiusEditor.setMajorTickSpacing(MAX/4);
        radiusEditor.setMinorTickSpacing(MAX/20);

        radiusEditor.addChangeListener(e -> layout.setPinRadius(radiusEditor.getValue()*100));
        layout.setPinRadius(radiusEditor.getValue());

        panel.add(label("Circle radius: " + MAX/4, radiusEditor));

        var ringsEditor = new JSpinner();

        ringsEditor.setModel(new SpinnerNumberModel(4, 0, 100, 1));
        ringsEditor.addChangeListener(e -> layout.setMaxRings((Integer) ringsEditor.getValue()));
        layout.setMaxRings((Integer) ringsEditor.getValue());
        panel.add(label("Max rings:", ringsEditor));

        panel.add(group(addListener(new JButton("Run layout algorithm"), e -> layout.runLayout())));

        return panel;
    }

    protected JPanel createStylePanel() {
        JPanel panel = createTitledPanel("Style");

        // TODO: Other mappings

        // CONTENTS
        panel.add(group(new JLabel("Node size based on"), new JComboBox<String>(new String[]{"Fixed"/*, "Indegree", "Outdegree"*/})));

        var sizeEditor = new JSlider(0, 100, 50);
        sizeEditor.setPaintTicks(true);
        sizeEditor.setMajorTickSpacing(50);
        sizeEditor.setMinorTickSpacing(10);

        sizeEditor.addChangeListener(e -> style.setNodeSize(sizeEditor.getValue()));

        panel.add(label("Node size: 50", sizeEditor));

        var transparencyEditor = new JSlider(0, 255, 100);
        transparencyEditor.setPaintTicks(true);
        transparencyEditor.setMajorTickSpacing(50);
        transparencyEditor.setMinorTickSpacing(10);

        transparencyEditor.addChangeListener(e -> style.setEdgeTransparency(transparencyEditor.getValue()));

        panel.add(label("Edge visibility: 100", transparencyEditor));

        panel.add(group(new JButton("Choose colors...")));

        return panel;
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
        return group(35, components);
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
