package lazydevs.transporter;

import lazydevs.services.basic.filter.BasicRequestFilter;
import lazydevs.services.basic.filter.RequestContext;
import lazydevs.transporter.config.PipelineContext;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static lazydevs.mapper.utils.SerDe.JSON;
import static lazydevs.persistence.util.MapUtils.getMap;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.OK;

/**
 * @author Abhijeet Rai
 */
@RestController
@RequestMapping("/transport")
public class TransporterController {


    public static final String PIPELINE_RUN_ID = "pipelineRunId";
    @Autowired private TransportService transportService;
    @Value("${transporterAsyncThreadPoolSize:2}")
    private int transporterAsyncThreadPoolSize;
    private BasicRequestFilter basicRequestFilter = new BasicRequestFilter();
    private ExecutorService executorService;

    @PostConstruct
    public void init(){
        executorService = Executors.newFixedThreadPool(transporterAsyncThreadPoolSize);
    }

    @PostMapping("/sync")
    public ResponseEntity<String> transport(@RequestParam String pipelineKey, @RequestBody List<Map<String,Object>> paramsList, HttpServletRequest req, HttpServletResponse resp){
        basicRequestFilter.setRequestContext(req, resp);
        final String pipelineRunId = RequestContext.current().getRequestId();
        basicRequestFilter.setRequestContext(req, resp);
        transportService.transport(pipelineRunId, pipelineKey, paramsList);
        return ResponseEntity.status(OK).body(JSON.serialize(getMap(
                PIPELINE_RUN_ID, pipelineRunId,
                "message", "Your Request is Completed, please use pipelineRunId to track the pipeline run")
        ));
    }

    @PostMapping("/async")
    public ResponseEntity<String> transportAsync(@RequestParam String pipelineKey, @RequestBody List<Map<String,Object>> paramsList, HttpServletRequest req, HttpServletResponse resp){
        basicRequestFilter.setRequestContext(req, resp);
        final String pipelineRunId = RequestContext.current().getRequestId();
        executorService.submit(()-> {
            PipelineContext.getCurrentContext().set(PIPELINE_RUN_ID, pipelineRunId);
            MDC.put(PIPELINE_RUN_ID, pipelineRunId);
            basicRequestFilter.setRequestContext(req, resp);
            transportService.transport(pipelineRunId, pipelineKey, paramsList);
        });
        return ResponseEntity.status(ACCEPTED).body(JSON.serialize(getMap(
                PIPELINE_RUN_ID, pipelineRunId,
                "message", "Your Request is Accepted, please use pipelineRunId to track the pipeline run")
        ));

    }

}
