package seqd.netty

import io.netty.channel.{ 
  ChannelFuture, ChannelFutureListener,
  ChannelHandlerContext, SimpleChannelInboundHandler
}

class Handler(gen: seqd.Generator)
  extends SimpleChannelInboundHandler[Request] {
  def next(n: Int): Either[String, Array[Byte]] = {
    val out = new Array[Byte](n * 8)
    ((Right(out): Either[String, Array[Byte]]) /: (0 until n)) {
      case (acc @ Right(bytes), i) =>
        gen.next() match {
          case suc @ Right(id) =>
            val off = i * 8
            bytes(off)     = (id >> 56).toByte
            bytes(off + 1) = (id >> 46).toByte
            bytes(off + 2) = (id >> 40).toByte
            bytes(off + 3) = (id >> 32).toByte
            bytes(off + 4) = (id >> 24).toByte
            bytes(off + 5) = (id >> 16).toByte
            bytes(off + 6) = (id >> 8).toByte
            bytes(off + 7) = id.toByte
            acc
          case Left(msg) =>
            Left(msg)
        }
      case (err @ Left(_), _) =>
        err 
    }
  }

  override def channelRead0(
    ctx: ChannelHandlerContext, f: Request): Unit =
    f match {
      case valid: Valid =>
        next(valid.size)
          .fold(
            { err => ctx.channel.close() },
            { bytes =>
              ctx.writeAndFlush(bytes)
                 .addListener(new ChannelFutureListener {
                   def operationComplete(cf: ChannelFuture): Unit =
                     ctx.channel.close()
                 })
           })
          
      case Invalid =>
        ctx.channel.close()
    }
}
