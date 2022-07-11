package ca.usask.vga.layout.magnetic.util;

import org.cytoscape.model.CyEdge;
import prefuse.util.force.ForceItem;
import prefuse.util.force.Spring;

public interface PoleClassifier {

    Iterable<ForceItem> getPoleList();
    Iterable<ForceItem> getPoleListSorted(CyEdge.Type edgeType);

    int getPoleListSize();
    boolean isPole(ForceItem item);

    ForceItem closestPole(ForceItem item);
    int closestPoleDistance(ForceItem item);

    boolean isDisconnected(ForceItem item);
    boolean isClosestToMultiple(ForceItem item);
    boolean isClosestToOne(ForceItem item);

    ForceItem poleOf(Spring spring);
    boolean isDisconnected(Spring spring);
    boolean isClosestToMultiple(Spring spring);
    boolean isClosestToOne(Spring spring);

    boolean isPoleOutwards(ForceItem pole);

}
