package cache;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.File;

import nc.NCUtils;

@XStreamAlias("Subfile_NC")
class NCPartFile extends PartFile {
    @Override
    public File getSendFile(byte[] coef) {
        return getNCSendFile(coef);
    }
}
