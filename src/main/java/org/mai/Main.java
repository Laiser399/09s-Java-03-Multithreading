package org.mai;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Count of arguments must be equal 1.");
            return;
        }

        var root = args[0];
        var rootDir = new File(root);

        System.out.println("One thread");
        test(() -> byOneThread(rootDir));
        System.out.println();

        System.out.println("Thread per dir");
        test(() -> byThreadPerDirectory(rootDir));
        System.out.println();

        System.out.println("ForkJoinPool");
        test(() -> byForkJoinPool(rootDir));
    }

    private static void test(Callable<Long> callable) {
        var start = System.currentTimeMillis();

        try {
            var res = callable.call();
            System.out.printf("Length: %s bytes\n", res);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

        var elapsed = System.currentTimeMillis() - start;
        System.out.printf("Elapsed %sms\n", elapsed);
    }

    private static long byOneThread(File rootDir) {
        return getFilesLength(rootDir);
    }

    private static long getFilesLength(File directory) {
        var sum = 0L;

        var files = directory.listFiles(File::isFile);
        if (files != null) {
            for (var file : files) {
                sum += file.length();
            }
        }

        var subDirs = directory.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (var subDir : subDirs) {
                sum += getFilesLength(subDir);
            }
        }

        return sum;
    }

    private static long byThreadPerDirectory(File rootDir) {
        var runnable = new FilesLengthCalculator(rootDir);
        var rootThread = new Thread(runnable);

        rootThread.start();
        try {
            rootThread.join();
            return runnable.getResult();
        } catch (InterruptedException e) {
            return -1;
        }
    }

    private static long byForkJoinPool(File rootDir) {
        var forkJoinPool = new ForkJoinPool();
        return forkJoinPool.invoke(new FilesLengthRecursiveCalculator(rootDir));
    }

}
