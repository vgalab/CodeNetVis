package ca.usask.vga.layout.magnetic;

import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

public class SoftwarePanel extends JPanel implements CytoPanelComponent2 {

    public static final String title = "Software Layout", identifier = "software-panel";
    private final Icon icon = new ImageIcon(getClass().getResource("/icons/add_pole_N_icon.png"));

    private final SoftwareLayout layout;

    protected SoftwarePanel(SoftwareLayout layout) {
        super();
        this.layout = layout;

        var mainLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(mainLayout);

        this.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Temporary DATA panel
        JPanel panel = createTitledPanel("Data");

        panel.add(group(new JLabel("GitHub Link:"), new JTextField("www.example.com")));

        panel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        add(panel);


        // Temporary POLE panel
        panel = createTitledPanel("Pole selection");

        panel.add(group(new JLabel("Search:"), new JTextField("print")));
        panel.add(group(new JLabel("Found:"), new JLabel("java.io.PrintStream")));
        //panel.add(new JList<>(new String[]{"PrintStream", "PrintMode", "PrintFiles"}));
        panel.add(group(new JButton("Add poles"), new JButton("Remove poles")));
        panel.add(group(new JLabel("Poles:"), new JLabel("String, Object...")));

        panel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        add(panel);


        // Temporary FILTERING panel
        panel = createTitledPanel("Filtering");

        panel.add(group(new JRadioButton("No dependencies")));  // NOTE: Need button group
        panel.add(group(new JRadioButton("Unique dependencies")));

        panel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        add(panel);


        // LAYOUT panel
        add(createLayoutPanel());


        // Temporary STYLE panel
        panel = createTitledPanel("Style");

        // CONTENTS
        panel.add(group(new JLabel("Node size based on"), new JComboBox<String>(new String[]{"Fixed", "Indegree", "Outdegree"})));
        panel.add(group(new JLabel("Node size:"), new JSpinner()));
        panel.add(group(new JButton("Choose colors...")));

        panel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        add(panel);
    }

    protected JPanel createTitledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(group(new JLabel(bold(title))));
        return panel;
    }

    protected JPanel createLayoutPanel() {
        JPanel panel = createTitledPanel("Layout");

        var radiusLabel = new JLabel("Circle radius");
        var radiusEditor = new JSlider();

        int MAX = 10000;

        radiusEditor.setMinimum(0);
        radiusEditor.setMaximum(MAX);
        radiusEditor.setValue(MAX/4);
        radiusEditor.setPaintTicks(true);
        radiusEditor.setPaintLabels(true);
        radiusEditor.setMajorTickSpacing(MAX/4);
        radiusEditor.setMinorTickSpacing(MAX/20);

        /*var labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put(0, new JLabel("0") );
        radiusEditor.setLabelTable( labelTable );*/

        radiusEditor.addChangeListener(e -> layout.setPinRadius(radiusEditor.getValue()));
        layout.setPinRadius(radiusEditor.getValue());

        panel.add(group(radiusLabel, radiusEditor));

        var ringsLabel = new JLabel("Max rings");
        var ringsEditor = new JSpinner();

        ringsEditor.setModel(new SpinnerNumberModel(4, 0, 20, 1));
        ringsEditor.addChangeListener(e -> layout.setMaxRings((Integer) ringsEditor.getValue()));
        layout.setMaxRings((Integer) ringsEditor.getValue());
        panel.add(group(ringsLabel, ringsEditor));

        panel.add(group(addListener(new JButton("Run layout algorithm"), e -> layout.runLayout())));

        panel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        return panel;
    }

    private String bold(String text) {
        return "<html><b>" + text + "</b></html>";
    }

    private AbstractButton addListener(AbstractButton object, ActionListener action) {
        object.addActionListener(action);
        return object;
    }

    private JPanel group(JComponent... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, components.length, 10, 10));
        //panel.setBackground(Color.yellow);
        panel.setBorder(new EmptyBorder(5, 5,5,5));
        panel.setMaximumSize(new Dimension(10000, 35));
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
