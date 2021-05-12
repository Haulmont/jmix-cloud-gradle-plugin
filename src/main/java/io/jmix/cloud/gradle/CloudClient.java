package io.jmix.cloud.gradle;

import com.jcraft.jsch.JSchException;
import io.jmix.cloud.gradle.ssh.SshSession;

public interface CloudClient {

    void createResources() throws Exception;
    void destroyResources() throws Exception;
    InstanceState state();
    SshSession ssh() throws JSchException;
}
