package cache;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.File;
@XStreamAlias("Subfile_NC")
class NCPartFile extends PartFile {
    @Override
    public File getSendFile() {
        return null;
    }
}
