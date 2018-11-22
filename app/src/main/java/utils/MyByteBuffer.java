package utils;


import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 为了避免程序中不断申请释放大数组，这里申请几个放在这里
 */
public class MyByteBuffer {
    //只有两种可能的数组长度 2.5 和 10M
    private final static int SMALL_SIZE = 3 * 1024 * 1024;
    private final static int BIG_SIZE = 11 * 1024 * 1024;

    private static Vector<BufferWithLock> bufferWithLocks_3 = new Vector<>();
    private static Vector<BufferWithLock> bufferWithLocks_11 = new Vector<>();

    static {
        // 4个长度11的   4个长度3的
        for (int i = 0; i < 4; i++) {
            bufferWithLocks_3.add(new BufferWithLock(SMALL_SIZE));
            bufferWithLocks_11.add(new BufferWithLock(BIG_SIZE));
        }
    }

    //获取buffer
    public static byte[] getBuffer(int len) throws Exception {
        Vector<BufferWithLock > bufferWithLocks;
        if (len <= 1 * 1024 * 1024) {
            return new byte[len];
        } else if (len <= SMALL_SIZE) {
            bufferWithLocks = bufferWithLocks_3;
        } else if (len <= BIG_SIZE) {
            bufferWithLocks = bufferWithLocks_11;
        } else {
            return new byte[len];
        }
        while (true) {
            for (BufferWithLock bufferWithLock : bufferWithLocks) {
                if (bufferWithLock.bufferTryLock()) {
                    //Log.d("hanhai", "获取到buffer");
                    return bufferWithLock.getBuffer();
                }
            }
        }

    }

    //释放buffer   一定要在finally中调用
    public static void releaseBuffer(byte[] bytes) {
        if (bytes == null) return;
        for (BufferWithLock bufferWithLock : bufferWithLocks_3) {
            if (bufferWithLock.isBufferAddEqual(bytes)) {
                bufferWithLock.bufferUnlock();
                return;
            }
        }

        for (BufferWithLock bufferWithLock : bufferWithLocks_11) {
            if (bufferWithLock.isBufferAddEqual(bytes)) {
                bufferWithLock.bufferUnlock();
                return;
            }
        }
    }
}

class BufferWithLock {
    private ReentrantLock lock;
    private byte[] buffer;
    private int len;

    BufferWithLock(int len) {
        lock = new ReentrantLock(true);
        this.len = len;
    }

    public byte[] getBuffer() {
        if (buffer == null) {
            buffer = new byte[len];
        }
        return buffer;
    }

    public boolean isBufferAddEqual(byte[] bytes) {
        return buffer == bytes;
    }

    public boolean bufferTryLock() {
        return lock.tryLock();
    }


    public void bufferUnlock() {
        try {
            lock.unlock();
        } catch (IllegalMonitorStateException e) {
            //如果当前线程不拥有此锁
            e.printStackTrace();
        }
    }
}