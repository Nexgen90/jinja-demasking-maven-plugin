#jinja-demasking-maven-plugin
If you use [ansible](https://docs.ansible.com) closely ties with java, you can see in your project files something like this:
```
logging.file.max-size={{demo.logging.file.size}}
logging.file.max-history={{demo.logging.file.history}}
```
**jinja-demasking-maven-plugin** will help you to handle it.

I deal a lot with projects which are deploy by Ansible with big separated yaml configuration files. 
This files are the centralised place for application properties.
* Actually you have yaml-config file, which [ansible](https://docs.ansible.com) use in deploy phase:
```yaml
---
demo:
  logging:
    file:
      size: '10MB'
```
And java properties modified with specific [jinja2](https://jinja.palletsprojects.com/en/2.11.x/) patterns:
* application.properties
```properties
logging.file.max-size={{demo.logging.file.size}}
logging.file.max-history={{demo.logging.file.history}}
```

So you can use jinja-demasking-maven-plugin for useful demasking your properties (or other files).

## How to use:

* Add plugin in new profile:
```xml
...
<project>
    ...
    <profiles>
        <profile>
            <id>demasking</id>
            <build>
                <plugins>

                    <plugin>
                        <groupId>ru.nexgen.maven</groupId>
                        <artifactId>jinja-demasking-maven-plugin</artifactId>
                        <version>1.0</version>
                        <configuration>
                            <yamlFilePath>
                                C:\Users\Nikolay.Mikutskiy\git\demo\app-install\inventory\localhost\group_vars\demo.yml
                            </yamlFilePath>
                            <templateFilePaths>
                                <Path>${basedir}\samples\application.properties</Path>
                                <Path>${basedir}\samples\bootstrap.properties</Path>
                                <path>${basedir}\samples\samples.json</path>
                                <path>${basedir}\samples\db.sql</path>
                            </templateFilePaths>
                        </configuration>
                        <executions>
                            <execution>
                                <id>demasking</id>
                                <goals>
                                    <goal>demasking</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>

                </plugins>
            </build>
        </profile>
    </profiles>
    ...
</project>
```
* Or like this:
```xml
...
<project>
    ...
    <profiles>
        <profile>
            <id>demasking</id>
            <build>
                <plugins>

                    <plugin>
                        <groupId>ru.nexgen.maven</groupId>
                        <artifactId>jinja-demasking-maven-plugin</artifactId>
                        <version>1.0</version>
                        <configuration>
                            <yamlFilePath>
                                C:\Users\Nikolay.Mikutskiy\git\demo\app-install\inventory\localhost\group_vars\demo.yml
                            </yamlFilePath>
                             <keyForDemaskingInFile>
                               <keyForDemaskingInFile>(.*)#demask_me(.*)</keyForDemaskingInFile>
                               <keyForDemaskingInFile>--demask_me</keyForDemaskingInFile>
                             </keyForDemaskingInFile>
                             <excludeDirs>
                               <dir>target</dir>
                               <dir>utils</dir>
                             </excludeDirs>
                        </configuration>
                        <executions>
                            <execution>
                                <id>demasking</id>
                                <goals>
                                    <goal>demasking</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>

                </plugins>
            </build>
        </profile>
    </profiles>
    ...
</project>
```

###Steps to configure:
0. Add plugin in you pom.xml
1. In `<configuration>` section set:
    - `<yamlFilePath>` - main yaml configuration file. It will be merged with other yaml configuration files in the same folder;
2. Specify which files should be demasked:
    - `<templateFilePaths>` - your can specify each file to demasking;
    or
    - `<templateFileDirs>` - specify directory with this files;
    or
    - `<keyForDemaskingInFile>` - specify your custom key to mark file (actually in comments) and plugin will find this files;
3. Specify `<excludeDirs>` if some files doesn't need to demask. By default `target` directory is excluded;
*You can use linux style or windows style for path. Plugin handle it well;*

4. Run it:
```
mvn clean compile -Pdemasking
```

Now all `{{..}}` patterns demasked:
```properties
logging.file.max-size=10MB
logging.file.max-history=7
```
