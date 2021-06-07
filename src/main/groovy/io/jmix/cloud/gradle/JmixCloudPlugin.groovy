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

package io.jmix.cloud.gradle

import io.jmix.cloud.gradle.dsl.DockerExtension
import io.jmix.cloud.gradle.tasks.CloudClean
import io.jmix.cloud.gradle.tasks.CloudRun
import io.jmix.cloud.gradle.tasks.docker.DockerBuild
import io.jmix.cloud.gradle.tasks.docker.DockerPush
import org.gradle.api.Plugin
import org.gradle.api.Project

class JmixCloudPlugin implements Plugin<Project> {

    private static final String CLOUD_RUN_TASK = 'cloudRun'
    private static final String CLOUD_CLEAN_TASK = 'cloudClean'
    private static final String DOCKER_BUILD = 'dockerBuild'
    private static final String DOCKER_PUSH = 'dockerPush'

    @Override
    void apply(Project project) {
        project.extensions.create('docker', DockerExtension, project)
//        project.docker.extensions.create('registries', DockerExtension.DockerRegistry, project)

        project.task([type: CloudRun], CLOUD_RUN_TASK)
        project.task([type: CloudClean], CLOUD_CLEAN_TASK)
        project.task([type: DockerBuild], DOCKER_BUILD)
        project.task([type: DockerPush], DOCKER_PUSH)

        project.afterEvaluate {
            project.tasks.findByName('clean').dependsOn(CLOUD_CLEAN_TASK)
        }
    }
}
