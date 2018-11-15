package com.bugbycode.client.startup;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.handler.ClientHandler;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient {
	
	private final Logger logger = LogManager.getLogger(NettyClient.class);
	
	private ChannelFuture future;
	
	private Bootstrap remoteClient;
	
	private String token;
	
	private Channel serverChannel;
	
	private EventLoopGroup remoteGroup;
	
	private Map<String,NettyClient> nettyClientMap;
	
	private String host;
	
	private int port;
	
	private ConnectionInfo conn;

	public NettyClient(Message msg, Channel serverChannel,
			EventLoopGroup remoteGroup,Map<String,NettyClient> nettyClientMap) {
		this.remoteClient = new Bootstrap();
		this.token = msg.getToken();
		this.serverChannel = serverChannel;
		this.conn = (ConnectionInfo) msg.getData();
		this.remoteGroup = remoteGroup;
		this.nettyClientMap = nettyClientMap;
		this.nettyClientMap.put(token, this);
	}
	
	public void connection() {
		host = conn.getHost();
		port = conn.getPort();
		
		this.remoteClient.group(remoteGroup).channel(NioSocketChannel.class);
		this.remoteClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000);
		this.remoteClient.option(ChannelOption.TCP_NODELAY, true);
		this.remoteClient.option(ChannelOption.SO_KEEPALIVE, true);
		this.remoteClient.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new ClientHandler(serverChannel,token,NettyClient.this));
			}
		});
		
		future = this.remoteClient.connect(host, port).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				Message message = new Message(token, MessageCode.CONNECTION_SUCCESS, null);
				if(future.isSuccess()) {
					logger.info("Connection to " + host + ":" + port + " successfully.");
					message.setType(MessageCode.CONNECTION_SUCCESS);
					serverChannel.writeAndFlush(message);
				}else {
					logger.info("Connection to " + host + ":" + port + " failed.");
					message.setType(MessageCode.CONNECTION_ERROR);
					serverChannel.writeAndFlush(message);
					close();
				}
			}
		});
	}
	
	public void writeAndFlush(Object msg) {
		if(future == null) {
			return;
		}
		future.channel().writeAndFlush(msg);
	}
	
	public void close() {
		
		this.nettyClientMap.remove(token);
		
		Message message = new Message(token, MessageCode.CLOSE_CONNECTION, null);
		serverChannel.writeAndFlush(message);
		
		if(future == null) {
			return;
		}
		future.channel().close();
		
		logger.info("Disconnection to " + host + ":" + port + " .");
	}
}