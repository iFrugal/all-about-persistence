package lazydevs.scheduleit;

import lazydevs.scheduleit.pojo.ScheduledInstructionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RestController
@RequestMapping("/scheduleIt")
public class ScheduleItController {
    @Autowired private ScheduleItService scheduleItService;

    @GetMapping("/{scheduleName}")
    public ScheduledInstructionDTO get(@PathVariable(name = "scheduleName") String scheduleName){
        return new ScheduledInstructionDTO(scheduleItService.getSchedule(scheduleName));
    }

    @GetMapping("/list")
    public List<ScheduledInstructionDTO> getAll(){
        return scheduleItService.getAllSchedules().stream().map(ScheduledInstructionDTO::new).collect(Collectors.toList());
    }

    @PostMapping("/{scheduleName}/run")
    public void run(@PathVariable(name = "scheduleName") String scheduleName){
        scheduleItService.runSchedule(scheduleName);
    }
}
