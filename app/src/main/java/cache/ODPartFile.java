package cache;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.File;

@XStreamAlias("Subfile_OD")
class ODPartFile extends PartFile {

    @Override
    public File getSendFile(byte[] coef) {
        return getODSendFile(coef);
    }
}
