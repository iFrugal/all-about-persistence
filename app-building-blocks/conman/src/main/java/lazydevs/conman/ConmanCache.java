package lazydevs.conman;

import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.file.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static lazydevs.mapper.utils.file.FileUtils.readInputStreamAsString;


/**
 * @author Abhijeet Rai
 */
//@Component
    @Slf4j
public class ConmanCache {
    private final Map<String, MockConfig> mockConfigMap = new ConcurrentHashMap<>();
    @Autowired private ConmanConfig conmanConfig;
    @Autowired ApplicationContext applicationContext;
    @Value("${conman.banner.path:classpath:conman-banner.txt}")
    private String conmanBannerPath;

    @PostConstruct
    public void init() throws IOException {
        log.info(readInputStreamAsString(applicationContext.getResource(conmanBannerPath).getInputStream()));
        mockConfigMap.clear();
        conmanConfig.getMockMappingFiles().stream().forEach(filePath -> {
            try {
                register(null, applicationContext.getResources(filePath));
            }catch (Exception e){
                if(!filePath.equals("classpath:conman.yml")){
                    throw new IllegalArgumentException("filePath = "+filePath, e);
                }
            }
        });
        mockConfigMap.entrySet().forEach(e -> log.debug("" + e));
    }

    public void register(String tenantId, Resource... resources) {
        if(resources != null){
            Arrays.stream(resources).forEach(resource -> {
                try {
                    log.info("Loading File = {}", resource.getURL());
                    register(null, resource.getInputStream());
                } catch (IOException e) {
                    throw new IllegalStateException("", e);
                }
            });
        }
    }

    public void register(String tenantId, InputStream inputStream){
        try {
            List<MockConfig> mockConfigs = SerDe.YAML.deserializeToList(inputStream, MockConfig.class);
            mockConfigs.stream().forEach(mockConfig -> {
                if(null != tenantId) {
                    mockConfig.setTenantId(tenantId);
                }
                setMockConfig(mockConfig);
            });
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static String getKey(HttpMethod httpMethod, String uri, String tenantId){
        return httpMethod.name() + "_" + uri + "_"+ (null == tenantId ? "null" : tenantId);
    }

    public MockConfig getMockConfig(HttpMethod httpMethod, String uri, String tenantId){
        MockConfig mockConfig = mockConfigMap.get(getKey(httpMethod, uri, tenantId));
        if(null == mockConfig){
            mockConfig = mockConfigMap.get(getKey(httpMethod, uri, null));
        }
        return mockConfig;
    }

    public void setMockConfig(MockConfig mockConfig){
        if(null != mockConfig.getTenantIds() && !mockConfig.getTenantIds().isEmpty()){
            mockConfig.getTenantIds().forEach(tenantId -> mockConfigMap.put(getKey(mockConfig.getRequest().getHttpMethod(), mockConfig.getRequest().getUri(), tenantId), mockConfig));
        }else {
            mockConfigMap.put(getKey(mockConfig.getRequest().getHttpMethod(), mockConfig.getRequest().getUri(), mockConfig.getTenantId()), mockConfig);
        }
    }

    public MockConfig unsetMockConfig(MockConfig mockConfig){
        return mockConfigMap.remove(getKey(mockConfig.getRequest().getHttpMethod(), mockConfig.getRequest().getUri(), mockConfig.getTenantId()));
    }
}
