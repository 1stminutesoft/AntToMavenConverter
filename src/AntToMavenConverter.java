import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class AntToMavenConverter extends JFrame {

    private JTextField projectPathField;
    private JTextArea logArea;

    public AntToMavenConverter() {
        setTitle("NetBeans Ant to Maven Converter");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 600);
        setLocationRelativeTo(null);
        initComponents();
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JLabel welcome = new JLabel("Welcome! Select a NetBeans Ant project to convert it to Maven format.", SwingConstants.CENTER);
        welcome.setFont(new Font("SansSerif", Font.BOLD, 13));
        welcome.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));

        JLabel instructionLabel = new JLabel("<html>"
                + "<b>Step 1:</b> Click 'Select Project Folder'.<br>"
                + "<b>Step 2:</b> Click 'Convert to Maven' to generate the project.<br>"
                + "<b>Step 3:</b> Open the Mavenized project in NetBeans."
                + "</html>");
        instructionLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.add(welcome);
        topSection.add(instructionLabel);

        JPanel pathPanel = new JPanel(new BorderLayout(5, 5));
        projectPathField = new JTextField();
        JButton browseButton = new JButton("Select Project Folder");
        browseButton.setPreferredSize(new Dimension(160, 30));
        browseButton.addActionListener(this::onBrowse);
        pathPanel.add(projectPathField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);

        JButton convertButton = new JButton("Convert to Maven");
        convertButton.setPreferredSize(new Dimension(160, 30));
        convertButton.addActionListener(this::onConvert);
        JPanel convertPanel = new JPanel();
        convertPanel.add(convertButton);

        logArea = new JTextArea(25, 65);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(640, 350));

        JLabel footer = new JLabel("Universiti Malaysia Terengganu 2025", SwingConstants.CENTER);
        footer.setFont(new Font("Serif", Font.ITALIC, 12));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(pathPanel);
        centerPanel.add(Box.createVerticalStrut(5));
        centerPanel.add(convertPanel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(scrollPane);

        panel.add(topSection, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);

        add(panel);
    }

    private void onBrowse(ActionEvent event) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            projectPathField.setText(selectedDir.getAbsolutePath());
        }
    }

    private void onConvert(ActionEvent event) {
        String projectPath = projectPathField.getText().trim();
        if (projectPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a project folder.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !new File(projectDir, "nbproject/project.xml").exists()) {
            JOptionPane.showMessageDialog(this, "Invalid NetBeans Ant project folder.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        log("Project path: " + projectPath);

        try {
            File parentDir = projectDir.getParentFile();
            File mavenStructure = new File(parentDir, projectDir.getName() + "-mavenized");
            Files.createDirectories(mavenStructure.toPath());

            File srcJava = new File(mavenStructure, "src/main/java");
            File srcWeb = new File(mavenStructure, "src/main/webapp");
            Files.createDirectories(srcJava.toPath());
            Files.createDirectories(srcWeb.toPath());

            Path src = new File(projectDir, "src").toPath();
            if (src.toFile().exists()) {
                copyFolder(src, srcJava.toPath());
                log("Copied Java source to src/main/java");
            }

            Path web = new File(projectDir, "web").toPath();
            if (web.toFile().exists()) {
                copyFolder(web, srcWeb.toPath());
                log("Copied Web resources to src/main/webapp");

                File webXml = new File(projectDir, "web/WEB-INF/web.xml");
                if (webXml.exists()) {
                    File webInfTarget = new File(srcWeb, "WEB-INF");
                    Files.createDirectories(webInfTarget.toPath());
                    Files.copy(webXml.toPath(), new File(webInfTarget, "web.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);
                    log("Copied web.xml to src/main/webapp/WEB-INF/");
                }
            }

            File libDir = new File(projectDir, "lib");
            List<File> jars = List.of();
            if (libDir.exists() && libDir.isDirectory()) {
                jars = Files.list(libDir.toPath())
                        .filter(p -> p.toString().endsWith(".jar"))
                        .map(Path::toFile)
                        .collect(Collectors.toList());
                log("Detected jars: " + jars.stream().map(File::getName).collect(Collectors.joining(", ")));

                File targetLib = new File(mavenStructure, "lib");
                Files.createDirectories(targetLib.toPath());
                copyFolder(libDir.toPath(), targetLib.toPath());
                log("Copied lib folder to Maven project");
            }

            File pom = new File(mavenStructure, "pom.xml");
            writePomXml(pom, jars);
            log("Generated pom.xml");

            log("Conversion completed at: " + mavenStructure.getAbsolutePath());

        } catch (IOException e) {
            log("Error during conversion: " + e.getMessage());
        }
    }

    private void copyFolder(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
            try {
                Path dest = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                log("Failed to copy: " + src);
            }
        });
    }

    private void writePomXml(File pomFile, List<File> jarFiles) throws IOException {
        StringBuilder deps = new StringBuilder();
        deps.append("\n        <dependency>\n")
                .append("            <groupId>javax.servlet</groupId>\n")
                .append("            <artifactId>jstl</artifactId>\n")
                .append("            <version>1.2</version>\n")
                .append("        </dependency>\n")
                .append("        <dependency>\n")
                .append("            <groupId>javax.servlet</groupId>\n")
                .append("            <artifactId>javax.servlet-api</artifactId>\n")
                .append("            <version>4.0.1</version>\n")
                .append("            <scope>provided</scope>\n")
                .append("        </dependency>\n")
                .append("        <dependency>\n")
                .append("            <groupId>mysql</groupId>\n")
                .append("            <artifactId>mysql-connector-java</artifactId>\n")
                .append("            <version>8.0.33</version>\n")
                .append("        </dependency>");

        for (File jar : jarFiles) {
            String name = jar.getName().toLowerCase();
            if (!(name.contains("servlet") || name.contains("jstl") || name.contains("mysql"))) {
                deps.append("\n        <!-- TODO: Manually add mapping for ").append(jar.getName()).append(" -->");
            }
        }

        String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <groupId>com.example</groupId>\n"
                + "    <artifactId>converted-project</artifactId>\n"
                + "    <version>1.0-SNAPSHOT</version>\n"
                + "    <packaging>war</packaging>\n"
                + "    <dependencies>" + deps.toString() + "\n    </dependencies>\n"
                + "    <build>\n"
                + "        <plugins>\n"
                + "            <plugin>\n"
                + "                <groupId>org.apache.maven.plugins</groupId>\n"
                + "                <artifactId>maven-compiler-plugin</artifactId>\n"
                + "                <version>3.11.0</version>\n"
                + "                <configuration>\n"
                + "                     <release>11</release>\n"
                + "                </configuration>\n"
                + "            </plugin>\n"
                + "            <plugin>\n"
                + "                <groupId>org.apache.maven.plugins</groupId>\n"
                + "                <artifactId>maven-war-plugin</artifactId>\n"
                + "                <version>3.3.2</version>\n"
                + "                <configuration>\n"
                + "                    <failOnMissingWebXml>false</failOnMissingWebXml>\n"
                + "                </configuration>\n"
                + "            </plugin>\n"
                + "        </plugins>\n"
                + "    </build>\n"
                + "</project>\n";

        Files.writeString(pomFile.toPath(), pom);
    }

    private void log(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AntToMavenConverter().setVisible(true));
    }
}
