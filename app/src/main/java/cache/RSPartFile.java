package cache;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.io.File;
import data.ConstantData;

@XStreamAlias("Subfile_RS")
class RSPartFile extends PartFile {
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
}
