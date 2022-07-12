package ca.usask.vga.layout.magnetic.io;

import ca.usask.vga.layout.magnetic.util.Vector;
import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.model.*;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class PajekReader extends AbstractInputStreamTaskFactory {

    public static final String NODE_ID_COLUMN = "name", NODE_LABEL_COLUMN = "Label";
    public static final String EDGE_ID_COLUMN = "name", EDGE_WEIGHT_COLUMN = "Weight";

    public boolean mirrorVertically = true;

    public static class CyAccess {
        public final CyNetworkFactory nf;
        public final CyNetworkViewFactory vf;

        public CyAccess(CyNetworkFactory nf, CyNetworkViewFactory vf) {
            this.nf = nf;
            this.vf = vf;
        }
    }

    private final CyAccess cy;

    protected PajekReader(CyFileFilter fileFilter, CyAccess dependencies) {
        super(fileFilter);
        this.cy = dependencies;
    }

    public static PajekReader create(CyAccess dependencies, StreamUtil streamUtil) {

        //1. define a file filter (BasicCyFileFilter), to support the reader to read the file with extension '.tc'
        HashSet<String> extensions = new HashSet<>();
        extensions.add("net");
        extensions.add("NET");
        HashSet<String> contentTypes = new HashSet<>();
        contentTypes.add("txt");
        String description = "Pajek (.net) file filter";
        DataCategory category = DataCategory.NETWORK;
        BasicCyFileFilter filter = new BasicCyFileFilter(extensions,contentTypes, description, category, streamUtil);

        //2. Create an instance of the ReaderFactory
        // Note that extends TCReaderFactory  must implement the interface InputStreamTaskFactory or extends the class  AbstractInputStreamTaskFactory.
        // And the defined task must implement CyNetworkReader
        return new PajekReader(filter, dependencies);

        //3. register the ReaderFactory as an InputStreamTaskFactory.
    }

    public Properties getDefaultProperties() {
        Properties props = new Properties();
        props.setProperty("readerDescription","TC file reader");
        props.setProperty("readerId","tcNetworkReader");
        return props;
    }

    public Class<InputStreamTaskFactory> getServiceClass() {
        return InputStreamTaskFactory.class;
    }

    @Override
    public TaskIterator createTaskIterator(InputStream inputStream, String inputName) {
        return new TaskIterator(new ReaderTask(inputStream, inputName, cy));
    }

    protected class ReaderTask implements CyNetworkReader {

        private final InputStream inputStream;
        private final String inputName;
        private final CyAccess cy;

        private boolean cancelled;

        private final List<CyNetwork> newNetworks;

        private final Map<CyNode, Vector> nodeCoordinates;

        public ReaderTask(InputStream inputStream, String inputName, CyAccess dependencies) {
            this.inputStream = inputStream;
            this.inputName = inputName;
            cy = dependencies;
            newNetworks = new ArrayList<>();
            nodeCoordinates = new HashMap<>();
        }

        @Override
        public CyNetwork[] getNetworks() {
            return newNetworks.toArray(new CyNetwork[0]);
        }

        @Override
        public CyNetworkView buildCyNetworkView(CyNetwork network) {
            CyNetworkView view = cy.vf.createNetworkView(network);
            for (CyNode node : nodeCoordinates.keySet()) {
                Vector pos = nodeCoordinates.get(node);
                view.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, (double) pos.x);
                view.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, (double) pos.y);
            }
            return view;
        }

        protected CyNode newNode(CyNetwork network, String id, String label) {
            CyNode node = network.addNode();
            CyTable table = network.getDefaultNodeTable();
            if (table.getColumn(NODE_ID_COLUMN) == null)
                table.createColumn(NODE_ID_COLUMN, String.class, false);
            if (table.getColumn(NODE_LABEL_COLUMN) == null)
                table.createColumn(NODE_LABEL_COLUMN, String.class, false);
            table.getRow(node.getSUID()).set(NODE_ID_COLUMN, id);
            if (label != null)
                table.getRow(node.getSUID()).set(NODE_LABEL_COLUMN, label);
            return node;
        }

        protected CyEdge newEdge(CyNetwork network, String from, String to, Long weight) {
            CyTable nodeTable = network.getDefaultNodeTable();
            Collection<Long> fromMatches = nodeTable.getMatchingKeys(NODE_ID_COLUMN, from, Long.class);
            long fromS;
            if (fromMatches.size() == 0)
                fromS = newNode(network, from, null).getSUID();
            else
                fromS = fromMatches.iterator().next();

            Collection<Long> toMatches = nodeTable.getMatchingKeys(NODE_ID_COLUMN, to, Long.class);
            long toS;
            if (toMatches.size() == 0)
                toS = newNode(network, to, null).getSUID();
            else
                toS = toMatches.iterator().next();

            CyEdge edge = network.addEdge(network.getNode(fromS), network.getNode(toS), true);

            CyTable edgeTable = network.getDefaultEdgeTable();
            if (edgeTable.getColumn(EDGE_ID_COLUMN) == null)
                edgeTable.createColumn(EDGE_ID_COLUMN, String.class, false);
            edgeTable.getRow(edge.getSUID()).set(EDGE_ID_COLUMN, from + " > " + to);

            if (weight != null) {
                if (edgeTable.getColumn(EDGE_WEIGHT_COLUMN) == null)
                    edgeTable.createColumn(EDGE_WEIGHT_COLUMN, Long.class, false);
                edgeTable.getRow(edge.getSUID()).set(EDGE_WEIGHT_COLUMN, weight);
            }
            return edge;
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {

            taskMonitor.setTitle("Importing a Pajek (.net) file: " + inputName);
            taskMonitor.setProgress(0.1);

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            CyNetwork network = cy.nf.createNetwork();

            boolean readingVertices = false;
            while(reader.ready()) {
                if (cancelled) return;

                String line = reader.readLine();
                if (line.startsWith("*")) {
                    String[] splitLine = line.split("[* ]+");
                    if (splitLine.length == 0) continue;
                    String subject = splitLine[1].toLowerCase();
                    //long count = Long.parseLong(splitLine[2]);
                    if (subject.equals("vertices")) {
                        taskMonitor.setProgress(0.3);
                        readingVertices = true;
                    } else if (subject.equals("arcs") || subject.equals("edges")) {
                        readingVertices = false;
                        taskMonitor.setProgress(0.6);
                    }
                    continue;
                }
                if (readingVertices) {
                    // VERTEX MODE
                    String[] firstSplit = line.trim().split(" +", 2);
                    String identifier = firstSplit[0];
                    String attributes = firstSplit[1];

                    String label, misc = "";
                    if (attributes.matches(" *\"(.*?)\".*")) {
                        // Double quotes string present
                        String[] textSplit = attributes.trim().split("\"", 3);
                        if (textSplit.length < 3) continue;
                        label = textSplit[1];
                        misc = textSplit[2];
                    } else {
                        // Spaces separated
                        String[] textSplit = attributes.trim().split(" +", 2);
                        label = textSplit[0];
                        if (textSplit.length > 1)
                            misc = textSplit[1];
                    }

                    CyNode node = newNode(network, identifier, label);

                    String[] coordsSplit = misc.trim().split(" +", 3);
                    float X = 0, Y = 0;
                    if (coordsSplit.length >= 2) {
                        if (!coordsSplit[0].equals("") && !coordsSplit[1].equals("")) {
                            X = Float.parseFloat(coordsSplit[0]);
                            Y = Float.parseFloat(coordsSplit[1]) * (mirrorVertically ? -1 : 1);
                            nodeCoordinates.put(node, new Vector(X, Y));
                        }
                    }

                } else {
                    // EDGE MODE
                    String[] edgeAttrs = line.trim().split(" +");
                    if (edgeAttrs.length < 2) continue;
                    String from = edgeAttrs[0];
                    String to = edgeAttrs[1];
                    Long weight = null;
                    if (edgeAttrs.length >= 3 && !edgeAttrs[2].equals(""))
                        weight = Long.parseLong(edgeAttrs[2]);

                    newEdge(network, from, to, weight);

                }
            }

            if (mirrorVertically && !nodeCoordinates.isEmpty())
                taskMonitor.showMessage(TaskMonitor.Level.INFO, "Note: The graph has been mirrored vertically for compatibility");

            taskMonitor.setProgress(0.9);
            newNetworks.add(network);
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

}
