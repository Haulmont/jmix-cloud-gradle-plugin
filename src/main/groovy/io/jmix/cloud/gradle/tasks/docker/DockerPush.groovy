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
import com.github.dockerjava.api.async.ResultCallback
import io.jmix.cloud.gradle.JmixCloudPlugin
import io.jmix.cloud.gradle.dsl.DockerExtension
import io.jmix.cloud.gradle.utils.docker.DockerUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class DockerPush extends DefaultTask {

    protected DockerExtension extension

    DockerPush() {
        setGroup("docker")
        setDescription("Pushes Docker image to remote repositories")
    }

    @TaskAction
    push() {
        extension = project.extensions.findByName(JmixCloudPlugin.EXTENSION_DOCKER_NAME) as DockerExtension
        try (DockerClient client = DockerUtils.clientLocal()) {
            extension.getRegistries().forEach(registry -> {
                String fullName = registry.getFullImageName()
                client.pushImageCmd(fullName)
                        .withAuthConfig(client.authConfig()
                                .withRegistryAddress(registry.getAddress())
                                .withEmail(registry.getEmail())
                                .withUsername(registry.getUsername())
                                .withPassword(registry.getPassword()))
                        .exec(new ResultCallback.Adapter())
                        .awaitCompletion()
                logger.lifecycle("Pushed docker image with name: {}", fullName)
            }
            )
        }
    }

}
