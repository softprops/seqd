package seqd

trait Clock {
  def apply(): Long
}

object Clock {
  val default: Clock =
    new Clock {
      def apply() = System.currentTimeMillis
    }
}
