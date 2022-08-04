package ca.usask.vga.layout.magnetic;

import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

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

        var radiusEditor = createCustomSlider(0, 100, 25, 25, 5, 5);

        radiusEditor.addChangeListener(e -> layout.setPinRadius(radiusEditor.getValue()*100));
        radiusEditor.addChangeListener(e -> style.getRadiusAnnotation().setRadius(radiusEditor.getValue()*100));
        radiusEditor.addMouseListener(annotationOnMouse(style.getRadiusAnnotation()));

        layout.setPinRadius(radiusEditor.getValue());

        panel.add(label("Circle radius: " + 25, radiusEditor));

        var ringsEditor = createCustomSpinner(0, 100, 4, 1);

        ringsEditor.addChangeListener(e -> layout.setMaxRings((Integer) ringsEditor.getValue()));
        ringsEditor.addChangeListener(e -> style.getRingsAnnotation().setMaxRings((Integer) ringsEditor.getValue()));
        layout.setMaxRings((Integer) ringsEditor.getValue());

        ((JSpinner.NumberEditor)ringsEditor.getEditor()).getTextField()
                .addMouseListener(annotationOnMouse(style.getRingsAnnotation()));

        panel.add(label("Max rings:", ringsEditor));

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

        // TODO: Other mappings

        // CONTENTS
        panel.add(group(new JLabel("Node size based on"), new JComboBox<String>(new String[]{"Fixed"/*, "Indegree", "Outdegree"*/})));

        var sizeEditor = createCustomSlider(0, 100, 50, 25, 5, 1);

        sizeEditor.addChangeListener(e -> style.setNodeSize(sizeEditor.getValue()));

        panel.add(label("Node size: 50", sizeEditor));

        var transparencyEditor = createCustomSlider(0, 255, 105, 60, 15, 15);

        transparencyEditor.addChangeListener(e -> style.setEdgeTransparency(transparencyEditor.getValue()));

        panel.add(label("Edge visibility: 100", transparencyEditor));

        panel.add(group(new JButton("Choose colors...")));

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
