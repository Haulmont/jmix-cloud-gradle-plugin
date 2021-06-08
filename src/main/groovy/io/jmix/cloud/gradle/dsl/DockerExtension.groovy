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

package io.jmix.cloud.gradle.dsl

import org.gradle.api.Project

class DockerExtension {
    private Project project
    private String imageName = null
    private String tag = "latest"

    List<DockerRegistry> registries = []

    DockerExtension(Project project) {
        this.project = project
    }

    String getImageName() {
        return imageName ?: project.name
    }

    void setImageName(String imageName) {
        this.imageName = imageName
    }

    String getTag() {
        return tag
    }

    void setTag(String tag) {
        this.tag = tag
    }

    List<DockerRegistry> getRegistries() {
        return registries
    }

    void setRegistries(List<DockerRegistry> registries) {
        this.registries = registries
    }

    void registry(String address, Closure closure) {
        DockerRegistry registry = new DockerRegistry(address)
        registries << registry
        project.configure(registry, closure)
    }

    class DockerRegistry {
        private String address = ""
        private String username = ""
        private String password = ""
        private String email = ""
        private String targetName = ""

        DockerRegistry(String address) {
            this.address = address
        }

        String getAddress() {
            return address
        }

        String getUsername() {
            return username
        }

        void setUsername(String username) {
            this.username = username
        }

        String getPassword() {
            return password
        }

        void setPassword(String password) {
            this.password = password
        }

        String getEmail() {
            return email
        }

        void setEmail(String email) {
            this.email = email
        }

        String getTargetName() {
            return targetName
        }

        void setTargetName(String targetName) {
            this.targetName = targetName
        }

        @Override
        public String toString() {
            return "DockerRegistry{" +
                    "address='" + address + '\'' +
                    ", username='" + username + '\'' +
                    ", password='" + password + '\'' +
                    ", email='" + email + '\'' +
                    ", targetName='" + targetName + '\'' +
                    '}';
        }
    }

}
