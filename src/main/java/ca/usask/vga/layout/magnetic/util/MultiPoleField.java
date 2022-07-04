package ca.usask.vga.layout.magnetic.util;

import prefuse.util.force.ForceItem;
import prefuse.util.force.Spring;

public class MultiPoleField {

    private final PoleClassifier classifier;

    public MultiPoleField(PoleClassifier classifier) {
        this.classifier = classifier;
    }

}
