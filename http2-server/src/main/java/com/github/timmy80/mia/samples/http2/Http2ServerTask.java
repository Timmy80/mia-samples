package com.github.timmy80.mia.samples.http2;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.timmy80.mia.core.LogFmt;
import com.github.timmy80.mia.core.Task;
import com.github.timmy80.mia.samples.http2.nettyhandlers.HttpAlpnHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class Http2ServerTask extends Task {
	
	public final Http2ServerTask h2Task = this;
	public final int port;
	public final Logger logger;
	public final Http2Settings serverSettings;
	public final ApplicationProtocolConfig alpnCfg;
	public final SelfSignedCertificate ssc;
	public final SslContext sslContext;
	
	private ServerSocketChannel serverSocket;

	public Http2ServerTask(String name, int port) throws IllegalArgumentException, CertificateException, SSLException {
		super(name);
		this.port = port;
		logger = LogManager.getLogger(this.getClass());
		serverSettings = Http2Settings.defaultSettings();
		
		alpnCfg = new ApplicationProtocolConfig(
	    		Protocol.ALPN,
	    		SelectorFailureBehavior.NO_ADVERTISE, //SelectorFailureBehavior.FATAL_ALERT,
	    		SelectedListenerFailureBehavior.ACCEPT,
	    		ApplicationProtocolNames.HTTP_2,
	    		ApplicationProtocolNames.HTTP_1_1);
		

		ssc = new SelfSignedCertificate();
		sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
				.applicationProtocolConfig(alpnCfg)
				.build();
	}

	@Override
	public void eventStartTask() {
		try {
			serverSocket = openServerSocket("0.0.0.0", port, new ChannelInitializer<SocketChannel>() {
				
				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					
					SslHandler sslHandler = new SslHandler(sslContext.newEngine(ch.alloc()));
					
					ch.pipeline().addLast(sslHandler);
					ch.pipeline().addLast(new HttpAlpnHandler(new Http2Terminal(h2Task, ch, null), serverSettings));
				}
			});
		} catch(InterruptedException e) {
			logger.fatal("{}", new LogFmt().append("error", "Interrupted on server socket open"));
			getAppCtx().stop();
		}
	}
	
	@Override
	protected void eventStopRequested() {
		serverSocket.close();
	}

}
