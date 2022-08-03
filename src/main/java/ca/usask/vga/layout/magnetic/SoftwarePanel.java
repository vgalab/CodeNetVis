package ca.usask.vga.layout.magnetic;

import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SoftwarePanel extends JPanel implements CytoPanelComponent2 {

    public static final String title = "Software Layout", identifier = "software-panel";
    private final Icon icon = new ImageIcon(getClass().getResource("/icons/add_pole_N_icon.png"));

    protected SoftwarePanel() {
        super();

        var mainLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(mainLayout);

        this.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(group(new JLabel(bold("Data"))));

        // CONTENTS
        panel.add(group(new JLabel("GitHub Link:"), new JTextField("www.example.com")));

        panel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        add(panel);

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(group(new JLabel(bold("Pole selection"))));

        // CONTENTS
        panel.add(group(new JLabel("Search:"), new JTextField("print")));
        panel.add(group(new JLabel("Found:"), new JLabel("java.io.PrintStream")));
        //panel.add(new JList<>(new String[]{"PrintStream", "PrintMode", "PrintFiles"}));
        panel.add(group(new JButton("Add poles"), new JButton("Remove poles")));
        panel.add(group(new JLabel("Poles:"), new JLabel("String, Object...")));

        panel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        add(panel);

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(group(new JLabel(bold("Filtering"))));

        // CONTENTS
        panel.add(group(new JRadioButton("No dependencies")));  // NOTE: Need button group
        panel.add(group(new JRadioButton("Unique dependencies")));

        panel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        add(panel);


        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(group(new JLabel(bold("Layout"))));

        // CONTENTS
        panel.add(group(new JLabel("Circle radius"), new JSlider()));
        panel.add(group(new JLabel("Max rings"), new JSpinner()));
        panel.add(group(new JButton("Run layout algorithm")));

        panel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        add(panel);

        panel = new JPanel();
        //panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(group(new JLabel(bold("Style"))));

        // CONTENTS
        panel.add(group(new JLabel("Node size based on"), new JComboBox<String>(new String[]{"Fixed", "Indegree", "Outdegree"})));
        panel.add(group(new JLabel("Node size:"), new JSpinner()));
        panel.add(group(new JButton("Choose colors...")));

        panel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        add(panel);
    }

    private String bold(String text) {
        return "<html><b>" + text + "</b></html>";
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
