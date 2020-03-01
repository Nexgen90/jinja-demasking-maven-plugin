package ru.nexgen.maven.demasking;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * Для чтения главного конфига
 * и всех остальных из этой же папки
 */
public class YamlReader {
    private Path mainYaml;
    private Path filesPath;

    private YamlReader() {
    }

    public YamlReader(String mainYamlPath) {
        mainYaml = Paths.get(mainYamlPath);
        filesPath = mainYaml.getParent();
    }

    private Map<String, Object> read(Path path) throws IOException {
        InputStream in = Files.newInputStream(path);
        Yaml yaml = new Yaml();
        return (Map<String, Object>) yaml.load(in);
    }


    public Map<String, Object> readMainYaml() throws IOException {
        if (mainYaml.getFileName().toString().endsWith(".yml")) {
            return read(mainYaml);
        } else {
            throw new IOException("Must be .yml file");
        }
    }

    public List<Map<String, Object>> readOthersYaml() throws IOException {
        //получаем список всех yml файлов в папке
        List<File> listOfFiles = new LinkedList<>(asList(filesPath.toFile().listFiles()));

        //удаляем из списка с файлами основной конфиг
        listOfFiles.removeIf(file -> file.equals(mainYaml.toFile()));

        //читаем оставшиеся конфиги
        List<Map<String, Object>> othersYaml = new ArrayList<>();
        for (File file : listOfFiles){
            othersYaml.add(read(Paths.get(file.getPath())));
        }
        return othersYaml;
    }

    public Path getConfigsDir(){
        return filesPath;
    }
}
