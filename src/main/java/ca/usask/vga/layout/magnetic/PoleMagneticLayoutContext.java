package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.force.FieldType;
import ca.usask.vga.layout.magnetic.force.HierarchyForce;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

/**
 * Contains all the input parameters and settings of the layout.
 * Note that it also inherits parameters from {@link SimpleMagneticLayoutContext}
 */
public class PoleMagneticLayoutContext extends SimpleMagneticLayoutContext {

    protected final String POLE_GROUP = "Selected poles";

    @Tunable(
            description = "Field type",
            groups = MAGNET_GROUP,
            context = "both",
            longDescription = "The direction of the magnetic field to use; enum value",
            exampleStringValue = "Linear (horizontal)",
            dependsOn="useMagneticPoles=false",
            gravity=300.05
    )
    @Override
    public ListSingleSelection<FieldType> getFieldType() {
        return super.getFieldType();
    }

    @Tunable(description="Use magnetic poles", gravity=300.09, groups=MAGNET_GROUP, dependsOn="magnetEnabled=true", context="both",
            longDescription="Enable pole magnetic fields; boolean value", exampleStringValue="true")
    public boolean useMagneticPoles = true;

    @Tunable(description="Pin pole positions", gravity=270.2, groups=POLE_GROUP, context="both",
            longDescription="Pin pole positions in one place; boolean value", exampleStringValue="true")
    public boolean pinPoles = false;

    @Tunable(description="Enable pole attraction", gravity=270.31, groups=POLE_GROUP, context="both",
            longDescription="Enable attraction towards the poles; boolean value", exampleStringValue="true")
    public boolean usePoleAttraction = false;

    @Tunable(description="Pole attraction", format="#.##E0", gravity=270.32, groups=POLE_GROUP, dependsOn="usePoleAttraction=true", context="both",
            longDescription="The strength of the attraction towards the poles; float value", exampleStringValue="1e-4")
    public double poleGravity = 1e-4;

    @Tunable(description="Enable central gravity", gravity=250.41, groups=EXTRA_GROUP, context="both",
            longDescription="Enable gravity towards the center of the network; boolean value", exampleStringValue="true")
    public boolean useCentralGravity = false;

    @Tunable(description="Gravity constant", format="#.##E0", gravity=250.42, groups=EXTRA_GROUP, dependsOn="useCentralGravity=true", context="both",
            longDescription="The strength of the gravity towards the center of the network; float value", exampleStringValue="1e-4")
    public double centralGravity = 1e-4;

    @Tunable(description="Circle pin", gravity=270.21, groups=POLE_GROUP, dependsOn="pinPoles=true", context="both",
            longDescription="Pin the poles in a counterclockwise circle; boolean value", exampleStringValue="true")
    public boolean useCirclePin = true;

    @Tunable(description="Circle pin radius", gravity=270.22, groups=POLE_GROUP, dependsOn="pinPoles=true", context="both",
            longDescription="The radius of the pin circle; float value", exampleStringValue="10000")
    public double pinRadius = 10000;

    // Hierarchy
    @Tunable(description="Enable hierarchy force", gravity=280.01, groups=HIERARCHY_GROUP, context="both",
            longDescription="Enable hierarchical force; boolean value", exampleStringValue="true")
    public boolean useHierarchyForce = false;

    protected final String HIERARCHY_GROUP = "Hierarchy";
    public HierarchyForce.Type hierarchyType = HierarchyForce.Type.SINE_FUNCTION;

    @Tunable(description="Choose hierarchy type", gravity=280.1, groups=HIERARCHY_GROUP, dependsOn="useHierarchyForce=true", context="both",
            longDescription="The type of hierarchy force to use; enum value", exampleStringValue="Sine function rings")
    public ListSingleSelection<HierarchyForce.Type> getHierarchyType() {
        ListSingleSelection<HierarchyForce.Type> t = new ListSingleSelection<>(HierarchyForce.Type.BASED_ON_HOP_DISTANCE, HierarchyForce.Type.SINE_FUNCTION);
        t.setSelectedValue(this.hierarchyType);
        return t;
    }
    public void setHierarchyType(ListSingleSelection<HierarchyForce.Type> t) {
        this.hierarchyType = (HierarchyForce.Type) t.getSelectedValue();
    }

    @Tunable(description="Hierarchy force strength", format="#.##E0", gravity=280.2, groups=HIERARCHY_GROUP, dependsOn="useHierarchyForce=true", context="both",
            longDescription="The strength of the hierarchy force; float value", exampleStringValue="1e-4")
    public double hierarchyForce = 1e-4;

    @Tunable(description="Ring radius", gravity=280.3, groups=HIERARCHY_GROUP, dependsOn="useHierarchyForce=true", context="both",
            longDescription="The radius difference between each ring; float value", exampleStringValue="250")
    public double ringRadius = 250;

    @Tunable(description="AUTO LAYOUT", gravity=900.01, context="both",
            longDescription="Iterate over parameter combinations to find better initial parameters", exampleStringValue="false")
    public boolean useAutoLayout = false;


}
