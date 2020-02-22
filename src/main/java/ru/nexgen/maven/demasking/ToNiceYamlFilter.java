package ru.nexgen.maven.demasking;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlWriter;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.filter.Filter;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.removePattern;

public class ToNiceYamlFilter implements Filter {
    private Map<String, Map<String, String>> values;

    @Override
    public String getName() {
        return "to_nice_yaml";
    }

    @Override
    public Object filter(Object o, JinjavaInterpreter jinjavaInterpreter, String... args) {
        Writer stringWriter = new StringWriter();
        YamlWriter writer = new YamlWriter(stringWriter);

        try {
            writer.write(o);
        } catch (YamlException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (YamlException e) {
                e.printStackTrace();
            }
        }
        return removePattern(stringWriter.toString(), "![^\\s]*");
    }
}
