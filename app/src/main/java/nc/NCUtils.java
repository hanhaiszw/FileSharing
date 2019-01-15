package nc;

import android.util.Log;

import java.util.Random;

/**
 * Created by mroot on 2018/4/7.
 */

public class NCUtils {
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        //申请有限域
        InitGalois();

        Log.e("hanhai", "载入native-cpp");
    }


    private NCUtils() {
    }

    public static byte[] mul(byte[] a, byte[] b) {
        byte[] ret = new byte[2 * 2];
        Multiply2(a, 2, 2, b, 2, 2, ret);
        return ret;
    }

    public static void test(){

    }

    /**
     * 只生成一个再编码文件
     *
     * @param encodeData
     * @param row
     * @param col
     * @param result     存放结果的数组
     * @return
     */
    private static byte[] reencode(byte[] encodeData, int row, int col, byte[] result) {
        //1*row 随机矩阵  与  row * col的数据矩阵相乘
        byte[] randomMatrix = new byte[row];
        Random random = new Random();
        random.nextBytes(randomMatrix);
        //为了避免矩阵的来回复制，
        //再编码没有取出首字节K值，
        //再编码矩阵与编码数据相乘后，在把再编码结果首位置为K值
        Multiply2(randomMatrix, 1, row, encodeData, row, col, result);
        result[0] = encodeData[0];
        return result;
    }

    /**
     * 解码
     *
     * @param encodeData
     * @param row
     * @param col
     * @param result     存放结果的数组
     * @return
     */
    private static void decode(byte[] encodeData, int row, int col, byte[] result) {
        int K = encodeData[0];
        if (row < K) {
            //数据不足，解码失败
            return;
        }
        //取出编码矩阵
        byte[] coefMatrix = new byte[K * K];
        for (int i = 0; i < K; i++) {
            for (int j = 0; j < K; j++) {
                coefMatrix[i * K + j] = encodeData[i * col + 1 + j];
            }
        }
        //对编码矩阵求逆
        byte[] invMatrix = InverseMatrix(coefMatrix, K);
        //为编码数据来回在数组中复制，这里没有取出encodeData 1+K之后的字节
        //而是对整个encodeData数据进行相乘，然后再取出原始数据

        //解码
        //byte[] result = Multiply(invMatrix, K, K, encodeData, K, col);
        Multiply2(invMatrix, K, K, encodeData, K, col, result);
    }


    public static int getRank(byte[][] matrix) {
        int row = matrix.length;
        int col = matrix[0].length;
        byte[] mat = new byte[row * col];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                mat[i * col + j] = matrix[i][j];
            }
        }
        int rank = GetRank(mat, row, col);
        return rank;
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //申请有限域
    private static native void InitGalois();

    //释放jni申请的空间
    public static native void UninitGalois();

    //矩阵相乘
    public static native byte[] Multiply(byte[] matrix1, int row1, int col1, byte[] matrix2, int row2, int col2);

    //矩阵相乘
    public static native void Multiply2(byte[] matrix1, int row1, int col1, byte[] matrix2, int row2, int col2, byte[] ret);

    //矩阵求逆
    public static native byte[] InverseMatrix(byte[] arrayData, int nK);

    //矩阵求秩
    private static native int GetRank(byte[] matrix, int nRow, int nCol);
}
