package com.bugbycode.proxy.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.conf.AppConfig;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class StartupRunnable implements Runnable {

	private final Logger logger = LogManager.getLogger(StartupRunnable.class);
	
	private int serverPort; 
	
	private int so_backlog;
	
	private ChannelHandler serverChannelInitializer;
	
	private EventLoopGroup boss;
	
	private EventLoopGroup worker;
	
	public StartupRunnable(int serverPort, int so_backlog, ChannelHandler serverChannelInitializer) {
		this.serverPort = serverPort;
		this.so_backlog = so_backlog;
		this.serverChannelInitializer = serverChannelInitializer;
	}

	@Override
	public void run() {
		
		boss = new NioEventLoopGroup(AppConfig.WORK_THREAD_NUMBER);
		worker = new NioEventLoopGroup(AppConfig.WORK_THREAD_NUMBER);
		ServerBootstrap bootstrap = new ServerBootstrap();
		
		bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, so_backlog)
				.option(ChannelOption.TCP_NODELAY, true)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childHandler(serverChannelInitializer);
		
		bootstrap.bind(serverPort).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					logger.info("Proxy server startup successfully, port " + serverPort + "......");
				} else {
					logger.info("Proxy server startup failed, port " + serverPort + "......");
					close();
				}
			}
		});
	}

	public void close() {
		
		if(boss != null) {
			boss.shutdownGracefully();
		}
		
		if(worker != null) {
			worker.shutdownGracefully();
		}
		
		logger.info("Proxy server shutdown, port " + serverPort + "......");
	}
}
