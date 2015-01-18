package seqd.netty

import seqd.Generator
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel

case class Server
 (gen: Generator,
  port: Int             = 8080,
  token: Option[String] = None) {
  private[this] lazy val bosses = new NioEventLoopGroup(1)
  private[this] lazy val workers = new NioEventLoopGroup()

  def start() {
    new ServerBootstrap()
      .group(bosses, workers)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new ChannelInitializer[SocketChannel] {
        def initChannel(ch: SocketChannel) {
          ch.pipeline.addLast(
            "dec", new Decoder(token))
            .addLast("hand", new Handler(gen))
        }
      }).bind(port).sync().channel().closeFuture().sync()
  }

  def close() {
    bosses.shutdownGracefully()
    workers.shutdownGracefully()
  }
}
