package cache;

import data.RunMode;

//创建partfile子类对象
class PartFileFactory {
    private PartFileFactory() {

    }

    static PartFile createPartFile(String runModeString) {
        PartFile partFile = null;
        switch (runModeString) {
            case RunMode.OD_MODE:
                partFile = new ODPartFile();
                break;
            case RunMode.RS_MODE:
                partFile = new RSPartFile();
                break;
            case RunMode.NC_MODE:
                partFile = new NCPartFile();
                break;
            default:
                break;
        }

        return partFile;
    }
}
