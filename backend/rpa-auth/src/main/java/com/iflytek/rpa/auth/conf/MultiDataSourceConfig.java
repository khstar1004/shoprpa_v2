package com.iflytek.rpa.auth.conf;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.iflytek.rpa.auth.sp.casdoor.dao.MarketUserDao;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * 다중데이터매칭유형
 * 에서 casdoor 모듈방식아래
 *
 * @author Auto Generated
 */
@Configuration
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor", matchIfMissing = false)
public class MultiDataSourceConfig {

    // ========== RPA 데이터매칭 ==========
    @Value("${spring.datasource.url}")
    private String rpaUrl;

    @Value("${spring.datasource.username}")
    private String rpaUsername;

    @Value("${spring.datasource.password}")
    private String rpaPassword;

    @Value("${spring.datasource.driverClassName:com.mysql.cj.jdbc.Driver}")
    private String rpaDriverClassName;

    // ========== Casdoor 데이터매칭 ==========
    @Value("${spring.datasource.casdoor.url}")
    private String casdoorUrl;

    @Value("${spring.datasource.casdoor.username}")
    private String casdoorUsername;

    @Value("${spring.datasource.casdoor.password}")
    private String casdoorPassword;

    @Value("${spring.datasource.casdoor.driverClassName:com.mysql.cj.jdbc.Driver}")
    private String casdoorDriverClassName;

    /**
     * RPA 데이터(데이터)
     */
    @Bean(name = "rpaDataSource")
    @Primary
    public DataSource rpaDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(rpaUrl);
        dataSource.setUsername(rpaUsername);
        dataSource.setPassword(rpaPassword);
        dataSource.setDriverClassName(rpaDriverClassName);
        // Druid 연결매칭
        dataSource.setInitialSize(1);
        dataSource.setMinIdle(3);
        dataSource.setMaxActive(20);
        dataSource.setMaxWait(60000);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(30000);
        dataSource.setValidationQuery("select 'x'");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);
        try {
            dataSource.setFilters("stat,wall,slf4j");
        } catch (Exception e) {
            // ignore
        }
        dataSource.setConnectionProperties(
                "druid.stat.mergeSql=true;druid.stat.slowSqlMillis=2000;druid.stat.logSlowSql=true;druid.stat.enabled=true");
        return dataSource;
    }

    /**
     * Casdoor 데이터
     */
    @Bean(name = "casdoorDataSource")
    public DataSource casdoorDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(casdoorUrl);
        dataSource.setUsername(casdoorUsername);
        dataSource.setPassword(casdoorPassword);
        dataSource.setDriverClassName(casdoorDriverClassName);
        // Druid 연결매칭
        dataSource.setInitialSize(1);
        dataSource.setMinIdle(3);
        dataSource.setMaxActive(20);
        dataSource.setMaxWait(60000);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(30000);
        dataSource.setValidationQuery("select 'x'");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);
        try {
            dataSource.setFilters("stat,wall,slf4j");
        } catch (Exception e) {
            // ignore
        }
        dataSource.setConnectionProperties(
                "druid.stat.mergeSql=true;druid.stat.slowSqlMillis=2000;druid.stat.logSlowSql=true;druid.stat.enabled=true");
        return dataSource;
    }

    /**
     * RPA SqlSessionFactory
     */
    @Bean(name = "rpaSqlSessionFactory")
    @Primary
    public SqlSessionFactory rpaSqlSessionFactory(@Qualifier("rpaDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean sessionFactory = new MybatisSqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        // Mapper XML파일에서Java코드디렉터리아래, 후에서classpath의패키지경로아래
        sessionFactory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:/com/iflytek/rpa/auth/**/*Dao.xml"));
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);
        sessionFactory.setConfiguration(configuration);
        return sessionFactory.getObject();
    }

    /**
     * Casdoor SqlSessionFactory
     */
    @Bean(name = "casdoorSqlSessionFactory")
    public SqlSessionFactory casdoorSqlSessionFactory(@Qualifier("casdoorDataSource") DataSource dataSource)
            throws Exception {
        MybatisSqlSessionFactoryBean sessionFactory = new MybatisSqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        // Mapper XML파일에서Java코드디렉터리아래, 후에서classpath의패키지경로아래
        sessionFactory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:/com/iflytek/rpa/auth/**/*Dao.xml"));
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);
        sessionFactory.setConfiguration(configuration);
        return sessionFactory.getObject();
    }

    /**
     * RPA SqlSessionTemplate
     */
    @Bean(name = "rpaSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate rpaSqlSessionTemplate(@Qualifier("rpaSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * Casdoor SqlSessionTemplate
     */
    @Bean(name = "casdoorSqlSessionTemplate")
    public SqlSessionTemplate casdoorSqlSessionTemplate(
            @Qualifier("casdoorSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * RPA 서비스관리관리기기
     */
    @Bean(name = "rpaTransactionManager")
    @Primary
    public DataSourceTransactionManager rpaTransactionManager(@Qualifier("rpaDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * Casdoor 서비스관리관리기기
     */
    @Bean(name = "casdoorTransactionManager")
    public DataSourceTransactionManager casdoorTransactionManager(@Qualifier("casdoorDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * Casdoor닫기DAO의MapperScan매칭
     * 지정사용 casdoor 데이터(방문astron-agent-casdoor-mysql)
     * 설명 com.iflytek.rpa.auth.sp.casdoor.dao 패키지,  MarketUserDao 단일매칭까지 RPA 데이터
     */
    @Configuration
    @MapperScan(
            basePackages = "com.iflytek.rpa.auth.sp.casdoor.dao",
            sqlSessionFactoryRef = "casdoorSqlSessionFactory",
            sqlSessionTemplateRef = "casdoorSqlSessionTemplate")
    static class CasdoorDaoMapperScanConfig {
        // 빈매칭유형, 사용 @MapperScan 비고해제
    }

    /**
     * MarketUserDao의단일매칭
     * 지정사용 rpa 데이터(방문shoprpa-mysql)
     * 비고: MarketUserDao 에서 com.iflytek.rpa.auth.sp.casdoor.dao 패키지아래, 필요사용 RPA 데이터
     * 원인필요회원가입, 및 CasdoorDaoMapperScanConfig 
     */
    @Bean
    public MapperFactoryBean<MarketUserDao> marketUserDao(
            @Qualifier("rpaSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<MarketUserDao> factoryBean = new MapperFactoryBean<>(MarketUserDao.class);
        factoryBean.setSqlSessionFactory(sqlSessionFactory);
        return factoryBean;
    }

    /**
     * RPA서비스데이터베이스DAO의MapperScan매칭
     * 지정사용 rpa 데이터(방문shoprpa-mysql)
     */
    @Configuration
    @MapperScan(
            basePackages = {
                    "com.iflytek.rpa.auth.auditRecord.dao",
                    "com.iflytek.rpa.auth.blacklist.dao",
                    "com.iflytek.rpa.auth.dataPreheater.dao"
            },
            sqlSessionFactoryRef = "rpaSqlSessionFactory",
            sqlSessionTemplateRef = "rpaSqlSessionTemplate")
    static class RpaBusinessDaoMapperScanConfig {
        // 빈매칭유형, 사용 @MapperScan 비고해제
    }

    /**
     * 데이터분매칭설명: 
     * 1. CasdoorUserDao, CasdoorTenantDao, CasdoorRoleDao, CasdoorGroupDao 사용 casdoor 데이터(방문astron-agent-casdoor-mysql)
     * 2. 으로아래DAO사용 rpa 데이터(방문shoprpa-mysql):
     *    - MarketUserDao(방문app_market_user테이블)
     *    - AuditRecordDao(방문audit_record테이블)
     *    - UserBlacklistDao(방문user_blacklist테이블)
     *    - SharedVarKeyTenantDao(방문shared_var_key_tenant테이블)
     *    - AppMarketUserDao(방문app_market_user테이블)
     *    - AppMarketDao(방문app_market테이블)
     *    - AppMarketClassificationDao(방문app_market_classification테이블)
     * 3. 데이터의조회(예getMarketUserList필요시조회개데이터), 요청에서Service분조회후병합결과
     */
}
