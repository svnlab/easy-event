package org.svnee.easyevent.starter.spring.boot.autoconfig;

import static org.svnee.easyevent.common.utils.ParamUtils.checkNotEmpty;
import static org.svnee.easyevent.common.utils.ParamUtils.checkNotNull;

import java.util.Map;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.svnee.easyevent.common.constant.CommonConstants;
import org.svnee.easyevent.common.exception.CommonErrorCode;
import org.svnee.easyevent.common.exception.EasyEventException;
import org.svnee.easyevent.common.property.EasyEventProperties;
import org.svnee.easyevent.common.serde.Serializer;
import org.svnee.easyevent.starter.spring.boot.autoconfig.property.JdbcStorageProperties;
import org.svnee.easyevent.storage.api.EventStorageService;
import org.svnee.easyevent.storage.identify.IdGenerator;
import org.svnee.easyevent.storage.jdbc.JdbcEventStorageServiceImpl;
import org.svnee.easyevent.storage.jdbc.mapper.BusEventEntityMapper;
import org.svnee.easyevent.storage.jdbc.mapper.impl.BusEventEntityMapperImpl;
import org.svnee.easyevent.storage.jdbc.sharding.ShardingRouter;
import org.svnee.easyevent.storage.jdbc.sharding.impl.DefaultShardingRouterImpl;
import org.svnee.easyevent.storage.jdbc.sharding.property.EventStorageShardingProperty;
import org.svnee.easyevent.storage.jdbc.table.EasyEventTableGeneratorSupplier;

/**
 * Jdbc Storage Configuration
 *
 * @author svnee
 **/
@Slf4j
@Configuration
@EnableConfigurationProperties(JdbcStorageProperties.class)
@ConditionalOnClass(JdbcEventStorageServiceImpl.class)
@AutoConfigureAfter(EasyEventStorageAutoConfiguration.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 120)
public class JdbcStorageAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info(
            "-----------------------------------------JdbcStorageAutoConfiguration-------------------------------");
    }

    @Bean
    @ConditionalOnMissingBean(type = "easyEventJdbcStorageDataSource", value = DataSource.class)
    public DataSource easyEventJdbcStorageDataSource(JdbcStorageProperties jdbcStorageProperties,
        Environment environment) {

        Iterable<ConfigurationPropertySource> sources = ConfigurationPropertySources
            .get(environment);
        Binder binder = new Binder(sources);
        Properties properties = binder.bind(
            JdbcStorageProperties.PREFIX + CommonConstants.POINT_SPLITTER + CommonConstants.DATASOURCE_IDENTIFIER_STR,
            Properties.class).get();

        DataSource dataSource = buildDataSource(jdbcStorageProperties);
        buildDataSourceProperties(dataSource, properties);
        return dataSource;
    }


    private DataSource buildDataSource(JdbcStorageProperties jdbcStorageProperties) {
        try {
            String dataSourceType = jdbcStorageProperties.getDatasource().getType();

            Class<? extends DataSource> type = (Class<? extends DataSource>) Class.forName(dataSourceType);
            String driverClassName = jdbcStorageProperties.getDatasource().getDriverClassName();
            String url = jdbcStorageProperties.getDatasource().getUrl();
            String username = jdbcStorageProperties.getDatasource().getUsername();
            String password = jdbcStorageProperties.getDatasource().getPassword();

            checkNotNull(type);
            checkNotEmpty(driverClassName);
            checkNotEmpty(url);
            checkNotEmpty(username);
            checkNotEmpty(password);

            return DataSourceBuilder.create()
                .driverClassName(driverClassName)
                .url(url)
                .username(username)
                .password(password)
                .type(type)
                .build();

        } catch (ClassNotFoundException e) {
            log.error("[JdbcStorageAutoConfiguration#buildDataSource] class-not-found! error", e);
            throw new EasyEventException(CommonErrorCode.CLASS_NOT_FOUND_ERROR);
        }
    }

    private void buildDataSourceProperties(DataSource dataSource, Map<Object, Object> dsMap) {
        try {
            BeanUtils.copyProperties(dataSource, dsMap);
        } catch (Exception e) {
            log.error("[JdbcStorageAutoConfiguration#buildDataSourceProperties]error copy properties,dsMap:{}", dsMap,
                e);
        }
    }

    @Bean
    @ConditionalOnMissingBean(type = "jdbcStorageJdbcTemplate", value = JdbcTemplate.class)
    public JdbcTemplate jdbcStorageJdbcTemplate(DataSource easyEventJdbcStorageDataSource) {
        return new JdbcTemplate(easyEventJdbcStorageDataSource);
    }

    @Bean
    @ConditionalOnMissingBean(type = "busEventEntityMapperImpl", value = BusEventEntityMapper.class)
    public BusEventEntityMapper busEventEntityMapperImpl(JdbcTemplate jdbcStorageJdbcTemplate,
        EasyEventTableGeneratorSupplier easyEventTableGeneratorSupplier) {
        return new BusEventEntityMapperImpl(jdbcStorageJdbcTemplate, easyEventTableGeneratorSupplier);
    }

    @Bean
    @ConditionalOnMissingBean
    public ShardingRouter shardingRouter(JdbcStorageProperties jdbcStorageProperties,
        @Autowired(required = false) IdGenerator idGenerator) {
        EventStorageShardingProperty shardingProperty = new EventStorageShardingProperty();
        shardingProperty.setTotalSharding(jdbcStorageProperties.getTable().getTotalSharding());
        return new DefaultShardingRouterImpl(shardingProperty, idGenerator);
    }

    @Bean
    public EasyEventTableGeneratorSupplier easyEventTableGeneratorSupplier(JdbcStorageProperties jdbcStorageProperties,
        ShardingRouter shardingRouter) {
        return new EasyEventTableGeneratorSupplier(jdbcStorageProperties.getTable().getPrefix(), shardingRouter);
    }

    @Bean
    @ConditionalOnMissingBean(type = "jdbcEventStorageService", value = EventStorageService.class)
    public EventStorageService jdbcEventStorageService(BusEventEntityMapper busEventEntityMapperImpl,
        Serializer serializer,
        @Autowired(required = false) IdGenerator idGenerator,
        EasyEventProperties easyEventProperties) {

        return new JdbcEventStorageServiceImpl(busEventEntityMapperImpl, serializer, idGenerator, easyEventProperties);
    }

}
