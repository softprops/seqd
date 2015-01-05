package seqd

trait Clock {
  def apply(): Long
}

object Clock {
  def apply(gen: => Long): Clock =
    new Clock {
      def apply() = gen
    }
  val default: Clock =
    apply(System.currentTimeMillis)
}
