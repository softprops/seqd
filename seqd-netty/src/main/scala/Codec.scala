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
