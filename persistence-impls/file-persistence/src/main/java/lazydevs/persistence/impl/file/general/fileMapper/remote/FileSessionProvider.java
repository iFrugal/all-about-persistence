package lazydevs.persistence.impl.file.general.fileMapper.remote;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lazydevs.mapper.utils.reflection.ClassUtils.NamedArg;
import lazydevs.mapper.utils.reflection.Init;
import lazydevs.persistence.connection.ConnectionProvider;
import lazydevs.persistence.impl.file.general.fileMapper.remote.FileDownloadStrategy.SessionFactoryInit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Properties;

import static java.util.Arrays.asList;
import static lazydevs.mapper.utils.reflection.ReflectionUtils.getInterfaceReference;

@RequiredArgsConstructor @ToString @Getter @Setter
@Slf4j
public class FileSessionProvider implements ConnectionProvider<Session> {
    private final FileDownloadStrategy fileDownloadStrategy;

    private SessionFactory getSessionFactory() {
        SessionFactoryInit sessionFactoryInit = fileDownloadStrategy.getSessionFactoryInit();

        return sessionFactoryInit.getFileProtocol() != null
                ? getSessionFactory(sessionFactoryInit.getFileProtocol(), sessionFactoryInit.getParams())
                : getInterfaceReference(sessionFactoryInit.getInit(), SessionFactory.class);
    }

    @JsonIgnore
    public Session getConnection() {
        return getSessionFactory().getSession();
    }

    public File downloadFile() {
        try (Session session = getConnection()) {
            return downloadFileToLocal(session, fileDownloadStrategy.getSourceDirectory() + "/" + fileDownloadStrategy.getFileName(), fileDownloadStrategy.getWorkingDirectory() + "/" + fileDownloadStrategy.getFileName());
        } catch (Exception e) {
            log.error("Exception while accessing SFTP Files", e);
            throw new RuntimeException("Exception while accessing SFTP Files", e);
        }
    }
    private File downloadFileToLocal(final Session session, final String sourceFilePath, final String localFilePath) {
        File localFile = new File(localFilePath);

        try (FileOutputStream fos = new FileOutputStream(localFile)) {
            session.read(sourceFilePath, fos);
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred while moving the file from remote to local-temp", e );
        }

        return localFile;
    }


    @JsonIgnore
    public SessionFactory getSessionFactory(String fileProtocol, Map<String, Object> params){
        switch(fileProtocol){
            case "SFTP" :
                Properties sessionConfig = new Properties();
                sessionConfig.setProperty("PreferredAuthentications", "password");
                return getInterfaceReference(buildInit(params, sessionConfig), SessionFactory.class);

            default:
                throw new IllegalArgumentException("Not handled protocol = " + this);

        }
    }

    private Init buildInit(Map<String, Object> params, Properties sessionConfig) {
        return Init.builder()
                   .fqcn("org.springframework.integration.sftp.session.DefaultSftpSessionFactory")
                   .attributes(asList(
                           new NamedArg("host", params.get("host")),
                           new NamedArg("port", params.get("port")),
                           new NamedArg("user", params.get("user")),
                           new NamedArg("password", params.get("password")),
                           new NamedArg("allowUnknownKeys", true),
                           new NamedArg("sessionConfig", sessionConfig)))
                   .build();
    }
}
