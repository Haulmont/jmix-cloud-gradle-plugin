/*
 * Copyright 2021 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.cloud.gradle.utils.docker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public final class DockerHostDeterminantForWindows {

    private static final String DOCKER_HOST = "DOCKER_HOST";
    private static final String WINDOWS_DEFAULT_DOCKER_HOST = "tcp://localhost:2375";
    private static final String DOCKER_JAVA_PROPERTIES = "docker-java.properties";
    private static final String USER_HOME = "user.home";


    private DockerHostDeterminantForWindows() {
    }

    public static String defineDockerHost() {
        return defineDockerHost(System.getenv(), (Properties) System.getProperties().clone());
    }

    private static String defineDockerHost(Map<String, String> env, Properties systemProperties) {
        Properties defaultProperties = new Properties();
        defaultProperties.put(DOCKER_HOST, WINDOWS_DEFAULT_DOCKER_HOST);

        Properties properties = loadIncludedDockerProperties(systemProperties, defaultProperties);
        properties = overrideDockerPropertiesWithSettingsFromUserHome(properties, systemProperties);
        properties = overrideDockerPropertiesWithEnv(properties, env);
        properties = overrideDockerPropertiesWithSystemProperties(properties, systemProperties);
        return properties.getProperty(DOCKER_HOST);
    }

    private static Properties loadIncludedDockerProperties(Properties systemProperties, Properties defaultProperties) {
        Properties p = new Properties();
        p.putAll(defaultProperties);
        try (InputStream is = DockerHostDeterminantForWindows.class.getResourceAsStream("/" + DOCKER_JAVA_PROPERTIES)) {
            if (is != null) {
                p.load(is);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        replaceProperties(p, systemProperties);
        return p;
    }

    private static void replaceProperties(Properties properties, Properties replacements) {
        for (Object objectKey : properties.keySet()) {
            String key = objectKey.toString();
            properties.setProperty(key, replaceProperties(properties.getProperty(key), replacements));
        }
    }

    private static String replaceProperties(String s, Properties replacements) {
        for (Map.Entry<Object, Object> entry : replacements.entrySet()) {
            String key = "${" + entry.getKey() + "}";
            while (s.contains(key)) {
                s = s.replace(key, String.valueOf(entry.getValue()));
            }
        }
        return s;
    }

    private static Properties overrideDockerPropertiesWithSettingsFromUserHome(Properties p, Properties systemProperties) {
        Properties overriddenProperties = new Properties();
        overriddenProperties.putAll(p);

        final File usersDockerPropertiesFile = new File(systemProperties.getProperty(USER_HOME),
                "." + DOCKER_JAVA_PROPERTIES);
        if (usersDockerPropertiesFile.isFile()) {
            try (FileInputStream in = new FileInputStream(usersDockerPropertiesFile)) {
                overriddenProperties.load(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return overriddenProperties;
    }

    private static Properties overrideDockerPropertiesWithEnv(Properties properties, Map<String, String> env) {
        Properties overriddenProperties = new Properties();
        overriddenProperties.putAll(properties);

        if (env.containsKey(DOCKER_HOST)) {
            String value = env.get(DOCKER_HOST);
            if (value != null && value.trim().length() != 0) {
                overriddenProperties.setProperty(DOCKER_HOST, value);
            }
        }

        return overriddenProperties;
    }

    private static Properties overrideDockerPropertiesWithSystemProperties(Properties p, Properties systemProperties) {
        Properties overriddenProperties = new Properties();
        overriddenProperties.putAll(p);

        if (systemProperties.containsKey(DOCKER_HOST)) {
            overriddenProperties.setProperty(DOCKER_HOST, systemProperties.getProperty(DOCKER_HOST));
        }

        return overriddenProperties;
    }

}
