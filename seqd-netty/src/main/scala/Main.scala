package seqd.netty

object Main {
  def main(args: Array[String]) = {
    seqd.Generator().fold(sys.error, { g =>
      Server(g).start()
    })
  }
}
