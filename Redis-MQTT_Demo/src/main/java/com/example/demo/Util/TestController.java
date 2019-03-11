package com.example.demo.Util;

import com.example.demo.Config.MqttGateWay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private MqttGateWay mqttGateway;

    @RequestMapping("/sendMqtt.do")
    public String sendMqtt(String sendData) {
        mqttGateway.sendToMqtt(sendData, "hello");
        return "OK";
    }
}
