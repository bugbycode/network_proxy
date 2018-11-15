package com.bugbycode.proxy.initializer;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.config.HandlerConst;
import com.bugbycode.config.IdleConfig;
import com.bugbycode.handler.MessageDecoder;
import com.bugbycode.handler.MessageEncoder;
import com.bugbycode.proxy.handler.ServerHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

@Service("serverChannelInitializer")
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	@Autowired
	private ChannelGroup channelGroup;
	
	@Autowired
	private EventLoopGroup remoteGroup;
	
	@Autowired
	private Map<String,NettyClient> nettyClientMap;
	
	@Autowired
	private Map<String, Channel> onlineAgentMap;
	
	@Override
	protected void initChannel(SocketChannel sc) throws Exception {
		ChannelPipeline p = sc.pipeline();
		p.addLast(
				new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT, IdleConfig.WRITE_IDEL_TIME_OUT, IdleConfig.ALL_IDEL_TIME_OUT),
				new MessageDecoder(HandlerConst.MAX_FRAME_LENGTH, HandlerConst.LENGTH_FIELD_OFFSET, 
						HandlerConst.LENGTH_FIELD_LENGTH, HandlerConst.LENGTH_AD_JUSTMENT, 
						HandlerConst.INITIAL_BYTES_TO_STRIP),
				new MessageEncoder(),
				new ServerHandler(channelGroup, remoteGroup, nettyClientMap, onlineAgentMap)
		);
	}
}
