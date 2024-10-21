package lazydevs.persistence.impl.file.general.fileMapper.remote;

import lazydevs.mapper.file.FileMapper;
import lazydevs.mapper.utils.reflection.InitDTO;
import lazydevs.persistence.impl.file.general.ReadInstruction;
import lazydevs.persistence.impl.file.general.fileMapper.GeneralFileMapperReader;
import lombok.Getter;

import java.io.File;

import static lazydevs.mapper.utils.reflection.ReflectionUtils.getInterfaceReference;

/**
 * @author Abhijeet Rai
 */

public class RemoteGeneralFileMapperReader extends GeneralFileMapperReader {

    @Getter
    private final FileSessionProvider fileSessionProvider;

    public RemoteGeneralFileMapperReader(FileDownloadStrategy fileDownloadStrategy, FileMapper fileMapper) {
        super(fileMapper);
        this.fileSessionProvider = new FileSessionProvider(fileDownloadStrategy);
    }

    public RemoteGeneralFileMapperReader(InitDTO fileDownloadStrategyInit, InitDTO fileMapperInit) {
        super(fileMapperInit);
        this.fileSessionProvider = new FileSessionProvider(getInterfaceReference(fileDownloadStrategyInit, FileDownloadStrategy.class));
    }

    @Override
    protected boolean ensureFileExists(ReadInstruction readInstruction) {
        File downloadedFile = fileSessionProvider.downloadFile();
        readInstruction.setFilePath(downloadedFile.getAbsolutePath());
        return super.ensureFileExists(readInstruction);
    }
}
