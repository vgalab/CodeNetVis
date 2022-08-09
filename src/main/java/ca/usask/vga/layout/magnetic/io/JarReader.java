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
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import java.io.*;
import java.util.*;
import java.util.jar.JarInputStream;

public class JarReader extends AbstractInputStreamTaskFactory {

    public static final String NODE_NAME = "name", NODE_CLASS = "Class", NODE_PACKAGE = "Package",
            NODE_INNER_CLASS = "Simple class";

    public static final String EDGE_NAME = "name";

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

    private final JarReader.CyAccess cy;

    protected JarReader(CyFileFilter fileFilter, JarReader.CyAccess dependencies) {
        super(fileFilter);
        this.cy = dependencies;
    }

    public static JarReader create(JarReader.CyAccess dependencies, StreamUtil streamUtil) {

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
        return new JarReader(filter, dependencies);

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
        return new TaskIterator(new JarReader.ReaderTask(inputStream, inputName, cy));
    }

    public class ReaderTask implements CyNetworkReader {

        private final InputStream inputStream;
        private final String inputName;
        private final JarReader.CyAccess cy;

        private boolean cancelled;

        private final List<CyNetwork> newNetworks;

        public String[] packagesToIgnore = {"java.", "javax.", "com.sun.", "jdk."};

        @Tunable(description="Root package of interest:")
        public String userPackage = "";

        @Tunable(description="Ignore java.*, javax.*, jdk.*, com.sun.*")
        public boolean ignoreJavaLibraries = true;

        @Tunable(description="Hide inner classes:")
        public boolean hideInnerClasses = true;

        @Tunable(description="Hide anonymous classes:")
        public boolean hideAnonymousClasses = true;

        public ReaderTask(InputStream inputStream, String inputName, JarReader.CyAccess dependencies) {
            this.inputStream = inputStream;
            this.inputName = inputName;
            cy = dependencies;
            newNetworks = new ArrayList<>();
        }

        public ReaderTask(String filename, JarReader.CyAccess dependencies) throws FileNotFoundException {
            this.inputStream = new FileInputStream(filename);
            this.inputName = filename;
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

            taskMonitor.setTitle("Importing a JAR file: " + inputName);

            Set<String> edges = new HashSet<>();

            PrintStream ps = new PrintStream(new OutputStream() {
                public void write(int b) {}
            }) {
                public void print(String s) {
                    if (s == null) return;

                    if (!userPackage.equals("")) {
                        if (!s.startsWith(userPackage) || !s.split(" ")[1].startsWith(userPackage)) {
                            return;
                        }
                    }

                    if (ignoreJavaLibraries)
                        for (String p : packagesToIgnore)
                            if (s.startsWith(p) || s.split(" ")[1].startsWith(p))
                                return;

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

            // Create network
            CyNetwork network = cy.nf.createNetwork();

            for (var e : edges) {
                var split = e.split(" ");
                newEdge(network, split[0], split[1]);
            }

            initJavaColumns(network);
            newNetworks.add(network);
        }

        protected CyNode newNode(CyNetwork network, String fullName) {
            CyNode node = network.addNode();
            CyTable table = network.getDefaultNodeTable();

            if (table.getColumn(NODE_NAME) == null)
                table.createColumn(NODE_NAME, String.class, false);

            table.getRow(node.getSUID()).set(NODE_NAME, fullName);
            return node;
        }


        protected CyEdge newEdge(CyNetwork network, String from, String to) {
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
                toS = newNode(network, to).getSUID();
            else
                toS = toMatches.iterator().next();

            CyEdge edge = network.addEdge(network.getNode(fromS), network.getNode(toS), true);

            CyTable edgeTable = network.getDefaultEdgeTable();

            if (edgeTable.getColumn(EDGE_NAME) == null)
                edgeTable.createColumn(EDGE_NAME, String.class, false);
            edgeTable.getRow(edge.getSUID()).set(EDGE_NAME, from + " > " + to);

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
