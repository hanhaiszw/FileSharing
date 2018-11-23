package cache;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.File;

@XStreamAlias("Subfile_RS")
class RSPartFile extends PartFile {
    @Override
    public File getSendFile() {
        return null;
    }
}
