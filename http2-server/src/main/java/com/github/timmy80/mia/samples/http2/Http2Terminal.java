package com.github.timmy80.mia.samples.http2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.timmy80.mia.core.LogFmt;
import com.github.timmy80.mia.core.Terminal;
import com.github.timmy80.mia.core.TerminalState;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2FrameStream;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;

public class Http2Terminal extends Terminal<Http2ServerTask> {

	public class HandshakingState extends TerminalState {

		@Override
		protected void eventEntry() {
		}

		public void eventHanshakeComplete(Future<Channel> f) {
			logger.info("{}", new LogFmt()
					.append("event", "HanshakeComplete(")
					.append("remote", channel.remoteAddress())
					.append("protocol", sslHandler.applicationProtocol()));
			nextState(connectedState);
		}
	}

	public class ConnectedStated extends TerminalState {

		@Override
		protected void eventEntry() {
			// TODO Auto-generated method stub

		}

		public void eventRequestComplete(DefaultFullHttpRequest request, Http2FrameStream h2Stream) {
			if(h2Stream != null) {
				DefaultHttp2HeadersFrame response = new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().status(HttpResponseStatus.OK.codeAsText()), true);
				response.stream(h2Stream);
				channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
			}
			else {
				DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(),HttpResponseStatus.OK);
				channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
			}
		}

	}

	private final Logger logger = LogManager.getLogger(this.getClass());
	private final Channel channel;
	private final SslHandler sslHandler;
	private final HandshakingState handShakingState = new HandshakingState();
	private final ConnectedStated connectedState = new ConnectedStated();

	public Http2Terminal(Http2ServerTask task, Channel channel, SslHandler sslHandler) {
		super(task);
		this.channel = channel;
		this.sslHandler = sslHandler;
		
		this.listenFuture(channel.closeFuture(), this::eventSocketClosed);
		
		if(sslHandler != null) {
			this.nextState(handShakingState);
			this.listenFuture(this.sslHandler.handshakeFuture(), handShakingState::eventHanshakeComplete);
		}
		else
			nextState(connectedState);
	}

	public Channel channel() {
		return channel;
	}

	public void eventHttp2PipelineConfigured() {

	}
	
	public void eventSocketClosed(ChannelFuture f) {
		terminate();
	}

	public HandshakingState handShakingState() {
		return handShakingState;
	}

	public ConnectedStated connectedState() {
		return connectedState;
	}

}
