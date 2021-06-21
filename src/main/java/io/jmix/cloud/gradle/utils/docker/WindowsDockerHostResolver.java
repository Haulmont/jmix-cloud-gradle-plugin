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

public final class WindowsDockerHostResolver {

    private static final String DOCKER_HOST = "DOCKER_HOST";
    private static final String WINDOWS_DEFAULT_DOCKER_HOST = "npipe:////./pipe/docker_engine";
    private static final String DOCKER_JAVA_PROPERTIES = "docker-java.properties";
    private static final String USER_HOME = "user.home";


    private WindowsDockerHostResolver() {
    }

    public static String resolveDockerHost() {
        return resolveDockerHost(System.getenv(), (Properties) System.getProperties().clone());
    }

    private static String resolveDockerHost(Map<String, String> env, Properties systemProperties) {
        String dockerHost = getDockerHostFromSettingsFromSystemProperties(systemProperties);

        if(dockerHost == null) dockerHost = getDockerHostFromEnvironmentVariables(env);
        if(dockerHost == null) dockerHost = getDockerHostFromSettingsFromUserHome(systemProperties);
        if(dockerHost == null) dockerHost = getDockerHostFromIncludedDockerProperties();
        if(dockerHost == null) dockerHost = WINDOWS_DEFAULT_DOCKER_HOST;

        return dockerHost;
    }

    private static String getDockerHostFromIncludedDockerProperties() {
        Properties p = new Properties();
        try (InputStream is = WindowsDockerHostResolver.class.getResourceAsStream("/" + DOCKER_JAVA_PROPERTIES)) {
            if (is != null) {
                p.load(is);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return p.getProperty(DOCKER_HOST);
    }

    private static String getDockerHostFromSettingsFromUserHome(Properties systemProperties) {
        Properties p = new Properties();
        final File usersDockerPropertiesFile = new File(systemProperties.getProperty(USER_HOME),
                "." + DOCKER_JAVA_PROPERTIES);
        if (usersDockerPropertiesFile.isFile()) {
            try (FileInputStream in = new FileInputStream(usersDockerPropertiesFile)) {
                p.load(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return p.getProperty(DOCKER_HOST);
    }

    private static String getDockerHostFromEnvironmentVariables(Map<String, String> env) {
        return env.getOrDefault(DOCKER_HOST, null);
    }

    private static String getDockerHostFromSettingsFromSystemProperties(Properties systemProperties) {
        return systemProperties.containsKey(DOCKER_HOST) ? systemProperties.getProperty(DOCKER_HOST) : null;
    }

}
