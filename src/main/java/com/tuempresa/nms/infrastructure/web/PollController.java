package com.tuempresa.nms.infrastructure.web;

import com.tuempresa.nms.core.domain.DeviceReading;
import com.tuempresa.nms.core.services.DevicePoller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/poll")
public class PollController {

    private final DevicePoller poller;

    public PollController(DevicePoller poller) {
        this.poller = poller;
    }

    @GetMapping
    public DeviceReading poll(@RequestParam String ip) {
        return poller.poll(ip);
    }
}
