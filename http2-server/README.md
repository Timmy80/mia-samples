# MiA Custom Http/2 server

This example shows you how to design a custom Netty-based http/2 server (also compatible with Http/1.1).

## Design

This sample uses MiA 3 stages design. In the following class diagram, you can see how the events are spread across the different levels.

```mermaid
classDiagram
    class Http2ServerTask{
        - ServerSocketChannel serverSocket
        + void eventStartTask()
        + void eventStopRequested()
    }
    class Http2Terminal{
        - Channel channel
	    - SslHandler sslHandler
        + eventSocketClosed(ChannelFuture f)
    }
    class HandshakingState {
        + void eventHanshakeComplete(Future<Channel> f);
    }
    class ConnectedStated {
        + void eventRequestComplete(DefaultFullHttpRequest request, Http2FrameStream h2Stream)
    }
    Http2ServerTask o--Http2Terminal
    Http2Terminal o-- HandshakingState : handShakingState
    Http2Terminal o-- ConnectedStated : connectedState
```

## Event sequences
```mermaid
sequenceDiagram
    participant SocketChannel
    participant Http2ServerTask
    participant Http2Terminal
    participant SslHandler
    participant Http2FrameHandler
    participant HandshakingState
    participant ConnectedState

    Note over SocketChannel,Http2ServerTask: Init Netty Pipeline (Netty Thread)
    SocketChannel ->>+ Http2ServerTask : initChannel(SocketChannel ch)
    Http2ServerTask ->>- SocketChannel : Pipeline + Http2Terminal
    Http2Terminal ->>+ Http2Terminal: nextState(handshakingState)
    Note over SocketChannel,ConnectedState: Events asynchronously executed by Http2ServerTask
    SslHandler -)+ HandshakingState : eventHandshakeComplete(f)
    HandshakingState -)- Http2Terminal : nextState(connectedState)
    par 
    Http2FrameHandler -)+ ConnectedState : eventRequestComplete(request, h2Stream)
    ConnectedState --)- Http2FrameHandler : writeAndFlush(response)
    end
    SocketChannel -) Http2Terminal : eventSocketClosed(f)

    
```

## How to run ?

```bash
# Run the server
mvn compile exec:java

# Send a request with curl
curl --insecure https://localhost:8443/HelloWorld
```