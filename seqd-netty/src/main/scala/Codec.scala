package seqd.netty

import io.netty.handler.codec.ReplayingDecoder
import io.netty.channel.{ Channel, ChannelHandlerContext, ChannelFuture, ChannelFutureListener }
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import scala.util.{ Left, Right }
import java.util.{ List => JList }
/** same as https://github.com/bmizerany/noeqd#protocol */
sealed trait Request
trait Valid extends Request {
  def size: Int
}
case class Authenticated(token: String, size: Int) extends Valid
case class Anonymous(size: Int) extends Valid
case object Invalid extends Request

// http://netty.io/4.0/api/io/netty/handler/codec/ReplayingDecoder.html
// http://biasedbit.com/netty-tutorial-replaying-decoder/
object Decoder {
  sealed trait State
  case object Init extends State
  case object ReadToken extends State
  case class TokenSize(len: Int) extends State
  case class Authenticated(credential: String) extends State
}
/** https://github.com/brunodecarvalho/netty-tutorials/blob/master/customcodecs/src/main/java/com/biasedbit/nettytutorials/customcodecs/common/Decoder.java */

class Decoder(auth: Option[String] = None)
  extends ReplayingDecoder[Decoder.State](Decoder.Init) {

  def reset() = checkpoint(Decoder.Init)

  protected def decode(
    ctx: ChannelHandlerContext, in: ByteBuf, out: JList[Object]): Unit =
    auth match {
      case Some(token) =>
        state match {
          case Decoder.Init =>
            if (in.readByte() != 0) out.add(Invalid)
            else checkpoint(Decoder.ReadToken)
          case Decoder.ReadToken =>
            val size = in.readByte
            if (size < token.length) out.add(Invalid)
            else checkpoint(Decoder.TokenSize(size))
          case Decoder.TokenSize(size) =>
            val bytes = new Array[Byte](size)
            in.readBytes(bytes)
            val credential = new String(bytes)
            if (credential != token) out.add(Invalid)
            else checkpoint(Decoder.Authenticated(credential))
          case Decoder.Authenticated(credential) =>
            try out.add(Authenticated(credential, in.readInt))
            finally reset()
        }
      case _ =>
        try out.add(Anonymous(in.readInt)) finally reset()
    }
}

/*class Encoder extends SimpleChannelDownstreamHandler {
  override def writeRequested(ctx: ChannelHandlerContext, ev: MessageEvent): Unit =
    msg match {
      case 
    }
}*/

import io.netty.channel._
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
