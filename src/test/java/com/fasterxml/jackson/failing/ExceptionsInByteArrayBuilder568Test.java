package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExceptionsInByteArrayBuilder568Test extends BaseTest {

    static class ResetRunnable implements Runnable {
        private ByteArrayBuilder arrayBuilder;

        public ResetRunnable(ByteArrayBuilder b) {
            this.arrayBuilder = b;
        }

        @Override
        public void run() {
            arrayBuilder.reset();
        }
    }

    static class WriteRunnable implements Runnable {
        private ByteArrayBuilder arrayBuilder;

        public WriteRunnable(ByteArrayBuilder b) {
            this.arrayBuilder = b;
        }

        @Override
        public void run() {
            arrayBuilder.write(new byte[100]);
        }
    }

    public void testResetWhileWritingToByteArray() {
        ByteArrayBuilder b = new ByteArrayBuilder();
        byte[] bytes = new byte[99999999];
        b.write(bytes);

//        Should throw: java.lang.RuntimeException: Internal error: total len assumed to be 99999999, copied .... bytes
        _runInSeparateThreadWithDelay(new ResetRunnable(b), 150);
        b.toByteArray();
    }

    public void testWriteWhileWritingToByteArray() {
        ByteArrayBuilder b = new ByteArrayBuilder();
        byte[] bytes = new byte[99999999];
        b.write(bytes);

//        Should throw: java.lang.ArrayIndexOutOfBoundsException at java.lang.System.arraycopy(Native Method)
        _runInSeparateThreadWithDelay(new WriteRunnable(b), 150);
        b.toByteArray();
    }

    private void _runInSeparateThreadWithDelay(Runnable runnable, int delay) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(runnable, delay, TimeUnit.MILLISECONDS);
    }
}
