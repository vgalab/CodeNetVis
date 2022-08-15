package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.io.JarReader;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.swing.DialogTaskManager;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;

public class SoftwareImport {

    private final DialogTaskManager dtm;
    private final FileUtil fu;
    private final LoadNetworkFileTaskFactory nftf;
    private final JarReader.CyAccess readerAccess;

    public SoftwareImport(DialogTaskManager dtm, FileUtil fu, LoadNetworkFileTaskFactory nftf, JarReader.CyAccess readerAccess) {
        this.dtm = dtm;
        this.fu = fu;
        this.nftf = nftf;
        this.readerAccess = readerAccess;
    }

    public void loadFromFile(Component parent, Consumer<String> onSuccess) {

        File f = fu.getFile(parent, "New graph from file", FileUtil.LOAD, new HashSet<>());

        if (f == null) return;

        dtm.execute(nftf.createTaskIterator(f), new TaskObserver() {
            public void taskFinished(ObservableTask task) {}
            public void allFinished(FinishStatus finishStatus) {
                if (onSuccess != null && finishStatus.getType() == FinishStatus.Type.SUCCEEDED)
                    onSuccess.accept(f.getName());
            }
        });

    }

    // LOAD FROM GITHUB FUNCTIONS:

    public void loadFromGitHub() {
        try {

            GitHub github = new GitHubBuilder().withPassword("BJNick", "335622git").build();
            var repo = github.getRepository("BJNick/CytoscapeMagneticLayout");

            System.out.println("Repo: " + repo.getName());

            var classes = getAllClasses(repo);

            for (var clazz : classes) {
                System.out.println(clazz.getName());
            }

            var contents= convertAll(classes);
            filterImports(contents, getClassFullNames(contents));
            loadGraph(contents);


        } catch (Exception e) {e.printStackTrace();}
    }


    private Collection<GHContent> getAllClasses(GHRepository repo) throws IOException {

        var mainDir = repo.getDirectoryContent("src/main/java");

        Queue<GHContent> toExplore = new LinkedList<>(mainDir);

        var classes = new HashSet<GHContent>();

        while (!toExplore.isEmpty()) {
            System.out.println("Exploring " + toExplore.size() + " directories");
            var current = toExplore.poll();
            if (current.isDirectory()) {
                var subdir = repo.getDirectoryContent(current.getPath());
                toExplore.addAll(subdir);
            } else {
                classes.add(current);
            }
        }

        System.out.println("Classes: " +  classes.size());

        return classes;
    }

    private static class JavaFileContents {

        GHContent origin;

        String className;
        String path;
        String packageName;
        String fullName;

        Collection<String> imports;

        @Override
        public String toString() {
            return "JavaFileContents{" +
                    "className='" + className + '\'' +
                    ", path='" + path + '\'' +
                    ", packageName='" + packageName + '\'' +
                    ", fullName='" + fullName + '\'' +
                    ", importsSize=" + imports.size() +
                    '}';
        }
    }

    private JavaFileContents readGHJavaFile(GHContent javaFile) throws IOException {

        var fileContents = new JavaFileContents();

        fileContents.origin = javaFile;
        fileContents.className = javaFile.getName().replace(".java", "");
        fileContents.path = javaFile.getPath();

        InputStream stream = javaFile.read();
        Scanner scanner = new Scanner(stream);
        // Get all the import statements

        var imports = new HashSet<String>();

        while (scanner.hasNextLine()) {
            var line = scanner.nextLine();
            if (line.startsWith("package")) {
                fileContents.packageName = line.replace("package", "").replace(";", "").trim();
            } else if (line.startsWith("import")) {
                var importStatement = line.replace("import", "").replace(";", "").trim();
                imports.add(importStatement);
            }
        }

        fileContents.imports = imports;
        fileContents.fullName = fileContents.packageName + "." + fileContents.className;

        System.out.println("File: " + fileContents);

        return fileContents;
    }

    private Collection<JavaFileContents> convertAll(Collection<GHContent> classes) throws IOException {
        var javaFiles = new HashSet<JavaFileContents>();
        for (var clazz : classes) {
            System.out.println("Reading: " + clazz.getName());
            javaFiles.add(readGHJavaFile(clazz));
        }
        return javaFiles;
    }

    private Set<String> getClassFullNames(Collection<JavaFileContents> javaFiles) {
        var fullNames = new HashSet<String>();
        for (var javaFile : javaFiles) {
            fullNames.add(javaFile.fullName);
        }
        return fullNames;
    }

    private void filterImports(Collection<JavaFileContents> javaFiles, Set<String> fullNames) {
        for (var javaFile : javaFiles) {
            javaFile.imports.removeIf(f -> !fullNames.contains(f));
        }
    }

    private void loadGraph(Collection<JavaFileContents> javaFiles) throws FileNotFoundException {
        Set<String> nodes = new HashSet<>();
        Set<String> edges = new HashSet<>();
        for (var javaFile : javaFiles) {
            nodes.add(javaFile.fullName);
            for (var importStatement : javaFile.imports) {
                edges.add(javaFile.fullName + " " + importStatement);
            }
        }
        dtm.execute(new TaskIterator(new JarReader.ReaderTask(nodes, edges, readerAccess)));
    }

}
