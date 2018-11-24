package com.bugbycode.proxy.initializer;

import java.util.Map;

import javax.net.ssl.SSLEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import com.bugbycode.config.HandlerConst;
import com.bugbycode.config.IdleConfig;
import com.bugbycode.handler.MessageDecoder;
import com.bugbycode.handler.MessageEncoder;
import com.bugbycode.proxy.handler.ServerHandler;
import com.util.ssl.SSLContextUtil;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

@Configuration
@Service("serverChannelInitializer")
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	@Autowired
	private ChannelGroup channelGroup;
	
	@Autowired
	private EventLoopGroup remoteGroup;
	
	@Autowired
	private Map<String, Channel> onlineAgentMap;
	
	@Value("${spring.keystore.path}")
	private String keystorePath;
	
	@Value("${spring.keystore.password}")
	private String keystorePassword;
	
	private SslContext context;
	
	public ServerChannelInitializer() {
		
	}
	
	@Override
	protected void initChannel(SocketChannel sc) throws Exception {
		sc.config().setAllocator(UnpooledByteBufAllocator.DEFAULT);
		ChannelPipeline p = sc.pipeline();
		/*
		if(this.context == null) {
			this.context = SSLContextUtil.getServerContext(keystorePath, keystorePassword);
		}
		
		SSLEngine engine = context.newEngine(sc.alloc());
		engine.setUseClientMode(false);
		p.addLast(new SslHandler(engine));
		*/
		p.addLast(
				new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT, IdleConfig.WRITE_IDEL_TIME_OUT, IdleConfig.ALL_IDEL_TIME_OUT),
				new MessageDecoder(HandlerConst.MAX_FRAME_LENGTH, HandlerConst.LENGTH_FIELD_OFFSET, 
						HandlerConst.LENGTH_FIELD_LENGTH, HandlerConst.LENGTH_AD_JUSTMENT, 
						HandlerConst.INITIAL_BYTES_TO_STRIP),
				new MessageEncoder(),
				new ServerHandler(channelGroup, remoteGroup, onlineAgentMap)
		);
	}
}
