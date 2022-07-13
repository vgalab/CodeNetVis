package ca.usask.vga.layout.magnetic.highlight;

import org.cytoscape.application.swing.AbstractCyAction;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ChangeHopDistanceAction extends AbstractCyAction {

    private EdgeHighlighting edgeHighlighting;

    public int counter = 1;

    public ChangeHopDistanceAction(EdgeHighlighting edgeHighlighting) {
        super("HopDistanceAction");

        this.edgeHighlighting = edgeHighlighting;

        ImageIcon icon = new ImageIcon(getClass().getResource("/icons/hop_distance1.png"));
        putValue(LARGE_ICON_KEY, icon);

        putValue(SHORT_DESCRIPTION, "Change the hop distance for the edge highlighting");

        setToolbarGravity(15.2f);

        this.useToggleButton = false;
        this.inToolBar = true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        counter = (counter % 4) + 1;
        edgeHighlighting.setDesiredHopDistance(counter);
        ImageIcon icon = new ImageIcon(getClass().getResource("/hop_distance" + counter + ".png"));
        putValue(LARGE_ICON_KEY, icon);

    }

}
