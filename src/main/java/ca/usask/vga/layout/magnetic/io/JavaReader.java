package ca.usask.vga.layout.magnetic.io;

import gr.gousiosg.javacg.stat.ClassVisitor;
import org.apache.bcel.classfile.ClassParser;
import org.cytoscape.equations.EquationCompiler;
import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.model.*;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

public class JavaReader extends AbstractInputStreamTaskFactory {

    public static final String NODE_NAME = "name", NODE_CLASS = "Class", NODE_PACKAGE = "Package",
            NODE_INNER_CLASS = "Simple class";

    public static final String EDGE_NAME = "name", EDGE_INTERACTION = "interaction";

    public static final String CLASS_FORMULA = "=LAST(SPLIT(${"+NODE_NAME+"},\".\"))";
    public static final String PACKAGE_FORMULA = "=SUBSTITUTE($name, CONCATENATE(\".\",${"+NODE_CLASS+"}), \"\")";
    public static final String INNER_CLASS_FORMULA = "=LAST(SPLIT(${"+NODE_CLASS+"},\"$\"))";


    public static class CyAccess {
        public final CyNetworkFactory nf;
        public final CyNetworkViewFactory vf;
        public final EquationCompiler eq;

        public CyAccess(CyNetworkFactory nf, CyNetworkViewFactory vf, EquationCompiler eq) {
            this.nf = nf;
            this.vf = vf;
            this.eq = eq;
        }
    }

    private final JavaReader.CyAccess cy;

    protected JavaReader(CyFileFilter fileFilter, JavaReader.CyAccess dependencies) {
        super(fileFilter);
        this.cy = dependencies;
    }

    public static JavaReader create(JavaReader.CyAccess dependencies, StreamUtil streamUtil) {

        //1. define a file filter (BasicCyFileFilter), to support the reader to read the file with extension '.tc'
        HashSet<String> extensions = new HashSet<>();
        extensions.add("jar");
        extensions.add("JAR");
        HashSet<String> contentTypes = new HashSet<>();
        contentTypes.add("txt");
        String description = "JAR (.jar) file filter";
        DataCategory category = DataCategory.NETWORK;
        BasicCyFileFilter filter = new BasicCyFileFilter(extensions, contentTypes, description, category, streamUtil);

        //2. Create an instance of the ReaderFactory
        // Note that extends TCReaderFactory  must implement the interface InputStreamTaskFactory or extends the class  AbstractInputStreamTaskFactory.
        // And the defined task must implement CyNetworkReader
        return new JavaReader(filter, dependencies);

        //3. register the ReaderFactory as an InputStreamTaskFactory.
    }

    public Properties getDefaultProperties() {
        Properties props = new Properties();
        props.setProperty("readerDescription","JAR file reader");
        props.setProperty("readerId","jarNetworkReader");
        return props;
    }

    public Class<InputStreamTaskFactory> getServiceClass() {
        return InputStreamTaskFactory.class;
    }

    @Override
    public TaskIterator createTaskIterator(InputStream inputStream, String inputName) {
        return new TaskIterator(new JavaReader.ReaderTask(inputStream, inputName, cy));
    }

    public static class ReaderTask implements CyNetworkReader {

        private InputStream inputStream = null;
        private String inputName = null;
        private String srcFolder = null;

        private final JavaReader.CyAccess cy;
        private Set<String> nodes = null;
        private Set<String> edges = null;
        private Consumer<ReaderTask> afterComplete = c -> {};

        private boolean cancelled;

        private final List<CyNetwork> newNetworks;

        public String[] packagesToIgnore = {"java.", "javax.", "com.sun.", "jdk.", "scala."};

        @ProvidesTitle
        public String getTitle() {
            return "JAR file import properties";
        }

        // @Tunable(description="Root package of interest:")
        public String userPackage = "";

        // @Tunable(description="Ignore java.*, javax.*, jdk.*, com.sun.*")
        public boolean ignoreJavaLibraries = true;

        // @Tunable(description="Hide inner classes:")
        public boolean hideInnerClasses = true;

        // @Tunable(description="Hide anonymous classes:")
        public boolean hideAnonymousClasses = true;

        public ReaderTask(InputStream inputStream, String inputName, JavaReader.CyAccess dependencies) {
            this.inputStream = inputStream;
            this.inputName = inputName;
            cy = dependencies;
            newNetworks = new ArrayList<>();
        }

        public ReaderTask(String filename, JavaReader.CyAccess dependencies, Consumer<ReaderTask> afterComplete)  {
            this.inputName = filename;
            this.afterComplete = afterComplete;
            if (filename.endsWith(".jar")) {
                try {
                    this.inputStream = new FileInputStream(filename);
                } catch (FileNotFoundException e) {throw new RuntimeException(e);}
            } else {
                srcFolder = filename;
            }
            cy = dependencies;
            newNetworks = new ArrayList<>();
        }

        public ReaderTask(Set<String> nodes, Set<String> edges, JavaReader.CyAccess dependencies, Consumer<ReaderTask> afterComplete) {
            this.nodes = nodes;
            this.edges = edges;
            this.afterComplete = afterComplete;
            this.inputStream = null;
            this.inputName = "Other";
            cy = dependencies;
            newNetworks = new ArrayList<>();
        }

        @Override
        public CyNetwork[] getNetworks() {
            return newNetworks.toArray(new CyNetwork[0]);
        }

        @Override
        public CyNetworkView buildCyNetworkView(CyNetwork network) {
            return cy.vf.createNetworkView(network);
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {

            taskMonitor.setTitle("Importing Java files: " + inputName);

            if (edges == null || nodes == null) {
                nodes = new HashSet<>();
                edges = new HashSet<>();
                if (srcFolder == null)
                    readFromJar(nodes, edges);
                else
                    readFromSource(nodes, edges);
            }

            // Create network
            CyNetwork network = cy.nf.createNetwork();

            for (var n : nodes) {
                newNode(network, n);
            }

            for (var e : edges) {
                var split = e.split(" ");
                newEdge(network, split[0], split[1], split.length >= 3 ? split[2] : null);
            }

            initJavaColumns(network);
            newNetworks.add(network);

            afterComplete.accept(this);
        }

        public void loadIntoView(CyNetworkManager nm, CyNetworkViewManager vm) {
            for (var n : getNetworks()) {
                nm.addNetwork(n, true);
                var view = buildCyNetworkView(n);
                vm.addNetworkView(view, true);
            }
        }

        private String formatEdgeString(String s) {
            if (s == null) return null;

            if (hideInnerClasses && hideAnonymousClasses) {
                // Ignore all inner members
                s = s.replaceAll("\\$[\\dA-Za-z$]*", "");
            } else {
                // Ignore unnamed inner references / static references
                s = s.replaceAll("\\$(?![\\dA-Za-z$])", "");
                s = s.replaceAll("\\$class(?![\\dA-Za-z$])", "");

                if (hideInnerClasses) {
                    // Ignore named inner classes
                    s = s.replaceAll("\\$[a-zA-Z][\\da-zA-Z$]*.*?", "");
                }

                if (hideAnonymousClasses) {
                    // Ignore anonymous classes and functions
                    s = s.replaceAll("\\$\\d[\\da-zA-Z$]*.*?", "");
                    s = s.replaceAll("\\$\\$[\\da-zA-Z$]*.*?", "");
                }
            }
            return s;
        }

        private void readFromSource(Set<String> nodes, Set<String> edges) {

            if (!EdgeClassVisitor.isValidSRC(srcFolder))
                throw new RuntimeException("Invalid SRC folder");

            var parsed = EdgeClassVisitor.parseSRCFolder(srcFolder);
            var result = EdgeClassVisitor.visitAll(parsed, true);

            nodes.addAll(result[0].stream().map(this::formatEdgeString).collect(Collectors.toSet()));
            edges.addAll(result[1].stream().map(this::formatEdgeString).collect(Collectors.toSet()));

            nodes.remove(null);
            edges.remove(null);
        }

        private void readFromJar(Set<String> nodes, Set<String> edges) {

            PrintStream ps = new PrintStream(new OutputStream() {
                public void write(int b) {
                }
            }) {
                public void print(String s) {
                    s = formatEdgeString(s);
                    if (s == null) return;

                    // Only add source nodes
                    nodes.add(s.split(" ")[0]);
                    edges.add(s);
                }
            };


            try (var jar = new JarInputStream(inputStream)) {
                var e = jar.getNextJarEntry();
                while (e != null) {

                    if (cancelled) return;

                    if (!e.isDirectory() && e.getName().endsWith(".class")) {

                        ClassParser cp = new ClassParser(jar, e.getName());
                        var classVisitor = new ClassVisitor(cp.parse());
                        classVisitor.setPrintStream(ps);
                        classVisitor.start();
                    }

                    e = jar.getNextJarEntry();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        protected CyNode newNode(CyNetwork network, String fullName) {
            CyNode node = network.addNode();
            CyTable table = network.getDefaultNodeTable();

            if (table.getColumn(NODE_NAME) == null)
                table.createColumn(NODE_NAME, String.class, false);

            table.getRow(node.getSUID()).set(NODE_NAME, fullName);
            return node;
        }


        protected CyEdge newEdge(CyNetwork network, String from, String to, String interaction) {
            // Ignore self edges
            if (from.equals(to))
                return null;

            // Ignore edges from inner class to parent
            if (from.contains("$") && from.startsWith(to))
                return null;

            CyTable nodeTable = network.getDefaultNodeTable();
            Collection<Long> fromMatches = nodeTable.getMatchingKeys(NODE_NAME, from, Long.class);
            long fromS;
            if (fromMatches.size() == 0)
                fromS = newNode(network, from).getSUID();
            else
                fromS = fromMatches.iterator().next();

            Collection<Long> toMatches = nodeTable.getMatchingKeys(NODE_NAME, to, Long.class);
            long toS;
            if (toMatches.size() == 0)
                return null; // Skip classes outside the domain
                //toS = newNode(network, to).getSUID();
            else
                toS = toMatches.iterator().next();

            CyEdge edge = network.addEdge(network.getNode(fromS), network.getNode(toS), true);

            CyTable edgeTable = network.getDefaultEdgeTable();

            if (edgeTable.getColumn(EDGE_NAME) == null)
                edgeTable.createColumn(EDGE_NAME, String.class, false);
            edgeTable.getRow(edge.getSUID()).set(EDGE_NAME, from + " > " + to);

            if (edgeTable.getColumn(EDGE_INTERACTION) == null)
                edgeTable.createColumn(EDGE_INTERACTION, String.class, false);
            if (interaction != null && !interaction.equals(""))
                edgeTable.getRow(edge.getSUID()).set(EDGE_INTERACTION, interaction);

            return edge;
        }

        protected void initJavaColumns(CyNetwork net) {

            var table = net.getDefaultNodeTable();

            var map = new HashMap<String, Class<?>>();
            map.put(NODE_NAME, String.class);
            map.put(NODE_CLASS, String.class);
            map.put(NODE_PACKAGE, String.class);

            if (table.getColumn(NODE_PACKAGE) == null) {
                table.createColumn(NODE_PACKAGE, String.class, false);
            }

            if (table.getColumn(NODE_CLASS) == null) {
                table.createColumn(NODE_CLASS, String.class, false);
            }

            if (table.getColumn(NODE_INNER_CLASS) == null) {
                table.createColumn(NODE_INNER_CLASS, String.class, false);
            }

            cy.eq.compile(String.format(CLASS_FORMULA), map);
            table.getAllRows().forEach(r -> r.set(NODE_CLASS, cy.eq.getEquation()));

            cy.eq.compile(PACKAGE_FORMULA, map);
            table.getAllRows().forEach(r -> r.set(NODE_PACKAGE, cy.eq.getEquation()));

            cy.eq.compile(INNER_CLASS_FORMULA, map);
            table.getAllRows().forEach(r -> r.set(NODE_INNER_CLASS, cy.eq.getEquation()));
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

}
