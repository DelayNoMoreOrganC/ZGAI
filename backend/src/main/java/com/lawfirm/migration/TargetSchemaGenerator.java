package com.lawfirm.migration;

import com.lawfirm.converter.EncryptConverter;
import com.lawfirm.util.CryptoUtil;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.SpringBeanContainer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

final class TargetSchemaGenerator {

    private TargetSchemaGenerator() {
    }

    static void updatePostgreSqlSchema(String url, String username, String password) {
        updateSchema("org.postgresql.Driver", url, username, password,
                "org.hibernate.dialect.PostgreSQLDialect", "zgaiMigrationTarget");
    }

    static void createH2CompatibilitySchema(String url) {
        updateSchema("org.h2.Driver", url, "sa", "",
                "org.hibernate.dialect.H2Dialect", "zgaiMigrationDryRun");
    }

    private static void updateSchema(String driverClassName, String url, String username, String password,
                                     String dialect, String persistenceUnitName) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setShowSql(false);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", dialect);
        properties.put("hibernate.temp.use_jdbc_metadata_defaults", "true");
        properties.put("hibernate.globally_quoted_identifiers", "false");
        properties.put("hibernate.implicit_naming_strategy",
                "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");
        properties.put("hibernate.physical_naming_strategy",
                "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy");

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        CryptoUtil cryptoUtil = new CryptoUtil();
        beanFactory.registerSingleton("cryptoUtil", cryptoUtil);
        beanFactory.registerSingleton("encryptConverter", new EncryptConverter(cryptoUtil));
        properties.put("hibernate.resource.beans.container", new SpringBeanContainer(beanFactory));
        factory.setPersistenceUnitName(persistenceUnitName);
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.lawfirm.entity");
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setJpaPropertyMap(properties);
        factory.setBeanFactory(beanFactory);
        factory.afterPropertiesSet();
        factory.destroy();
    }
}
