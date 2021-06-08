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

package io.jmix.cloud.gradle.utils.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import io.jmix.cloud.gradle.InstanceState;

import java.io.*;

public class SshSession implements AutoCloseable {

    private static final int BUFFER_SIZE = 1024;

    private final Session session;

    public static SshSession forInstance(InstanceState instance) {
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(instance.getKeyFile());
            Session session = jsch.getSession(instance.getUsername(), instance.getHost());
            session.setConfig("StrictHostKeyChecking", "no");
            return new SshSession(session);
        } catch (Exception e) {
            throw new SshException("Error creating SSH session for instance " + instance.getHost(), e);
        }

    }

    private SshSession(Session session) {
        this.session = session;
        openSession();
    }

    private void openSession() {
        if (!session.isConnected()) {
            try {
                session.connect();
            } catch (Exception e) {
                throw new SshException("Error establishing connection for SSH session", e);
            }
        }
    }

    @Override
    public void close() {
        if (session.isConnected()) {
            session.disconnect();
        }
    }

    public void execute(String command) {
        execute(command, null, System.out);
    }

    public void execute(String command, InputStream inputStream, OutputStream outputStream) {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            if (inputStream != null) {
                channel.setInputStream(inputStream);
            }
            channel.connect();

            InputStream in = channel.getInputStream();

            byte[] buf = new byte[BUFFER_SIZE];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(buf, 0, BUFFER_SIZE);
                    if (i < 0) break;
                    if (outputStream != null) {
                        outputStream.write(buf, 0, i);
                    }
                }
                if (outputStream != null) {
                    outputStream.flush();
                }
                if (channel.isClosed()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            throw new SshException("Error executing command \"" + command + "\"", e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public void scpUploadFile(File fromFile, String targetPath) {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("scp -t " + targetPath);

            // get I/O streams for remote scp
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            checkScpAck(in);

            // send "C0644 filesize filename", where filename should not include '/'
            String command = "C0644 " + fromFile.length() + " " + fromFile.getName() + "\n";
            out.write(command.getBytes());
            out.flush();

            checkScpAck(in);

            // send a content of a file
            try (FileInputStream fis = new FileInputStream(fromFile)) {
                byte[] buf = new byte[1024];
                while (true) {
                    int len = fis.read(buf, 0, buf.length);
                    if (len <= 0) break;
                    out.write(buf, 0, len);
                }

                // send '\0'
                buf[0] = 0;
                out.write(buf, 0, 1);
                out.flush();

                checkScpAck(in);

                out.close();
            }
        } catch (Exception e) {
            throw new SshException("Error uploading file " + fromFile.getName() + " via SCP", e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private static void checkScpAck(InputStream in) throws IOException {
        int b = in.read();
        if (b == 0) { // success
            return;
        }

        if (b == 1 || b == 2) { // error
            StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            } while (c != '\n');
            throw new SshException(sb.toString());
        }

        throw new SshException("Failed to receive ACK during SCP file transmission");
    }
}
