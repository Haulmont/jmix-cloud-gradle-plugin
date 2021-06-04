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

package io.jmix.cloud.gradle.tasks.docker

import com.github.dockerjava.api.DockerClient
import io.jmix.cloud.gradle.utils.docker.DockerUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

class DockerBuild extends DefaultTask {

    private String imageName = null
    private String imageTag = "latest"
    private boolean removeContainers = true
    private boolean forceRemoveContainers = false

    DockerBuild() {
        setGroup("docker")
        setDescription("Builds Docker image")
    }

    @Input
    @Optional
    String getImageName() {
        return this.imageName
    }

    @Option(option = "imageName", description = "Docker image name")
    void setImageName(String imageName) {
        this.imageName = imageName
    }

    @Input
    @Optional
    String getImageTag() {
        return this.imageTag
    }

    @Option(option = "imageTag", description = "Docker image tag")
    void setImageTag(String imageTag) {
        this.imageTag = imageTag
    }

    @Input
    @Optional
    boolean isRemoveContainers() {
        return removeContainers
    }

    @Option(option = "removeContainers", description = "Remove intermediate containers after a successful build")
    void setRemoveContainers(boolean removeContainers) {
        this.removeContainers = removeContainers
    }

    @Input
    @Optional
    boolean isForceRemoveContainers() {
        return forceRemoveContainers
    }

    @Option(option = "forceRemoveContainers", description = "Always remove intermediate containers")
    void setForceRemoveContainers(boolean forceRemoveContainers) {
        this.forceRemoveContainers = forceRemoveContainers
    }

    @TaskAction
    void buildImage() {
        String tag = calculateImageName()
        logger.lifecycle("Building Docker image {}", tag)
        try (DockerClient client = DockerUtils.clientLocal()) {
            String imageId = client.buildImageCmd(project.file("Dockerfile"))
                    .withTags([tag] as Set<String>)
                    .withRemove(removeContainers)
                    .withForcerm(forceRemoveContainers)
                    .start()
                    .awaitImageId()
            logger.lifecycle("Build Docker image {} (id = {})", tag, imageId)
        }
    }

    private String calculateImageName() {
        def name = imageName ?: project.name
        return name.contains(':') ? name : "${name}:${imageTag}"
    }
}
