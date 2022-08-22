package ca.usask.vga.layout.magnetic.highlight;


import ca.usask.vga.layout.magnetic.poles.PoleManager;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;

/**
 * Utility class for storing Cytoscape services important for
 * network manipulation and copying.
 */
public class NetworkCyAccess {
    public final CyNetworkFactory nf;
    public final CyNetworkManager nm;
    public final CyNetworkViewFactory vf;
    public final CyNetworkViewManager vm;
    public final CyNetworkNaming cnn;
    public final VisualMappingManager vmm;
    public final CyRootNetworkManager rnm;
    public final CyApplicationManager am;
    public final PoleManager pm;

    public NetworkCyAccess(CyNetworkFactory nf, CyNetworkManager nm, CyNetworkViewFactory vf, CyNetworkViewManager vm, CyNetworkNaming cnn, VisualMappingManager vmm, CyRootNetworkManager rnm, CyApplicationManager am, PoleManager pm) {
        this.nf = nf;
        this.nm = nm;
        this.vf = vf;
        this.vm = vm;
        this.cnn = cnn;
        this.vmm = vmm;
        this.rnm = rnm;
        this.am = am;
        this.pm = pm;
    }
}
