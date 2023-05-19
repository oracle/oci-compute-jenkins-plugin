package com.oracle.cloud.baremetal.jenkins.ssh;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FileCreator {

    String createfilename;

    public void createFileOnMaster() throws IOException, InterruptedException {
        FilePath workspace = new FilePath(new File("."));
        FileCreationCallable fcc = new FileCreationCallable();
        fcc.filename = createfilename;
        workspace.act(fcc);
    }

    private static class FileCreationCallable extends MasterToSlaveFileCallable<Void> {
        private String filename;

        @Override
        public Void invoke(File file, VirtualChannel virtualChannel) throws IOException {
            File newFile = new File(file, filename);
            try (OutputStream outputStream = new FileOutputStream(newFile)) {
                String content = "";
                outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        }
    }
}
