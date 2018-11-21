package utils;


import android.util.Log;

import java.util.concurrent.locks.ReentrantLock;

public class MyByteBuffer {
    private final static ReentrantLock[] lock;
    private final static byte[][] buffer;

    static {
        buffer = new byte[3][11 * 1024 * 1024];
        //创建一个具有给定公平策略的 ReentrantLock。
        lock = new ReentrantLock[3];
        lock[0] = new ReentrantLock(true);
        lock[1] = new ReentrantLock(true);
        lock[2] = new ReentrantLock(true);
    }

    public static byte[] getBuffer() throws Exception {
        while(true){
            for (int i = 0; i < buffer.length; i++) {
                if (lock[i].tryLock()) {
                    Log.d("hanhai","使用buffer "+i);
                    return buffer[i];
                }
            }
        }
    }

    public static void releaseBuffer(byte[] bytes) {
//        if(lock.isLocked()){
//            lock.unlock();
//        }
        for (int i = 0; i < buffer.length; i++) {
            if (bytes == buffer[i]) {
                try {
                    lock[i].unlock();
                } catch (IllegalMonitorStateException e) {
                    //如果当前线程不拥有此锁
                    e.printStackTrace();
                }
                return;
            }
        }

    }
}
