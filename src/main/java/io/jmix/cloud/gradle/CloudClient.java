package io.jmix.cloud.gradle;

import io.jmix.cloud.gradle.ssh.SshSession;

public interface CloudClient {
    void createResources();
    void destroyResources();
    InstanceState state();
    SshSession ssh();
}
