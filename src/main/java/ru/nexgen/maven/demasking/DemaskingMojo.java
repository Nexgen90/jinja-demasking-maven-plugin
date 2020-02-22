package ru.nexgen.maven.demasking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.jinjava.Jinjava;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author nexgen (Nikolay Mikutskiy)
 */
@Mojo(name = "demasking", defaultPhase = LifecyclePhase.VALIDATE)
public class DemaskingMojo extends AbstractMojo {

    private static final String ENCODING = "UTF-8";
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<JsonNode> othersConf = new LinkedList<>();
    private final YamlMerger merger = new YamlMerger();

    @Parameter(defaultValue = "${basedir}", readonly = true)
    private String baseDirectory;

    @Parameter(property = "excludeDirs")
    private List<String> excludeDirs;

    @Parameter(property = "yamlFilePath")
    private String yamlFilePath;

    @Parameter(property = "keyForDemaskingInFile")
    private List<String> keyForDemaskingInFile;

    @Parameter(property = "templateFilePaths")
    private List<String> templateFilePaths;

    @Parameter(property = "templateFileDirs")
    private List<String> templateFileDirs;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Start config demasking-plugin");
        if (excludeDirs.isEmpty()) {
            excludeDirs = Collections.singletonList("target");
        }

        if (yamlFilePath != null && !yamlFilePath.isEmpty()) {
            // При необходимости замена символа разделителя директорий в yamlFilePath
            getLog().info("replace delimeter in yaml path");
            getLog().info("yaml path before replace separetor: " + yamlFilePath);
            yamlFilePath = yamlFilePath.replaceAll("[\\\\/]", Matcher.quoteReplacement(File.separator));
            getLog().info("yaml path after replace separator: " + yamlFilePath);
            getMaskedFiles();
            templateFilePaths = templateFilePaths.stream().map(path -> path.replaceAll("[\\\\/]", Matcher.quoteReplacement(File.separator))).collect(Collectors.toList());
            try {
                // Load the parameters
                YamlReader yamlReader = new YamlReader(yamlFilePath);
                Map<String, Object> mainYaml = yamlReader.readMainYaml();
                List<Map<String, Object>> othersYaml = yamlReader.readOthersYaml();

                // Merge yaml configs
                getLog().info("Start merging yaml configs");
                JsonNode mainConf = mapper.valueToTree(mainYaml);

                for (Map<String, Object> oneYaml : othersYaml) {
                    othersConf.add(mapper.valueToTree(oneYaml));
                }

                JsonNode resultConf = merger.merge(mainConf, mainConf);

                for (JsonNode oneNode : othersConf) {
                    resultConf = merger.merge(resultConf, oneNode);
                }

                Map<String, Object> finalYaml = mapper.convertValue(resultConf, Map.class);
                getLog().debug("finalYaml:\n" + finalYaml);


                // Load template
                getLog().info("Load template:");
                List<String> templates = new LinkedList<>();
                for (String template : templateFilePaths) {
                    templates.add(FileUtils.readFileToString(new File(template), ENCODING));
                    getLog().info(template);
                }

                // Render and save
                getLog().info("Render and save:");
                Jinjava jinjava = new Jinjava();
                jinjava.getGlobalContext().registerFilter(new ToNiceYamlFilter());

                for (int i = 0; i < templateFilePaths.size(); i++) {
                    String rendered = jinjava.render(templates.get(i), finalYaml);
                    FileUtils.writeStringToFile(new File(templateFilePaths.get(i)), rendered, ENCODING);
                    getLog().info(templateFilePaths.get(i));
                    getLog().debug(rendered);
                }

            } catch (IOException e) {
                // Print error and exit with -1
                throw new MojoExecutionException(e.getLocalizedMessage(), e);
            }
        } else {
            getLog().info("Source yaml not found");
        }
    }


    private void getMaskedFiles() {
        if (!keyForDemaskingInFile.isEmpty()) {
            String demaskingList = keyForDemaskingInFile.stream().map(v -> v.concat("|")).collect(Collectors.joining());
            String regex = demaskingList.substring(0, demaskingList.length() - 1);
            List<File> files = new ArrayList<>();
            getLog().info("Base directory: " + baseDirectory);
            if (templateFileDirs != null && !templateFileDirs.isEmpty()) {
                getLog().info("Custom directories: " + templateFileDirs.stream().collect(Collectors.joining("; ")));
                templateFileDirs.forEach((directory) -> getAllFilesFromDirectory(directory, files));
            } else {
                getAllFilesFromDirectory(baseDirectory, files);
            }
            files.forEach(file -> {
                if (isDemasking(file, regex)) {
                    templateFilePaths.add(file.getAbsolutePath());
                }
            });
        } else {
            getLog().info("Property keyForDemaskingInFile is empty");
        }

    }

    private Boolean isDemasking(File file, String demaskingRegexp) {
        boolean result = false;
        Scanner in = null;
        try {
            in = new Scanner(file);
            while (in.hasNextLine() && !result) {
                result = in.nextLine().matches(demaskingRegexp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) { /* ignore */ }
        }
        return result;
    }

    private void getAllFilesFromDirectory(String directory, List<File> fileList) {
        if (directory != null && (!directory.isEmpty())) {
            Stream.of(new File(directory).listFiles()).filter(file -> !excludeDirs.contains(file.getName())).forEach(file -> {
                if (file.isDirectory()) {
                    getAllFilesFromDirectory(file.getAbsolutePath(), fileList);
                } else {
                    fileList.add(file);
                }
            });
        }

    }
}
