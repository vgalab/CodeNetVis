package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.io.JavaReader;
import org.apache.commons.io.FileUtils;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.*;
import org.cytoscape.work.swing.DialogTaskManager;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;

/**
 * Used to create a new graph from existing data, such as
 * a JAR file, Java source folder or a GitHub link.
 */
public class SoftwareImport {

    private final DialogTaskManager dtm;
    private final FileUtil fu;
    private final LoadNetworkFileTaskFactory nftf;
    private final JavaReader.CyAccess readerAccess;
    private final CyNetworkManager nm;
    private final CyNetworkViewManager vm;
    private final FileUtil fileUtil;
    private final CySwingApplication swingApp;

    public SoftwareImport(DialogTaskManager dtm, FileUtil fu, LoadNetworkFileTaskFactory nftf, JavaReader.CyAccess readerAccess,
                          CyNetworkManager nm, CyNetworkViewManager vm, FileUtil fileUtil, CySwingApplication swingApp) {
        this.dtm = dtm;
        this.fu = fu;
        this.nftf = nftf;
        this.readerAccess = readerAccess;
        this.nm = nm;
        this.vm = vm;
        this.fileUtil = fileUtil;
        this.swingApp = swingApp;
    }

    public void loadFromFile(Consumer<String> onSuccess) {

        File f = fu.getFile(swingApp.getJFrame(), "New graph from file", FileUtil.LOAD, new HashSet<>());

        if (f == null) return;

        dtm.execute(nftf.createTaskIterator(f), new TaskObserver() {
            public void taskFinished(ObservableTask task) {}
            public void allFinished(FinishStatus finishStatus) {
                if (onSuccess != null && finishStatus.getType() == FinishStatus.Type.SUCCEEDED)
                    onSuccess.accept(f.getName());
            }
        });

    }

    public void loadFromSrcFolder(String path, Consumer<String> onSuccess) {
        System.out.println("Importing Java source code from: " + path);
        if (path.equals("")) return;
        dtm.execute(new TaskIterator(new JavaReader.ReaderTask(path, readerAccess, rt -> {
            rt.loadIntoView(nm, vm);
            onSuccess.accept(path);
        })));
    }

    public void loadFromSrcFolderDialogue(String initialFolder, Consumer<String> onSuccess) {
        File folder = fileUtil.getFolder(swingApp.getJFrame(), "Select one Java SRC folder", initialFolder);
        loadFromSrcFolder(folder.getAbsolutePath(), onSuccess);
    }

    // LOAD FROM GITHUB FUNCTIONS:

    public void loadFromGitHub(String url, Consumer<String> onSuccess) {
        try {
            var repoName = getRepoByURL(url);

            GitHub github = new GitHubBuilder().build();
            var repo = github.getRepository(repoName);

            System.out.println("\nFound GitHub repo: " + repo.getFullName());

            downloadOrReadAsync(repo, (contents) -> {
                String sourceFolder = contents.getPath() + "/src/";
                if (Files.exists(Paths.get(sourceFolder))) {
                    loadFromSrcFolder(sourceFolder, onSuccess);
                } else {
                    loadFromSrcFolderDialogue(contents.getPath(), onSuccess);
                }
            });

        } catch (IOException e) {e.printStackTrace();}
    }

    private String getRepoByURL(String url) {
        if (!isValidGitHubUrl(url)) throw new IllegalArgumentException("Invalid GitHub URL: " + url);
        var split = url.replaceAll(".*github.com/", "").split("\\?")[0].split("/");
        var string = split.length <= 1 ? split[0] : split[0] + "/" + split[1];
        return string.replace(".git", "").strip();
    }

    public boolean isValidGitHubUrl(String url) {
        return url.matches(".*github.com/.*");
    }

    private void downloadOrReadAsync(GHRepository repo, Consumer<File> onComplete) {
        final File[] folder = {null};
        dtm.execute(new TaskIterator(new AbstractTask() {
            @Override
            public void run(TaskMonitor taskMonitor) {
                taskMonitor.setTitle("Downloading " + repo.getFullName());
                folder[0] = downloadOrRead(repo);
            }
        }), new TaskObserver() {
            public void taskFinished(ObservableTask task) {}
            public void allFinished(FinishStatus finishStatus) {
                if (finishStatus.getType() == FinishStatus.Type.SUCCEEDED)
                    onComplete.accept(folder[0]);
            }
        });
    }

    private File downloadOrRead(GHRepository repo) {
        File tempDir = getTempDir(repo.getFullName());

        try (var entries = Files.list(tempDir.toPath())) {
            var entry = entries.findFirst();
            if (entry.isPresent())
                return entry.get().toFile();

            return repo.readZip(input -> {
                ZipUtil.unpack(input, tempDir);
                System.out.println("Downloaded repo to: " + tempDir.getAbsolutePath());
                return downloadOrRead(repo);
            }, null);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String tempDirPath() {
        String tempDir = System.getProperty("java.io.tmpdir");
        return tempDir + "/cytoscape-loaded-repos/";
    }

    private File getTempDir(String repoName) {
        repoName = repoName.replace("/", "-");
        String subfolder = tempDirPath() + repoName + "/";
        File f = new File(subfolder);
        if (!f.exists()) {
            if (!f.mkdirs()) {
                throw new RuntimeException("Could not create temp directory: " + subfolder);
            }
        }
        return f;
    }

    public void clearTempDir() {
        File f = new File(tempDirPath());
        if (f.exists()) {
            try {
                FileUtils.deleteDirectory(f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getTempDirSize() {
        File f = new File(tempDirPath());
        if (f.exists()) {
            return readableFileSize(FileUtils.sizeOfDirectory(f));
        }
        return readableFileSize(0);
    }

    // From: https://stackoverflow.com/a/5599842
    private String readableFileSize(long size) {
        if(size <= 0) return "(empty)";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
