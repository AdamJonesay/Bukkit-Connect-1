package lilypad.bukkit.connect.injector;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.net.InetSocketAddress;
import java.lang.reflect.Method;
import java.util.List;

import lilypad.bukkit.connect.util.ReflectionUtils;

import org.bukkit.Server;

public class NettyInjector {

	@SuppressWarnings("unchecked")
	public static int injectAndFindPort(Server server, NettyInjectHandler handler) throws Exception {
		Method serverGetHandle = server.getClass().getDeclaredMethod("getServer");
		Object minecraftServer = serverGetHandle.invoke(server);
		// Get Server Connection
		Method serverConnectionMethod = null;
		for(Method method : minecraftServer.getClass().getSuperclass().getDeclaredMethods()) {
			if(!method.getReturnType().getSimpleName().equals("ServerConnection")) {
				continue;
			}
			serverConnectionMethod = method;
			break;
		}
		Object serverConnection = serverConnectionMethod.invoke(minecraftServer);
		// Get ChannelFuture List // TODO find the field dynamically
		List<ChannelFuture> channelFutureList = ReflectionUtils.getPrivateField(serverConnection.getClass(), serverConnection, List.class, "g");
		// Iterate ChannelFutures
		int commonPort = 0;
		for(ChannelFuture channelFuture : channelFutureList) {
			// Get ChannelPipeline
			ChannelPipeline channelPipeline = channelFuture.channel().pipeline();
			// Get ServerBootstrapAcceptor
			ChannelHandler serverBootstrapAcceptor = channelPipeline.first();
			// Get Old ChildHandler
			ChannelInitializer<SocketChannel> oldChildHandler = ReflectionUtils.getPrivateField(serverBootstrapAcceptor.getClass(), serverBootstrapAcceptor, ChannelInitializer.class, "childHandler");
			// Set New ChildHandler
			ReflectionUtils.setFinalField(serverBootstrapAcceptor.getClass(), serverBootstrapAcceptor, "childHandler", new NettyChannelInitializer(handler, oldChildHandler));
			// Update Common Port
			commonPort = ((InetSocketAddress) channelFuture.channel().localAddress()).getPort();
		}
		return commonPort;
	}
	
}
