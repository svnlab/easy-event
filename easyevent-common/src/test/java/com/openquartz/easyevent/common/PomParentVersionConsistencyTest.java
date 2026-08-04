package com.openquartz.easyevent.common;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Regression test for the root-pom version sync bug:
 * <p>
 * The root pom uses the CI-friendly version placeholder {@code ${revision}}
 * (see {@code <revision>} in the root {@code pom.xml}). Every module whose
 * {@code <parent>} belongs to {@code com.openquartz} must reference that
 * placeholder (or the concrete revision value) instead of a hard-coded,
 * possibly outdated version. Otherwise Maven cannot resolve the parent POM
 * and the whole reactor build fails.
 *
 * @author easy-event
 */
public class PomParentVersionConsistencyTest {

    private static final String ROOT_ARTIFACT_ID = "easy-event";
    private static final String REVISION_PLACEHOLDER = "${revision}";

    @Test
    public void testAllModuleParentVersionsMatchRootRevision() throws Exception {
        Path root = findProjectRoot();
        String revision = readRootRevision(root);

        List<Path> pomFiles = collectPomFiles(root);
        assertTrue("No pom.xml files found under project root: " + root, !pomFiles.isEmpty());

        List<String> mismatches = new ArrayList<>();
        for (Path pom : pomFiles) {
            Document doc = parseXml(pom);
            NodeList parentNodes = doc.getElementsByTagNameNS("*", "parent");
            for (int i = 0; i < parentNodes.getLength(); i++) {
                Element parent = (Element) parentNodes.item(i);
                String groupId = childElementText(parent, "groupId");
                if (!"com.openquartz".equals(groupId)) {
                    continue;
                }
                String version = childElementText(parent, "version");
                boolean inSync = REVISION_PLACEHOLDER.equals(version) || revision.equals(version);
                if (!inSync) {
                    mismatches.add(relativize(root, pom) + " declares parent version '" + version
                            + "' but root <revision> is '" + revision + "'");
                }
            }
        }

        assertTrue("Out-of-sync parent versions found (would break the reactor build):\n"
                + String.join("\n", mismatches), mismatches.isEmpty());
    }

    /**
     * Walks up from the current working directory (the module basedir when run
     * by surefire) until it finds the directory containing the root pom.xml.
     */
    private Path findProjectRoot() throws Exception {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            Path pom = current.resolve("pom.xml");
            if (Files.exists(pom) && isRootPom(pom)) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate root pom.xml (artifactId=" + ROOT_ARTIFACT_ID + ")");
    }

    private boolean isRootPom(Path pom) throws Exception {
        Document doc = parseXml(pom);
        return ROOT_ARTIFACT_ID.equals(childElementText(doc.getDocumentElement(), "artifactId"));
    }

    private String readRootRevision(Path root) throws Exception {
        Document doc = parseXml(root.resolve("pom.xml"));
        Element properties = firstChildElement(doc.getDocumentElement(), "properties");
        assertTrue("Root pom.xml must define <properties><revision>...</revision></properties>",
                properties != null);
        String revision = childElementText(properties, "revision");
        assertTrue("Root pom.xml <revision> must not be empty", revision != null && !revision.isEmpty());
        return revision;
    }

    private List<Path> collectPomFiles(Path root) throws Exception {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals("pom.xml"))
                    .filter(p -> !underDirectory(p, "target"))
                    .collect(Collectors.toList());
        }
    }

    private boolean underDirectory(Path path, String dirName) {
        for (Path part : path) {
            if (dirName.equals(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private Document parseXml(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(file.toFile());
    }

    private String childElementText(Element parent, String childName) {
        Element child = firstChildElement(parent, childName);
        return child == null ? null : child.getTextContent().trim();
    }

    private Element firstChildElement(Element parent, String childName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element child = (Element) children.item(i);
                if (childName.equals(child.getLocalName())) {
                    return child;
                }
            }
        }
        return null;
    }

    private String relativize(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }
}
