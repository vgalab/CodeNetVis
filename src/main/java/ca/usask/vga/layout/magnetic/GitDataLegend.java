package ca.usask.vga.layout.magnetic;

import javax.swing.*;

import java.awt.*;

import static ca.usask.vga.layout.magnetic.SoftwareStyle.*;
import static ca.usask.vga.layout.magnetic.SoftwareStyle.GitDataProperty.MOST_RECENT_COMMIT_DATE;

public class GitDataLegend extends JPanel {

    private static final Color DEFAULT_GREEN = new Color(141,211,199);
    private static final Color DEFAULT_GRAY = Color.lightGray;

    GitDataProperty property;
    GitDataVisualization visualization;

    LegendCircle[] circles;
    JLabel[] labels;
    JLabel leftMostLabel;

    final int circleCount = 5;

    public GitDataLegend() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        circles = new LegendCircle[circleCount];
        labels = new JLabel[circleCount];

        leftMostLabel = new JLabel();
        add(leftMostLabel);

        // Reverse order for later use
        for (int i = circleCount-1; i >= 0; i--) {
            circles[i] = new LegendCircle();
            labels[i] = new JLabel();
            add(circles[i]);
            add(labels[i]);
            if (i > 0) {
                add(Box.createHorizontalGlue());
            }
        }

    }

    public void setSelectedOptions(GitDataProperty property, GitDataVisualization visualization) {
        this.property = property;
        this.visualization = visualization;
        refreshLegend();
        this.repaint();
    }

    public void refreshLegend() {
        // Reset
        for (int i = 0; i < circleCount; i++)
            circles[i].color = (property == GitDataProperty.NONE || visualization == GitDataVisualization.NONE)
                    ? DEFAULT_GRAY : DEFAULT_GREEN ;
        for (int i = 0; i < circleCount; i++)
            circles[i].desiredDiameter = 15;
        for (int i = 0; i < circleCount; i++)
            circles[i].borderThickness = 0;
        for (int i = 0; i < circleCount; i++)
            circles[i].encircled = false;
        for (int i = 0; i < circleCount; i++)
            circles[i].borderColor = Color.black;

        for (int i = 0; i < circleCount; i++)
            labels[i].setText("");
        leftMostLabel.setText("");

        switch (property) {
            case LAST_COMMIT_AUTHOR: {
                for (int i = 0; i < circleCount; i++)
                    labels[i].setText("Author " + (5-i));
                break;
            }
            case MOST_RECENT_COMMIT_DATE: {
                labels[0].setText("Oldest");
                leftMostLabel.setText("Most recent");
                break;
            }
            case TOTAL_NUMBER_OF_COMMITS: {
                labels[0].setText("Least");
                leftMostLabel.setText("Most commits");
                break;
            }
            case NONE: {
                leftMostLabel.setText("Legend");
                break;
            }
        }

        switch (visualization) {
            case NODE_SIZE: {
                for (int i = 0; i < circleCount; i++)
                    circles[i].desiredDiameter = i*4 + 4;
                break;
            }
            case CIRCLED_NODES: {
                for (int i = 0; i < circleCount; i++)
                    circles[i].encircled = true;
                for (int i = 0; i < circleCount; i++)
                    circles[i].borderColor = new Color(0, 0, 0, i*255/4);
                break;
            }
            case BORDER_WIDTH: {
                for (int i = 0; i < circleCount; i++)
                    circles[i].borderThickness = i;
                break;
            }
            case NODE_COLOR: {
                switch (property) {
                    case TOTAL_NUMBER_OF_COMMITS: {
                        for (int i = 0; i < circleCount; i++)
                            circles[i].color = new Color(0, i*255/4, 0);
                        break;
                    }
                    case MOST_RECENT_COMMIT_DATE: {
                        circles[0].color = COLOR_BREWER_DIVERGENT[0];
                        circles[1].color = COLOR_BREWER_DIVERGENT[2];
                        circles[2].color = COLOR_BREWER_DIVERGENT[4];
                        circles[3].color = COLOR_BREWER_DIVERGENT[6];
                        circles[4].color = COLOR_BREWER_DIVERGENT[8];
                        break;
                    }
                    case LAST_COMMIT_AUTHOR: {
                        for (int i = 0; i < circleCount; i++)
                            circles[i].color = COLOR_BREWER_SET3[i];
                    }
                }
                break;
            }
        }

    }

    static class LegendCircle extends JComponent {

        private final int size = 24;

        protected Color color = DEFAULT_GRAY;
        protected Color borderColor = Color.black;

        protected int desiredDiameter = 15;

        protected int borderThickness = 0;

        protected boolean encircled = false;

        public LegendCircle() {
            super();
            setPreferredSize(new Dimension(size+10, size));
            setMinimumSize(new Dimension(size+10, size));
            setMaximumSize(new Dimension(size+10, size));
        }

        public void setParameters(Color color, int borderTransparency, int borderThickness, boolean encircled, int desiredDiameter) {
            this.color = color;
            this.borderColor = new Color(0, 0, 0, borderTransparency);
            this.borderThickness = borderThickness;
            this.encircled = encircled;
            this.desiredDiameter = desiredDiameter;
        }

        private int getCirclePos(int diameter) {
            return size / 2 - diameter / 2;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            RenderingHints rh = new RenderingHints(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHints(rh);

            int diameter;

            if (!encircled) {
                diameter = desiredDiameter;
                g2.setColor(color);
                g2.fillOval(getCirclePos(diameter)+5, getCirclePos(diameter), diameter, diameter);

                if (borderThickness > 0) {
                    g2.setColor(borderColor);
                    g2.setStroke(new BasicStroke(borderThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawOval(getCirclePos(diameter)+5, getCirclePos(diameter), diameter, diameter);
                }
            } else {
                diameter = 12;
                g2.setColor(color);
                g2.fillOval(getCirclePos(diameter)+5, getCirclePos(diameter), diameter, diameter);

                diameter = 20;
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]{1, 2}, 0));
                g2.drawOval(getCirclePos(diameter)+5, getCirclePos(diameter), diameter, diameter);
            }
        }
    }


}
