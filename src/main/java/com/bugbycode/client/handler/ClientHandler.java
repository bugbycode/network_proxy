package com.bugbycode.client.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private final Logger logger = LogManager.getLogger(ClientHandler.class);
	
	private Channel serverChannel;
	
	private String token;
	
	private NettyClient client;
	
	public ClientHandler(Channel serverChannel,String token,NettyClient client) {
		this.serverChannel = serverChannel;
		this.token = token;
		this.client = client;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		Message message = new Message(token, MessageCode.TRANSFER_DATA, data);
		serverChannel.writeAndFlush(message);
	}

	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
		super.channelInactive(ctx);
		client.close(true);
	}
	
	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
		logger.error(cause.getMessage());
		ctx.channel().close();
    }
}
