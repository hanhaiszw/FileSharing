package nc;

import java.util.concurrent.locks.ReentrantLock;

public class BufferControl {
    //只有两种可能的数组长度 2.5 和 10M
    private final static int SMALL_SIZE = 3 * 1024 * 1024;
    private final static int BIG_SIZE = 11 * 1024 * 1024;
    // 4个
    private static byte[][] mulRetBuffer_3;
    // 1个
    private static byte[] mulRetBuffer_11;
    //private static byte[] mulRetBuffer_sp;
    // 5把锁
    private final static ReentrantLock[] lock;
    private final static ReentrantLock lock_11;

    //private final static ReentrantLock lock_sp;
    static {
        mulRetBuffer_3 = new byte[3][SMALL_SIZE];
        //mulRetBuffer_11 = new byte[11 * 1024 * 1024];
        lock = new ReentrantLock[3];
        lock[0] = new ReentrantLock(true);
        lock[1] = new ReentrantLock(true);
        lock[2] = new ReentrantLock(true);

        lock_11 = new ReentrantLock(true);
    }

    private BufferControl(){

    }

    public static byte[] getBuffer(int len) {
        if (len <= SMALL_SIZE) {
            while(true){
                for (int i = 0; i < mulRetBuffer_3.length; i++) {
                    if (lock[i].tryLock()) {
                        return mulRetBuffer_3[i];
                    }
                }
            }
        } else if (len <= BIG_SIZE) {
            lock_11.lock();
            if (mulRetBuffer_11 == null) {
                mulRetBuffer_11 = new byte[BIG_SIZE];
            }
            return mulRetBuffer_11;
        }
        //error
        return null;
    }

    public static void releaseBuffer(byte[] bytes) {
        for (int i = 0; i < mulRetBuffer_3.length; i++) {
            if (bytes == mulRetBuffer_3[i]) {
                try {
                    lock[i].unlock();
                } catch (IllegalMonitorStateException e) {
                    //如果当前线程不拥有此锁
                    e.printStackTrace();
                }
                return;
            }
        }

        if (bytes == mulRetBuffer_11) {
            try {
                lock_11.unlock();
            } catch (IllegalMonitorStateException e) {
                //如果当前线程不拥有此锁
                e.printStackTrace();
            }
        }
    }
}
