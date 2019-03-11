package com.example.demo.Config;



import java.util.Arrays;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;


@Configuration
@IntegrationComponentScan
public class MqttSenderConfig {


    @Value("${spring.mqtt.username}")
    private String username;


    @Value("${spring.mqtt.password}")
    private String password;


    @Value("${spring.mqtt.url}")
    private String hostUrl;


    @Value("${spring.mqtt.client.id}")
    private String clientId = String.valueOf(System.currentTimeMillis());


    @Value("${spring.mqtt.default.topic}")
    private String defaultTopic;

    @Value("#{'${spring.mqtt.topics}'.split(',')}")
    private List<String> topics;

    @Value("#{'${spring.mqtt.qosValues}'.split(',')}")
    private List<Integer> qosValues;


    @Bean
    public MqttConnectOptions getMqttConnectOptions() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setPassword(password.toCharArray());
        mqttConnectOptions.setServerURIs(new String[]{hostUrl});
        mqttConnectOptions.setKeepAliveInterval(2);
        // 设置超时时间 单位为秒
        mqttConnectOptions.setConnectionTimeout(10);
        mqttConnectOptions.setMaxInflight(100000000);

        return mqttConnectOptions;

    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(getMqttConnectOptions());
        return factory;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(clientId, mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(defaultTopic);
        messageHandler.setDefaultRetained(false);
        return messageHandler;
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    //接收通道
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }


    //配置client,监听的topic
    @Bean
    public MessageProducer inbound() {
        String[] strings = new String[topics.size()];
        Integer[] ints = new Integer[qosValues.size()];
        topics.toArray(strings);
        qosValues.toArray(ints);

        int[] its = Arrays.stream(ints).mapToInt(Integer::valueOf).toArray();
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + "_inbound", mqttClientFactory(), strings);

        adapter.setCompletionTimeout(3000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    //通过通道获取数据
    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return new MessageHandler() {
            @Override
            public void handleMessage(Message<?> message) throws MessagingException {
                String topic = message.getHeaders().get("mqtt_receivedTopic").toString();
                String type = topic.substring(topic.lastIndexOf("/") + 1, topic.length());
                // System.out.println(topic+"|"+message.getPayload().toString());
            }
        };
    }
}
