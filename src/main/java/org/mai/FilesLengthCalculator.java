package org.mai;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FilesLengthCalculator implements Runnable {
    private final File directory;
    private long result = 0L;
    private Exception exception;

    private List<FilesLengthCalculator> callables;
    private List<Thread> threads;

    public FilesLengthCalculator(File directory) {
        this.directory = directory;
    }

    @Override
    public void run() {
        var subDirectories = getSubDirectories();

        startThreads(subDirectories);

        result = getFilesLengthInCurrentDirectory() + getThreadsSum();
    }

    private File[] getSubDirectories() {
        var subDirs = directory.listFiles(File::isDirectory);
        if (subDirs == null) {
            return new File[0];
        }
        return subDirs;
    }

    private void startThreads(File[] subDirs) {
        callables = Arrays.stream(subDirs)
                .map(FilesLengthCalculator::new)
                .collect(Collectors.toList());

        threads = callables.stream()
                .map(Thread::new)
                .collect(Collectors.toList());

        threads.forEach(Thread::start);
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

    private long getThreadsSum() {
        if (!joinThreads()) {
            return 0;
        }

        return CalculateThreadSum();
    }

    private boolean joinThreads() {
        try {
            for (var thread : threads) {
                thread.join();
            }
            return true;
        } catch (InterruptedException e) {
            exception = e;
            return false;
        }
    }

    private long CalculateThreadSum() {
        var callableWithError = callables.stream()
                .filter(x -> !x.isSuccess())
                .findFirst();

        if (callableWithError.isPresent()) {
            exception = callableWithError.get().getException();
            return 0;
        }

        return callables.stream()
                .filter(FilesLengthCalculator::isSuccess)
                .map(FilesLengthCalculator::getResult)
                .reduce(Long::sum)
                .orElse(0L);
    }

    public long getResult() {
        return result;
    }

    public boolean isSuccess() {
        return exception == null;
    }

    public Exception getException() {
        return exception;
    }
}
