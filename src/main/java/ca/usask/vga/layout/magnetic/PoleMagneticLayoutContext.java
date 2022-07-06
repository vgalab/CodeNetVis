package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.util.FieldType;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.ListSingleSelection;

public class PoleMagneticLayoutContext extends SimpleMagneticLayoutContext {

    // TODO: Write descriptions, change to floats (?), add validation states


    @Tunable(
            description = "Field type",
            groups = "Magnet",
            context = "both",
            longDescription = "TODO",
            exampleStringValue = "Linear (horizontal)",
            dependsOn="usePoles=false",
            gravity=300.05
    )
    @Override
    public ListSingleSelection<FieldType> getFieldType() {
        return super.getFieldType();
    }

    @Tunable(description="Use magnetic poles", gravity=300.09, groups="Magnet", dependsOn="magnetEnabled=true", context="both", longDescription="TODO", exampleStringValue="true")
    public boolean usePoles = true;

    @Tunable(description="Pin pole positions", gravity=800.2, context="both", longDescription="TODO", exampleStringValue="true")
    public boolean pinPoles = true;

}
