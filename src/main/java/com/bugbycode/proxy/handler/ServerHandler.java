package com.bugbycode.proxy.handler;

import java.util.LinkedList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.module.Authentication;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ServerHandler extends ChannelInboundHandlerAdapter {
	
	private final Logger logger = LogManager.getLogger(ServerHandler.class);
	
	private int loss_connect_time = 0;
	
	private String username;
	
	private String password;
	
	private ChannelGroup channelGroup;
	
	private EventLoopGroup remoteGroup;
	
	private Map<String,NettyClient> nettyClientMap;
	
	private Map<String, Channel> onlineAgentMap;
	
	private LinkedList<NettyClient> queue;
	
	public ServerHandler(ChannelGroup channelGroup, EventLoopGroup remoteGroup, 
			Map<String, NettyClient> nettyClientMap,
			Map<String, Channel> onlineAgentMap) {
		this.queue = new LinkedList<NettyClient>();
		this.channelGroup = channelGroup;
		this.remoteGroup = remoteGroup;
		this.nettyClientMap = nettyClientMap;
		this.onlineAgentMap = onlineAgentMap;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		logger.info("Agent connection...");
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("Agent connection closed... " + username);
		onlineAgentMap.remove(username);
		channelGroup.remove(ctx.channel());
		while(!queue.isEmpty()) {
			queue.removeFirst().close();
		}
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		loss_connect_time = 0;
		Channel channel = ctx.channel();
		Message message = (Message)msg;
		int type = message.getType();
		Object data = message.getData();
		String token = message.getToken();
		if(type == MessageCode.AUTH) {
			if(data == null || !(data instanceof Authentication)) {
				ctx.close();
				return;
			}
			Authentication authInfo = (Authentication)data;
			
			String username = authInfo.getUsername();
			String password = authInfo.getPassword();
			
			Channel clientAgent = onlineAgentMap.get(username);
			if(!(clientAgent == null)) {
				message.setType(MessageCode.AUTH_ERROR);
				message.setData(null);
				channel.writeAndFlush(message);
				ctx.close();
				return;
			}
			
			this.username = username;
			this.password = password;
			
			message.setType(MessageCode.AUTH_SUCCESS);
			message.setData(null);
			
			onlineAgentMap.put(username, channel);
			channelGroup.add(channel);
			return;
		}
		
		channel = channelGroup.find(channel.id());
		if(channel == null) {
			ctx.close();
			return;
		}
		
		if(type == MessageCode.HEARTBEAT) {
			//
			System.out.println(message);
			return;
		}
		
		// Connection
		if(type == MessageCode.CONNECTION) {
			if(!(data instanceof ConnectionInfo)) {
				ctx.close();
				return;
			}
			
			NettyClient client = new NettyClient(message, channel, remoteGroup, 
					nettyClientMap);
			client.connection();
			queue.add(client);
			return;
		}
		
		if(type == MessageCode.CLOSE_CONNECTION) {
			NettyClient client = nettyClientMap.get(token);
			if(client != null) {
				client.close();
			}
			return;
		}
		
		if(type == MessageCode.TRANSFER_DATA) {
			NettyClient client = nettyClientMap.get(token);
			if(client == null) {
				message.setToken(token);
				message.setType(MessageCode.CLOSE_CONNECTION);
				message.setData(null);
				channel.writeAndFlush(message);
				return;
			}
			byte[] buffer = (byte[]) data;
			ByteBuf buff = ctx.alloc().buffer(buffer.length);
			buff.writeBytes(buffer);
			client.writeAndFlush(buff);
		}
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				loss_connect_time++;
				logger.info("Read heartbeat timeout.");
				if (loss_connect_time > 2) {
					logger.info("Channel timeout.");
					ctx.channel().close();
				}
			} else {
				super.userEventTriggered(ctx, evt);
			}
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
		logger.error(cause.getMessage());
	}
}
