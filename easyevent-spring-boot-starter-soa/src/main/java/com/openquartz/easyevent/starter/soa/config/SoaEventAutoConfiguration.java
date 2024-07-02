package com.openquartz.easyevent.starter.soa.config;

import com.openquartz.easyevent.common.property.EasyEventProperties;
import com.openquartz.easyevent.common.utils.IpUtil;
import com.openquartz.easyevent.core.EventBus;
import com.openquartz.easyevent.core.expression.ExpressionParser;
import com.openquartz.easyevent.core.publisher.EventPublisher;
import com.openquartz.easyevent.starter.soa.core.SoaEventCenter;
import com.openquartz.easyevent.starter.soa.core.SoaEventHandler;
import com.openquartz.easyevent.starter.soa.core.rocket.SoaEventRocketMqProducer;
import com.openquartz.easyevent.starter.soa.core.rocket.RocketSoaEventCenter;
import com.openquartz.easyevent.starter.soa.core.rocket.SoaEventRocketMqCommonProperty;
import com.openquartz.easyevent.starter.soa.core.rocket.SoaRocketMqEventTrigger;
import com.openquartz.easyevent.starter.spring.boot.autoconfig.EasyEventAfterAutoConfiguration;
import com.openquartz.easyevent.starter.spring.boot.autoconfig.EasyEventTransferAutoConfiguration;
import com.openquartz.easyevent.transfer.api.constant.TransferConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.common.UtilAll;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * SoaEventAutoConfiguration
 *
 * @author svnee
 */
@Slf4j
@EnableConfigurationProperties({SoaEventRocketMqCommonProperty.class})
@AutoConfigureBefore(EasyEventAfterAutoConfiguration.class)
@AutoConfigureAfter(EasyEventTransferAutoConfiguration.class)
@ConditionalOnClass(MQProducer.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10050)
public class SoaEventAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SoaEventCenter.class)
    public SoaEventCenter soaEventCenter(List<EventBus> eventBusList,
                                         ExpressionParser expressionParser,
                                         EventPublisher eventPublisher,
                                         SoaEventRocketMqProducer soaEventRocketMqProducer) {
        return new RocketSoaEventCenter(eventBusList, expressionParser, eventPublisher, soaEventRocketMqProducer);
    }

    @Bean
    public SoaEventRocketMqProducer rocketMqProducer(MQProducer producer,
                                                     SoaEventRocketMqCommonProperty soaEventRocketMqCommonProperty) {
        return new SoaEventRocketMqProducer(producer, soaEventRocketMqCommonProperty);
    }

    @Bean
    public SoaRocketMqEventTrigger soaRocketMqEventTrigger(SoaEventRocketMqCommonProperty soaEventRocketMqCommonProperty,
                                                           SoaEventCenter soaEventCenter,
                                                           EasyEventProperties easyEventProperties) {
        return new SoaRocketMqEventTrigger(soaEventRocketMqCommonProperty, soaEventCenter::consume, easyEventProperties);
    }


    @Bean
    @ConditionalOnMissingBean(value = MQProducer.class, name = "easySoaEventTriggerMqProducer")
    public MQProducer easySoaEventTriggerMqProducer(SoaEventRocketMqCommonProperty soaEventRocketMqCommonProperty,
                                                    EasyEventProperties easyEventProperties) {

        DefaultMQProducer producer = new DefaultMQProducer(soaEventRocketMqCommonProperty.getProduceGroup());
        producer.setNamesrvAddr(soaEventRocketMqCommonProperty.getHost());
        producer.setVipChannelEnabled(false);
        producer.setRetryTimesWhenSendAsyncFailed(soaEventRocketMqCommonProperty.getProduceTryTimes());
        producer.setSendLatencyFaultEnable(soaEventRocketMqCommonProperty.isProduceLatencyFaultEnable());
        String ipAddress = IpUtil.getIp();
        String[] split = ipAddress.split("\\.");
        producer.setInstanceName(
                TransferConstants.SENDER_PREFIX + "@" + easyEventProperties.getAppId() + "@" + split[split.length - 1]
                        + "@" + UtilAll.getPid());
        producer.setClientIP(ipAddress);
        return producer;
    }

    @Bean
    public SoaEventHandler soaEventHandler(SoaEventCenter soaEventCenter, List<EventBus> eventBusList) {
        SoaEventHandler soaEventHandler = new SoaEventHandler(soaEventCenter);
        eventBusList.forEach(k -> k.register(soaEventHandler));
        return soaEventHandler;
    }

}