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
import io.jmix.cloud.gradle.dsl.DockerExtension
import io.jmix.cloud.gradle.utils.docker.DockerUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

class DockerPush extends DefaultTask {

    private String sourceTag
    private String targetTag

    DockerPush() {
        setGroup("docker")
        setDescription("Pushes Docker image to remote repositories")
    }

    @Input
    String getSourceTag() {
        return sourceTag
    }

    @Option(option = "sourceTag", description = "Docker image name to push")
    void setSourceTag(String sourceTag) {
        this.sourceTag = sourceTag
    }

    @Input
    @Optional
    String getTargetTag() {
        return targetTag
    }

    @Option(option = "targetTag", description = "Docker image name in registries")
    void setTargetTag(String targetTag) {
        this.targetTag = targetTag
    }

    @TaskAction
    push() {
        String tag = targetTag ?: sourceTag
        try (DockerClient client = DockerUtils.clientLocal()) {
            project.docker.registries.each { registry ->
                client.pushImageCmd("${registry.address}/${tag}")
                    .withAuthConfig(client.authConfig()
                            .withRegistryAddress(registry.address)
                            .withEmail(registry.email)
                            .withUsername(registry.username)
                            .withPassword(registry.password))
            }
        }
    }
}
