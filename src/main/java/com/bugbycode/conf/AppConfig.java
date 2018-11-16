package com.bugbycode.conf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bugbycode.client.startup.NettyClient;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

@Configuration
public class AppConfig {
	
	public static final int WORK_THREAD_NUMBER = 100;
	
	public static final int MAX_CLIENT_NUMBER = 500;
	
	@Bean("channelGroup")
	public ChannelGroup getChannelGroup() {
		return new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	}
	
	@Bean
	public Map<String,NettyClient> nettyClientMap(){
		return Collections.synchronizedMap(new HashMap<String,NettyClient>());
	}
	
	@Bean
	public NioEventLoopGroup remoteGroup() {
		return new NioEventLoopGroup(MAX_CLIENT_NUMBER);
	}
	
	@Bean
	public Map<String, Channel> onlineAgentMap(){
		return Collections.synchronizedMap(new HashMap<String,Channel>());
	}
}
