package ca.usask.vga.layout.magnetic.io;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.GenericListVisitorAdapter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java source code parser that goes through every class definition in the
 * given project and creates an edge for every interaction between classes.
 */
public class EdgeClassVisitor extends GenericListVisitorAdapter<String, Map<String, String>> {

    private String currentClassName;

    private String USES = "USES", CALL = "CALL", CREATION = "CREATION", DECLARATION = "DECLARATION",
            RETURN = "RETURN", PARAMETER = "PARAMETER", EXTENDS = "EXTENDS", IMPLEMENTS = "IMPLEMENTS",
            FIELD = "FIELD", INSIDEOF = "INSIDEOF";

    private static final Pattern INNER_CLASS = Pattern.compile("\\.[A-Z][A-Za-z0-9]*(?=\\.[A-Z0-9])");

    private static String innerClassToDollar(String className) {
        Matcher m = INNER_CLASS.matcher(className);
        while (m.find()) {
            className = className.replaceAll("\\.(?!.*\\.)", "\\$");
        }
        return className;
    }

    private static List<String> createEdge(String source, String target, String type) {
        if (source == null || target == null) return Collections.emptyList();
        target = target.replaceAll("\\[+.*]+", ""); // remove array brackets
        target = target.replaceAll("<+.*>+", ""); // remove generics
        target = innerClassToDollar(target);
        source = innerClassToDollar(source);
        if (!target.contains(".")) return Collections.emptyList(); // skip if no package, primitive, etc.
        return Collections.singletonList(source + " " + target + " " + type);
    }

    public static Set<String>[] visitAll(Collection<CompilationUnit> compilations, boolean allInteractions) {
        Set<String> nodes = new HashSet<>();
        Map<String, String> classDefinitions = new HashMap<>() {
            @Override
            public String put(String key, String value) {
                nodes.add(value);
                return super.put(key, value);
            }
        };
        for (var cu : compilations)
            new EdgeClassVisitor(allInteractions).visit(cu, classDefinitions);
        Set<String> edges = new HashSet<>();
        for (var cu : compilations)
            edges.addAll(new EdgeClassVisitor(allInteractions).visit(cu, classDefinitions));
        System.out.println("\nDone Java class import!");
        return new Set[]{nodes, edges};
    }

    public static boolean isValidSRC(String srcFolder) {
        srcFolder = srcFolder.replace("\\", "/");
        return srcFolder.matches(".*[\\\\/]src*.");
    }

    public static List<CompilationUnit> parseSRCFolder(String srcFolder) {

        if (!isValidSRC(srcFolder)) throw new InvalidPathException(srcFolder, "Must be a java src folder");
        srcFolder = srcFolder.replace("\\", "/");

        String javaSrcFolder = srcFolder + "";
        if (srcFolder.endsWith("src")) javaSrcFolder += "/main/java";
        else if (srcFolder.endsWith("src/")) javaSrcFolder += "main/java";

        if (new File(javaSrcFolder).exists()) {
            srcFolder = javaSrcFolder;
        }

        Path pathToSource = new File(srcFolder).toPath();

        // Set up a minimal type solver that only looks at the classes used to run this sample.
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(pathToSource));

        // Configure JavaParser to use type resolution
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(symbolSolver);

        SourceRoot sourceRoot = new SourceRoot(pathToSource, config);
        try {
            sourceRoot.tryToParse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sourceRoot.getCompilationUnits();
    }

    public EdgeClassVisitor(boolean allInteractions) {
        if (!allInteractions) {
            USES = "USES"; CALL = USES; CREATION = USES; DECLARATION = USES; RETURN = USES;
            PARAMETER = USES; EXTENDS = USES; IMPLEMENTS = USES; FIELD = USES; INSIDEOF = USES;
        }
    }

    public void resolveOrMatchType(NodeWithType n, List<String> edges, Map<String, String> arg, String interaction) {
        String resolved = null;
        try {
            resolved = n.getType().resolve().describe();
        } catch (Exception | StackOverflowError ignored) {
            if (arg != null) resolved = arg.get(n.getTypeAsString());
        }
        if (resolved != null) {
            edges.addAll(createEdge(currentClassName, resolved, interaction));
        }
    }

    public void resolveOrMatchClass(ClassOrInterfaceType n, List<String> edges, Map<String, String> arg, String interaction) {
        if (interaction.equals(USES)) return; // Skip generic uses
        String resolved = null;
        try {
            resolved = n.resolve().describe();
        } catch (Exception | StackOverflowError ignored) {
            if (arg != null) resolved = arg.get(n.getNameAsString());
        }
        if (resolved != null) {
            edges.addAll(createEdge(currentClassName, resolved, interaction));
        }
    }

    @Override
    public List<String> visit(CompilationUnit n, Map<String, String> arg) {
        try {
            return super.visit(n, arg);
        } catch (StackOverflowError ignored) {
            System.out.println("\nSkipping a compilation unit due to stack overflow");
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> visit(ClassOrInterfaceType n, Map<String, String> arg) {
        List<String> edges = super.visit(n, arg);
        resolveOrMatchClass(n, edges, arg, USES);
        return edges;
    }

    @Override
    public List<String> visit(ClassOrInterfaceDeclaration n, Map<String, String> arg) {
        String lastClassName = currentClassName;
        currentClassName = innerClassToDollar(n.resolve().getQualifiedName());
        if (arg != null) {
            arg.put(n.getNameAsString(), currentClassName);
            //System.out.println(n.getNameAsString() + " -> " + currentClassName);
        }
        // System.out.println("\nClass: " + currentClassName + (n.isInnerClass() ? " INNER" : ""));
        System.out.printf("Exploring class: %s\r", currentClassName);
        // System.out.println("Extends: " + n.getExtendedTypes());
        // System.out.println("Implements: " + n.getImplementedTypes());
        List<String> edges = new ArrayList<>();
        for (var c : n.getImplementedTypes()) {
            resolveOrMatchClass(c, edges, arg, IMPLEMENTS);
        }
        for (var c : n.getExtendedTypes()) {
            resolveOrMatchClass(c, edges, arg, EXTENDS);
        }
        edges.addAll(super.visit(n, arg));
        edges.addAll(createEdge(currentClassName, lastClassName, INSIDEOF));
        currentClassName = lastClassName;
        return edges;
    }

    @Override
    public List<String> visit(EnumDeclaration n, Map<String, String> arg) {
        String lastClassName = currentClassName;
        currentClassName = innerClassToDollar(n.resolve().getQualifiedName());
        if (arg != null) {
            arg.put(n.getNameAsString(), currentClassName);
            //System.out.println(n.getNameAsString() + " -> " + currentClassName);
        }
        // System.out.println("\nEnum: " + currentClassName);
        System.out.printf("Exploring class: %s\r", currentClassName);
        // System.out.println("Extends: " + n.getExtendedTypes());
        // System.out.println("Implements: " + n.getImplementedTypes());
        List<String> edges = super.visit(n, arg);
        edges.addAll(createEdge(currentClassName, lastClassName, INSIDEOF));
        currentClassName = lastClassName;
        return edges;
    }

    @Override
    public List<String> visit(MethodDeclaration n, Map<String, String> arg) {
        //System.out.println( n.getName() + " | " + n.getParameters() + " | " + n.getType());
        List<String> edges = super.visit(n, arg);
        // RETURN
        resolveOrMatchType(n, edges, arg, RETURN);
        // PARAMETERS
        for (var p : n.getParameters()) {
            resolveOrMatchType(p, edges, arg, PARAMETER);
        }
        return edges;
    }

    @Override
    public List<String> visit(FieldDeclaration n, Map<String, String> arg) {
        List<String> edges = super.visit(n, arg);
        for (var p : n.getVariables()) {
            resolveOrMatchType(p, edges, arg, FIELD);
        }
        return edges;
    }

    @Override
    public List<String> visit(MethodCallExpr n, Map<String, String> arg) {
        List<String> edges = super.visit(n, arg);
        //System.out.println("Call: " + n + " | ");
        try {
            var t = n.resolve().declaringType().getQualifiedName();
            edges.addAll(createEdge(currentClassName, t, CALL));
            //System.out.println("Call to: " + t);
        } catch (Exception ignored) {
        }
        return edges;
    }

    @Override
    public List<String> visit(VariableDeclarationExpr n, Map<String, String> arg) {
        List<String> edges = super.visit(n, arg);
        for (var p : n.getVariables()) {
            resolveOrMatchType(p, edges, arg, DECLARATION);
        }
        return edges;
    }

    @Override
    public List<String> visit(ObjectCreationExpr n, Map<String, String> arg) {
        List<String> edges = super.visit(n, arg);
        resolveOrMatchType(n, edges, arg, CREATION);

        return edges;
    }
}