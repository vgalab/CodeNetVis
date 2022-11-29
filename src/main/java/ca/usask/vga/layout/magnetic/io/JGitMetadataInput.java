
package ca.usask.vga.layout.magnetic.io;

import com.google.common.collect.Iterables;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static ca.usask.vga.layout.magnetic.io.JavaReader.PATH_TO_FILES_COLUMN;

/**
 * Class for loading Git metadata for the Cytoscape network from its repository.
 * The network must be created from a local git repository with commit history available.
 */
public class JGitMetadataInput implements AutoCloseable {

    // New CyNode table column names
    public static String TOTAL_COMMITS = "Total Commits", LAST_COMMIT_DATE = "Last Commit Date",
            LAST_COMMIT_MESSAGE = "Last Commit Message", LAST_COMMIT_AUTHOR = "Last Commit Author",
            LAST_COMMIT_SHA = "Last Commit SHA";

    private final Git git;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private boolean cancelled = false;

    /**
     * Checks if the metadata has already been loaded for this network.
     */
    public static boolean isMetadataLoaded(CyNetwork network) {
        CyTable nodeTable = network.getDefaultNodeTable();
        return nodeTable.getColumn(TOTAL_COMMITS) != null;
    }

    /**
     * Create a new JGitMetadata object from a path, and prepare for working with data.
     * Throws an IOException if there is no such repository at the address.
     */
    public JGitMetadataInput(String repoPath) throws IOException {
        git = Git.open(new File(repoPath));
    }

    /**
     * Create a new JGitMetadata object from a network, and prepare for working with data.
     * Attempts to get the repository path from the CyNetwork table.
     * Throws an IOException if there is no such repository at the address.
     */
    public JGitMetadataInput(CyNetwork network) throws IOException  {
        String packageRootFolder = network.getDefaultNetworkTable()
                .getRow(network.getSUID()).get(PATH_TO_FILES_COLUMN, String.class);

        if (packageRootFolder.matches("(http|file)s?:.*")) {
            // If it is a file or http link, notify user
            throw new FileNotFoundException("The provided network does not have a local git repository.");
        }

        File gitDir = findGitRepoRoot(new File(packageRootFolder));
        String repoPath = gitDir.getPath();

        git = Git.open(new File(repoPath));
    }

    /**
     * Create a single Git metadata column for the network, if it doesn't already exist.
     * Helper method for {@link #createDataColumns(CyNetwork)}.
     */
    private void createDataColumn(CyNetwork network, String column, Class<?> type) {
        CyTable nodeTable = network.getDefaultNodeTable();
        if (nodeTable.getColumn(column) == null) {
            nodeTable.createColumn(column, type, false);
        }
    }

    /**
     * Create all required Git metadata columns for the network, if they don't already exist.
     * They are: {@link #TOTAL_COMMITS}, {@link #LAST_COMMIT_DATE}, {@link #LAST_COMMIT_MESSAGE},
     * {@link #LAST_COMMIT_AUTHOR}, {@link #LAST_COMMIT_SHA}.
     */
    public void createDataColumns(CyNetwork network) {
        createDataColumn(network, TOTAL_COMMITS, Integer.class);
        createDataColumn(network, LAST_COMMIT_DATE, String.class);
        createDataColumn(network, LAST_COMMIT_MESSAGE, String.class);
        createDataColumn(network, LAST_COMMIT_AUTHOR, String.class);
        createDataColumn(network, LAST_COMMIT_SHA, String.class);
    }

    /**
     * Attempts to find the relative path of the node in the local git repository.
     * If it is unavailable, prints the error message but does not throw an exception.
     */
    public String getNodePath(CyNetwork network, CyNode node) {
        createDataColumns(network);
        CyTable nodeTable = network.getDefaultNodeTable();

        String javaPackage = nodeTable.getRow(node.getSUID()).get("name", String.class);

        String packageRootFolder = network.getDefaultNetworkTable()
                .getRow(network.getSUID()).get(PATH_TO_FILES_COLUMN, String.class);

        String fullPath = packageRootFolder + javaPackage.replace(".", "/") + ".java";

        File nodeFile = new File(fullPath);

        if (!nodeFile.exists()) {
            System.err.println("File " + fullPath + " does not exist.");
            return null;
        }

        File gitRoot = findGitRepoRoot(nodeFile);

        if (gitRoot == null) {
            System.err.println("File " + fullPath + " is not part of a git repository");
            return null;
        }

        String relativePath = gitRoot.toPath().relativize(nodeFile.toPath()).toString();

        System.out.println("Relative Path: " + relativePath);

        return relativePath;
    }

    /**
     * Attempts to load the Git metadata for the provided node in the network.
     * If data is unavailable, prints the error message but does not throw an exception.
     */
    public void getNodeData(CyNetwork network, CyNode node) {
        String relativePath = getNodePath(network, node);
        if (relativePath == null) return;

        // Git only accepts / as separators
        relativePath = relativePath.replace("\\", "/");

        CyTable nodeTable = network.getDefaultNodeTable();

        int totalCommits = 0;
        Date lastCommitDate = new Date(0);
        String lastCommitMessage = "", lastCommitAuthor = "", lastCommitSHA = "";

        try {
            totalCommits = Iterables.size(git.log().addPath(relativePath).call());
            for (RevCommit commit : git.log().addPath(relativePath).setMaxCount(1).call()) {
                lastCommitDate = commit.getAuthorIdent().getWhen();
                lastCommitMessage = commit.getFullMessage();
                lastCommitAuthor = commit.getAuthorIdent().getName();
                lastCommitSHA = commit.getName();
            }
        } catch (GitAPIException e) {
            System.err.println("Error getting Git data for " + relativePath);
            e.printStackTrace();
            return;
        }

        // Update the table for this node
        nodeTable.getRow(node.getSUID()).set(TOTAL_COMMITS, totalCommits);
        nodeTable.getRow(node.getSUID()).set(LAST_COMMIT_DATE, dateFormat.format(lastCommitDate));
        nodeTable.getRow(node.getSUID()).set(LAST_COMMIT_MESSAGE, lastCommitMessage);
        nodeTable.getRow(node.getSUID()).set(LAST_COMMIT_AUTHOR, lastCommitAuthor);
        nodeTable.getRow(node.getSUID()).set(LAST_COMMIT_SHA, lastCommitSHA);
    }

    /**
     * Loads all the node Git metadata for the given network. Blocks the current thread.
     */
    public void loadAllNodeData(CyNetwork network, TaskMonitor taskMonitor) {
        createDataColumns(network);

        if (taskMonitor != null) taskMonitor.setProgress(0);
        float totalNodes = network.getNodeCount(), nodesProcessed = 0;

        // For every node, load the metadata by its local filepath
        for (CyNode node : network.getNodeList()) {
            if (cancelled) return;
            System.out.println("Reading node " + node.getSUID());
            getNodeData(network, node);
            nodesProcessed++;
            if (taskMonitor != null) taskMonitor.setProgress(nodesProcessed / totalNodes);
        }
    }

    /**
     * Creates a TaskIterator for loading all the node Git metadata for the given network.
     * Does not block the current thread when executed with {@link TaskManager#execute(TaskIterator)}.
     * Updates the progress bar in the Cytoscape GUI to show the percentage of nodes processed.
     */
    public static TaskIterator loadGitTaskIterator(CyNetwork network) {
        return new TaskIterator(new Task() {
            JGitMetadataInput input;
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                taskMonitor.setTitle("Loading Git metadata for all nodes in the network...");
                input = new JGitMetadataInput(network);
                input.loadAllNodeData(network, taskMonitor);
                input.close();
                if (!input.cancelled) {
                    taskMonitor.setStatusMessage("Git metadata loaded successfully.");
                } else {
                    taskMonitor.setStatusMessage("Git metadata loading cancelled.");
                }
            }
            @Override
            public void cancel() {
                input.cancelled = true;
            }
        });
    }

    /**
     * Find the root of the Git repository (.git location) that contains
     * the provided folder somewhere in its subdirectories.
     * @return the root folder file, or null if no parent folder contains .git
     */
    public static File findGitRepoRoot(File topPath) {

        File directory = topPath;
        // Make sure to start with a directory
        if (!topPath.isDirectory())
            directory = topPath.getParentFile();

        File gitDir = new File(directory, ".git");

        while (!gitDir.exists() && directory != null) {
            // Go up one layer searching for the .git repository folder
            directory = directory.getParentFile();
            gitDir = new File(directory, ".git");
        }

        return directory;
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.*/
    @Override
    public void close() {
        git.close();
    }
}
