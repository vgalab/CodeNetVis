package ca.usask.vga.layout.magnetic.util;

import prefuse.util.force.ForceItem;
import prefuse.util.force.Spring;

public interface PoleClassifier {

    Iterable<ForceItem> getPoleList();
    boolean isPole(ForceItem item);

    ForceItem closestPole(ForceItem item);
    int distanceToPole(ForceItem item);

    boolean isDisconnected(ForceItem item);
    boolean isClosestToMultiple(ForceItem item);
    boolean isClosestToOne(ForceItem item);

    ForceItem poleOf(Spring spring);
    boolean isDisconnected(Spring spring);
    boolean isClosestToMultiple(Spring spring);
    boolean isClosestToOne(Spring spring);

    boolean isPoleOutwards(ForceItem pole);

}
