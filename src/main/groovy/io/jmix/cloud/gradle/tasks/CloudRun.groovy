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

package io.jmix.cloud.gradle.tasks

import com.fasterxml.jackson.databind.ObjectMapper
import io.jmix.cloud.gradle.CloudClient
import io.jmix.cloud.gradle.CloudClientFactory
import io.jmix.cloud.gradle.InstanceState
import io.jmix.cloud.gradle.utils.docker.DockerUtils
import io.jmix.cloud.gradle.utils.ssh.SshSession
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import java.util.zip.GZIPOutputStream

class CloudRun extends DefaultTask {

    private static final String PING_TYPE_REQUEST = "HEAD";
    private static final int PING_TIMEOUT = 300000;
    private static final int PING_WAITING_INTERVAL = 10000;

    private ObjectMapper objectMapper
    private String provider = 'aws'

    @Input
    String getProvider() {
        return provider
    }

    @Option(option = 'provider', description = 'Configures cloud provider where application should be deployed.')
    void setProvider(String provider) {
        this.provider = provider
    }

    CloudRun() {
        dependsOn("bootBuildImage")
        setGroup("application")
        setDescription("Runs Jmix project in cloud environment")

        objectMapper = new ObjectMapper()

        project.afterEvaluate {
            project.tasks.bootBuildImage.imageName = project.tasks.bootBuildImage.imageName ?: project.name
        }
    }

    @TaskAction
    void run() {
        Directory outDir = project.layout.buildDirectory.dir("tmp/jmixCloudRun").get()
        if (!outDir.asFile.exists()) {
            outDir.asFile.mkdirs()
        }

        File file = outDir.file('compute-instance.json').asFile
        InstanceState state = null
        boolean createInstance = true

        if (file.exists()) {
            state = objectMapper.readValue(file, InstanceState)

            try (SshSession ssh = SshSession.forInstance(state)) {
                ssh.execute("cd app && docker-compose kill")
            }
        }

        if (createInstance) {
            logger.lifecycle("Creating instance using $provider provider")
            CloudClient client = CloudClientFactory.create(provider, this, outDir.asFile.path)
            client.createResources()
            state = client.state()
            objectMapper.writeValue(file, state)
            logger.lifecycle("Successfully created instance $state.host")
        } else {
            logger.lifecycle("Using existing instance $state.host")
        }

        String imageName = project.tasks.bootBuildImage.imageName ?: project.name
        String imageArchiveName = "${imageName.replaceAll("[/:]", "-")}.tar.gz"
        File imageArchiveFile = outDir.file(imageArchiveName).asFile
        logger.lifecycle("Saving Docker image to file $imageArchiveName")
        try (InputStream dockerImageStream = DockerUtils.saveImage(DockerUtils.clientLocal(), imageName)) {
            gzip(dockerImageStream, imageArchiveFile)
        }
        logger.lifecycle("Successfully saved Docker image ")

        runDockerCompose(state, imageArchiveFile)

        boolean isReachable = ping("http://$state.host:8080")

        if (isReachable) {
            logger.quiet("Application is running on http://$state.host:8080")
        } else {
            logger.error("Application is started on http://$state.host:8080, but it cannot be reached")
        }
    }

    private boolean ping(String path) {
        logger.debug("ping called for path: $path")
        int retriesLeft = 30
        while (retriesLeft-- > 0) {
            try {
                HttpURLConnection httpUrlConnection = (HttpURLConnection) new URL(path).openConnection()
                httpUrlConnection.setConnectTimeout(PING_TIMEOUT)
                httpUrlConnection.setReadTimeout(PING_TIMEOUT)
                httpUrlConnection.setRequestMethod(PING_TYPE_REQUEST)
                int code = httpUrlConnection.getResponseCode()
                if (code == 200 || code == 302) {
                    return true
                }
            } catch (IOException | InterruptedException e) {
                logger.debug("Host with path $path is unrecheable")
            }
            Thread.sleep(PING_WAITING_INTERVAL)
        }
        return false
    }

    private void runDockerCompose(InstanceState instance, File imageFile) {
        try (SshSession ssh = SshSession.forInstance(instance)) {
            String imageName = project.tasks.bootBuildImage.imageName ?: project.name

            ssh.execute("mkdir app")

            logger.lifecycle("Uploading image file")
            ssh.scpUploadFile(imageFile, "app/$imageFile.name")
            logger.lifecycle("Uploaded image file")

            logger.lifecycle("Uploading docker-compose file")
            ssh.scpUploadFile(project.file("docker-compose.yml"), "app/docker-compose.yml")
            logger.lifecycle("Successfully uploaded docker-compose file")

            logger.lifecycle("Loading image $imageName from file $imageFile.name")
            ssh.execute("cd app && gunzip -c $imageFile.name | docker load")
            ssh.execute("docker tag $imageName ${project.name}")
            logger.lifecycle("Successfully loaded Docker image")

            logger.lifecycle("Starting application")
            ssh.execute("cd app && docker-compose up -d")
        }
    }

    static void gzip(InputStream inputStream, File file) {
        try (FileOutputStream fOut = new FileOutputStream(file)
             GZIPOutputStream gzipOut = new GZIPOutputStream(fOut)) {
            byte[] buf = new byte[16 * 1024]
            int length
            while ((length = inputStream.read(buf)) > 0) {
                gzipOut.write(buf, 0, length)
            }
        }
    }
}
