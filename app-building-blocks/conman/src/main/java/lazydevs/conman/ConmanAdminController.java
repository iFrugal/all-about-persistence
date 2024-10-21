package lazydevs.conman;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * @author Abhijeet Rai
 */
@RestController
@RequestMapping("/conman")
public class ConmanAdminController {

    @Autowired ConmanCache conmanCache;

    @GetMapping("/dryrun")
    public void dryRun(HttpServletRequest req, HttpServletResponse res, @RequestParam HttpMethod httpMethod, String uri, String tenantId, String requestBody, Map<String, String> headers) throws IOException {
        ConmanServlet.getInstance().serviceInternal(httpMethod, uri, tenantId, res, conmanCache.getMockConfig(httpMethod, uri, tenantId));
    }

    @PostMapping(value = "/register", consumes = MULTIPART_FORM_DATA_VALUE)
    public void register(@RequestPart(required = false) String tenantId,
                                            @RequestPart MultipartFile registrationFile) throws IOException {
        conmanCache.register(tenantId, registrationFile.getInputStream());
    }

}
