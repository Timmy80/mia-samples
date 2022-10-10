package com.github.timmy80.mia.samples.http2;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public class Http2ServerBootstrap {

	public static void main(String[] args) {
		int port = Integer.parseInt(System.getProperty("h2.port", "8443"));
		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
		
		LayoutComponentBuilder logfmtLayout = builder.newLayout("PatternLayout");
		logfmtLayout.addAttribute("pattern", "time=%d{ISO8601} thread=%t level=%level logger=%logger %msg%n");
		
		AppenderComponentBuilder console = builder.newAppender("stdout", "Console");
		console.add(logfmtLayout);
		builder.add(console);
		
		Level level = Level.getLevel(System.getProperty("h2.log.level", "DEBUG"));
		LoggerComponentBuilder loggerb = builder.newLogger("com.github.timmy80.mia.samples.http2", level);
		loggerb.add(builder.newAppenderRef("stdout"));
		loggerb.addAttribute("additivity", false);
		builder.add(loggerb);
		
		RootLoggerComponentBuilder rootLogger = builder.newRootLogger();
		rootLogger.add(builder.newAppenderRef("stdout"));
        builder.add(rootLogger);

		
		Configurator.initialize(builder.build());
		
		Logger logger = LogManager.getLogger(Http2ServerBootstrap.class);
		Http2ServerTask task = null;
        try {
            task = new Http2ServerTask("h2-server", port);
        } catch (IllegalArgumentException | CertificateException | SSLException e) {
            logger.fatal("Failed to instantiate Http2ServerTask.", e);
            System.exit(126);
        }
		
		task.start();
	}

}
