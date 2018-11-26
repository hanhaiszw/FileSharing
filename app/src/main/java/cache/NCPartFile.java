package cache;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.File;

import nc.NCUtils;

@XStreamAlias("Subfile_NC")
class NCPartFile extends PartFile {
    @Override
    public void initPartFile(String folderPath, int partNo, int K, String AndroidId, File file, int startPos, int len) {
        super.initPartFile(folderPath, partNo, K, AndroidId, file, startPos, len);
        // 生成再编码文件
        reencodePartFile();
    }

    @Override
    public File getSendFile(byte[] coef) {
        return getNCSendFile(coef);
    }

    @Override
    public void afterSendFile(File file) {
        afterNCSendFile(file);
    }
}
