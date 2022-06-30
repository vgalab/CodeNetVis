package ca.usask.vga.layout.magnetic.highlight;

import org.cytoscape.application.swing.AbstractCyAction;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ToggleHighlightAction extends AbstractCyAction {

    private EdgeHighlighting edgeHighlighting;

    public ToggleHighlightAction(EdgeHighlighting edgeHighlighting) {
        super("DirectedEdgeColor");

        this.edgeHighlighting = edgeHighlighting;

        ImageIcon icon = new ImageIcon(getClass().getResource("/edge_highlighting_off_icon.png"));
        putValue(LARGE_ICON_KEY, icon);

        putValue(SHORT_DESCRIPTION, "Toggle directed edge highlighting");

        setToolbarGravity(15.1f);

        this.useToggleButton = true;
        this.inToolBar = true;
        this.insertToolbarSeparatorBefore = true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        edgeHighlighting.toggleFeature();

        String icon = edgeHighlighting.getEnabled() ? "/edge_highlighting_on_icon.png" : "/edge_highlighting_off_icon.png";
        putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(icon)));
    }

}
