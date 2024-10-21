package lazydevs.persistence.impl.file.general.fileMapper.remote;

import lazydevs.mapper.utils.reflection.InitDTO;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

import static lazydevs.mapper.utils.reflection.ReflectionUtils.getInterfaceReference;

/**
 * @author Abhijeet Rai
 */
@NoArgsConstructor @Getter @Setter @ToString
public class FileDownloadStrategy {
    private SessionFactoryInit sessionFactoryInit;
    private String fileName;
    private String sourceDirectory;
    private String workingDirectory;

    public FileDownloadStrategy(SessionFactoryInit sessionFactoryInit, String fileName, String sourceDirectory, String workingDirectory) {
        this.sessionFactoryInit = sessionFactoryInit;
        this.fileName = fileName;
        this.sourceDirectory = sourceDirectory;
        this.workingDirectory = workingDirectory;
    }

    public FileDownloadStrategy(InitDTO sessionFactoryInit, String fileName, String sourceDirectory, String workingDirectory) {
        this.sessionFactoryInit = getInterfaceReference(sessionFactoryInit, SessionFactoryInit.class);
        this.fileName = fileName;
        this.sourceDirectory = sourceDirectory;
        this.workingDirectory = workingDirectory;
    }

    @Getter @Setter @ToString
    public static class SessionFactoryInit{
        private InitDTO init;
        private String fileProtocol = "SFTP";
        private Map<String, Object> params = new LinkedHashMap<>();
    }
}


