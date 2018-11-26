package cache;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.io.File;
import data.ConstantData;

@XStreamAlias("Subfile_RS")
class RSPartFile extends PartFile {
    @Override
    public void initPartFile(String folderPath, int partNo, int K, String AndroidId, File file, int startPos, int len) {
        super.initPartFile(folderPath, partNo, K, AndroidId, file, startPos, len);
        // 生成再编码文件
        reencodePartFile();
    }

    @Override
    public File getSendFile(byte[] coef) {
        //说明是文件源 则执行NC
        if (AndroidId.equals(ConstantData.ANDROID_ID)) {
            return getNCSendFile(coef);
        } else {
            //不是文件源 则执行OD
            return getODSendFile(coef);
        }
    }

    @Override
    public void afterSendFile(File file) {
        //说明是文件源 则执行NC
        if (AndroidId.equals(ConstantData.ANDROID_ID)) {
            afterNCSendFile(file);
        } else {
            //不是文件源 则执行OD
            afterODSendFile(file);
        }
    }
}
