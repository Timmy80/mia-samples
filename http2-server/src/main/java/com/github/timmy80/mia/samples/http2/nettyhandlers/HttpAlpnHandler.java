package com.github.timmy80.mia.samples.http2.nettyhandlers;

import com.github.timmy80.mia.samples.http2.Http2Terminal;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

public class HttpAlpnHandler extends ApplicationProtocolNegotiationHandler {
	private final Http2Terminal terminal;
	private final Http2Settings h2Settings;

	public HttpAlpnHandler(Http2Terminal terminal, Http2Settings h2Settings) {
		super(ApplicationProtocolNames.HTTP_1_1);
		this.terminal = terminal;
		this.h2Settings = h2Settings;
	}

	@Override
	protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
		// manage network trace
		ctx.pipeline().addLast(new LoggingHandler(LogLevel.TRACE));
		
	    if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
	        ctx.pipeline()
	        	.addLast(new LoggingHandler("com.github.timmy80.mia.samples.http2.LoggingHandler", LogLevel.DEBUG))
	        	.addLast(Http2FrameCodecBuilder.forServer()
	        			.autoAckPingFrame(true)
	        			.autoAckSettingsFrame(true)
	        			.initialSettings(h2Settings)
	        			.frameLogger(new Http2FrameLogger(LogLevel.DEBUG, "com.github.timmy80.mia.samples.http2.Http2FrameLogger"))
	        			.build())
	        	//.addLast(new LoggingHandler("com.github.timmy80.mia.samples.http2.LoggingHandler", LogLevel.DEBUG))
	        	.addLast(new Http2FrameHandler(terminal));
	        
	        terminal.runLater(terminal::eventHttp2PipelineConfigured);
	    }
	    else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
	    	ctx.pipeline()
	    		.addLast(new HttpRequestDecoder())
	    		.addLast(new HttpResponseEncoder())
	    		.addLast(new Http2FrameHandler(terminal));
	    }
	    else
	    	throw new IllegalStateException("Protocol: " + protocol + " not supported");
	}
}