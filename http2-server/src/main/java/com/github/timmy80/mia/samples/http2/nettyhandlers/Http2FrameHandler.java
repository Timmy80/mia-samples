package com.github.timmy80.mia.samples.http2.nettyhandlers;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.timmy80.mia.samples.http2.Http2Terminal;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameStream;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.HttpConversionUtil;

//curl -v --insecure --header "Content-Type: application/json" --request "POST" --data '{"username": "toto", "id": "12345" }' https://localhost:8443/toto
public class Http2FrameHandler extends ChannelInboundHandlerAdapter {

	protected static final Logger logger = LogManager.getLogger(Http2FrameHandler.class);
    protected static final HttpVersion HTTP2 = HttpVersion.valueOf("HTTP/2.0");
    
    protected final Http2Terminal terminal;
    protected final Http2Terminal.ConnectedStated httpTermState;
    protected final Channel channel;
	
	protected Map<Integer, Http2FrameStream> streams = new HashMap<Integer, Http2FrameStream>();
	protected Map<Integer, DefaultFullHttpRequest> pendingRequests = new HashMap<Integer, DefaultFullHttpRequest>();
	
	public Http2FrameHandler(Http2Terminal terminal) {
		this.terminal = terminal;
		this.httpTermState = terminal.connectedState();
		this.channel = terminal.channel();
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		/**************
		 * HTTP2 frames
		 **************/
	    if (msg instanceof Http2HeadersFrame)
	        processReceivedHeaderFrame(ctx, (Http2HeadersFrame) msg);
	    else if(msg instanceof Http2DataFrame)
	    	processReceivedDataFrame(ctx, (Http2DataFrame) msg);
	    /****************
		 * HTTP1.1 frames
		 ****************/
	    else if (msg instanceof HttpRequest)
	        this.processReceivedH1Request(ctx, (HttpRequest) msg);
	    else if (msg instanceof HttpContent)
	    	this.processReceivedH1Content(ctx, (HttpContent) msg);
	    else
	        super.channelRead(ctx, msg);
	}

	/****************
	 * HTTP2 events
	 ****************/
	public void processReceivedHeaderFrame(ChannelHandlerContext ctx, Http2HeadersFrame msg) {
		logger.trace("Header frame received: {}", msg);
		
		Http2FrameStream stream = msg.stream();
		DefaultFullHttpRequest theRequest = pendingRequests.get(stream.id());
		
		if(theRequest == null) {
			theRequest = new DefaultFullHttpRequest(HTTP2, HttpMethod.valueOf(msg.headers().method().toString()), msg.headers().path().toString());
			pendingRequests.put(stream.id(), theRequest);
			streams.put(stream.id(), stream);
		}
		
		try {
			HttpConversionUtil.addHttp2ToHttpHeaders(msg.stream().id(), msg.headers(), theRequest, false);
		} catch (Http2Exception e) {
			logger.throwing(Level.ERROR, e);
		}
		
		if(msg.isEndStream())
			processCompleteRequest(ctx, stream.id());
	}

	public void processReceivedDataFrame(ChannelHandlerContext ctx, Http2DataFrame msg) {
		logger.trace("Data frame received: {}", msg);

		Http2FrameStream stream = msg.stream();
		pendingRequests.get(stream.id()).content().writeBytes(msg.content());
		
		if(msg.isEndStream())
			processCompleteRequest(ctx, stream.id());
	}

	/****************
	 * HTTP1.1 events
	 ****************/
	public void processReceivedH1Request(ChannelHandlerContext ctx, HttpRequest msg) {
		logger.trace("HttpRequest received: {}", msg);
		
        if (HttpUtil.is100ContinueExpected(msg)) {
        	FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER);
            ctx.write(response);
            return;
        }
		
		DefaultFullHttpRequest theRequest = new DefaultFullHttpRequest(msg.protocolVersion(), msg.method(), msg.uri());
		pendingRequests.put(0, theRequest);
		theRequest.headers().add(msg.headers());
		
		if(HttpUtil.getContentLength(msg, 0L) == 0)
			processCompleteRequest(ctx, 0);
	}
	
	public void processReceivedH1Content(ChannelHandlerContext ctx, HttpContent msg) {
		logger.trace("HttpContent received: {}", msg);
		
		DefaultFullHttpRequest pendingRequest = pendingRequests.get(0);
		if(pendingRequest == null)
			return;
			
		pendingRequest.content().writeBytes(msg.content());
		
		if (msg instanceof LastHttpContent)
			processCompleteRequest(ctx, 0);
	}
	
	/**************
	 * GLOBAL event
	 **************/
	public void processCompleteRequest(ChannelHandlerContext ctx, int streamId) {
		
		DefaultFullHttpRequest theRequest = pendingRequests.remove(streamId);
		
		if(theRequest.content() != null && logger.isDebugEnabled()) {
			byte[] buf = theRequest.content().array();
			logger.debug("Request:\n{}\n\n{}", theRequest, new String(buf));
		}
		else
			logger.debug("Request:\n{}", theRequest);
		    
		Http2FrameStream h2Stream = null;
		if(HTTP2.equals(theRequest.protocolVersion()))
		    h2Stream = streams.remove(streamId);
		
		httpTermState.runLater(httpTermState::eventRequestComplete, theRequest, h2Stream);
	}

}

