package utils;


import android.util.Log;

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
        // 4个长度11M的   4个长度3M的
        for (int i = 0; i < 4; i++) {
            bufferWithLocks_3.add(new BufferWithLock(SMALL_SIZE));
            bufferWithLocks_11.add(new BufferWithLock(BIG_SIZE));
        }
    }

    //获取buffer
    public static byte[] getBuffer(int len) throws Exception {
        Vector<BufferWithLock> bufferWithLocks;
        if (len <= 1 * 1024 * 1024) {
            return new byte[len];
        } else if (len <= SMALL_SIZE) {
            bufferWithLocks = bufferWithLocks_3;
        } else if (len <= BIG_SIZE) {
            bufferWithLocks = bufferWithLocks_11;
        } else {
            return new byte[len];
        }

        //有可能卡死在此处
        //需要设置超时 跳出
        long startTime = System.currentTimeMillis();
        while (true) {
            for (int i = 0; i < bufferWithLocks.size(); i++) {
                BufferWithLock bufferWithLock = bufferWithLocks.get(i);
                if (bufferWithLock.bufferTryLock()) {
                    Log.e("hanhai", "获取到" + i + "下标buffer");

                    //Log.d("hanhai", "获取到buffer");
                    return bufferWithLock.getBuffer();
                }
            }

            // 如果5秒还没获取到buffer，则自定义一个buffer
            if (System.currentTimeMillis() - startTime > 5000) {
                return new byte[len];
            }

        }

    }

    //释放buffer   一定要在finally中调用
    public static void releaseBuffer(byte[] bytes) {
        if (bytes == null) return;
        for (BufferWithLock bufferWithLock : bufferWithLocks_3) {
            if (bufferWithLock.isBufferEqual(bytes)) {
                bufferWithLock.bufferUnlock();
                return;
            }
        }

        for (BufferWithLock bufferWithLock : bufferWithLocks_11) {
            if (bufferWithLock.isBufferEqual(bytes)) {
                bufferWithLock.bufferUnlock();
                return;
            }
        }
    }
}

class BufferWithLock {
    // ReentrantLock 是在多个线程内同步，不能再单个线程内保证资源只被一次占有
    //private ReentrantLock lock;
    private byte[] buffer;
    private int len;

    /**
     * 0 为开锁状态
     * 1 为加锁状态
     */
    private int myLock;

    BufferWithLock(int len) {
        //lock = new ReentrantLock(true);
        myLock = 0;
        this.len = len;
    }

    public byte[] getBuffer() {
        if (buffer == null) {
            buffer = new byte[len];
        }
        return buffer;
    }

    public boolean isBufferEqual(byte[] bytes) {
        return buffer == bytes;
    }

    public synchronized boolean bufferTryLock() {
        //return lock.tryLock();
        if (myLock == 0) {
            myLock = 1;
            return true;
        } else {
            return false;
        }
    }


    public synchronized void bufferUnlock() {
//        try {
//            lock.unlock();
//        } catch (IllegalMonitorStateException e) {
//            //如果当前线程不拥有此锁
//            e.printStackTrace();
//        }
        myLock = 0;
    }
}