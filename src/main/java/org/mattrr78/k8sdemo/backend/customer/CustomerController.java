package org.mattrr78.k8sdemo.backend.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/customer")
@Transactional(readOnly = true)
public class CustomerController {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService service;

    private final List<JunkClass> junkList = new ArrayList<>();

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping("/weather/{customerId}")
    ResponseEntity<WeatherStatus> findWeatherStatusByCustomerId(@PathVariable int customerId)  {
        WeatherStatus weatherStatus = service.findWeatherStatusByCustomerId(customerId);
        logMemory("After customer fetch");
        return ResponseEntity.ok(weatherStatus);
    }

    @PostMapping("/junk/{count}")
    ResponseEntity<Boolean> addJunk(@PathVariable int count)  {
        for (int i = 0; i < count; i++)  {
            junkList.add(new JunkClass());
        }
        logMemory("After adding junk to memory");
        return ResponseEntity.ok(true);
    }

    @DeleteMapping("/junk")
    ResponseEntity<Boolean> deleteJunk()  {
        junkList.clear();
        logMemory("After clearing junk");
        return ResponseEntity.ok(true);
    }

    private void logMemory(String message)  {
        long memTotalInMB = (Runtime.getRuntime().totalMemory() / 1024L) / 1024L;
        long memUsedInMB = ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L ) / 1024L;
        LOG.info(message + " => Total: " + memTotalInMB + "MB,  Used: " + memUsedInMB + "MB.");
    }

}
