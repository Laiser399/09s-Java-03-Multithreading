package org.mai;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

public class FilesLengthRecursiveCalculator extends RecursiveTask<Long> {
    private final File directory;
    private List<ForkJoinTask<Long>> subDirsTasks;

    public FilesLengthRecursiveCalculator(File directory) {
        this.directory = directory;
    }

    @Override
    protected Long compute() {
        startSubDirsTasks();

        var currentDirFilesLength = getFilesLengthInCurrentDirectory();

        var subDirsFilesLength = joinSubDirsTasks();

        return currentDirFilesLength + subDirsFilesLength;
    }

    private void startSubDirsTasks() {
        subDirsTasks = Arrays.stream(getSubDirectories())
                .map(FilesLengthRecursiveCalculator::new)
                .map(FilesLengthRecursiveCalculator::fork)
                .collect(Collectors.toList());
    }

    private File[] getSubDirectories() {
        var subDirs = directory.listFiles(File::isDirectory);
        if (subDirs == null) {
            return new File[0];
        }
        return subDirs;
    }

    private long getFilesLengthInCurrentDirectory() {
        var sum = 0L;

        var files = directory.listFiles(File::isFile);
        if (files != null) {
            for (var file : files) {
                sum += file.length();
            }
        }

        return sum;
    }

    private long joinSubDirsTasks() {
        return subDirsTasks.stream()
                .map(ForkJoinTask::join)
                .reduce(Long::sum)
                .orElse(0L);
    }
}
