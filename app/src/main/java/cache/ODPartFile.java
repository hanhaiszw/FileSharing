package cache;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.File;

@XStreamAlias("Subfile_OD")
class ODPartFile extends PartFile {

    @Override
    public File getSendFile(byte[] coef) {
        return getODSendFile(coef);
    }

    @Override
    public void afterSendFile(File file) {
        afterODSendFile(file);
    }
    @Override
    public String getMode() {
        return "OD";
    }

}
