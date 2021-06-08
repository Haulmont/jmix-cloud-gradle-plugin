package io.jmix.cloud.gradle;

import io.jmix.cloud.gradle.utils.ssh.SshSession;

public interface CloudClient {
    void createResources();
    void destroyResources();
    InstanceState state();
    SshSession ssh();
}
