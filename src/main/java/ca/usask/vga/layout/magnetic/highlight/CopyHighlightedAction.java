package ca.usask.vga.layout.magnetic.highlight;

import org.cytoscape.application.swing.AbstractCyAction;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Creates a new graph from the highlighted paths on the click of a mouse.
 * Present on the Cytoscape toolbar.
 * @see EdgeHighlighting
 */
public class CopyHighlightedAction extends AbstractCyAction {

    private EdgeHighlighting edgeHighlighting;

    public CopyHighlightedAction(EdgeHighlighting edgeHighlighting) {
        super("SelectionToNetwork");

        this.edgeHighlighting = edgeHighlighting;

        ImageIcon icon = new ImageIcon(getClass().getResource("/icons/copy_highlighted_graph.png"));
        putValue(LARGE_ICON_KEY, icon);

        putValue(SHORT_DESCRIPTION, "Make a new graph with the highlighted edges and nodes");

        setToolbarGravity(15.3f);

        this.useToggleButton = false;
        this.inToolBar = true;
        this.insertSeparatorAfter = true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        edgeHighlighting.selectionToNetwork();
    }

}