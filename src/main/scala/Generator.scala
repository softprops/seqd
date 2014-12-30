package seqd

object Generator {
  object Id {
    private val workerMask     = 0x000000000001F000L
    private val datacenterMask = 0x00000000003E0000L
    private val timestampMask  = 0xFFFFFFFFFFC00000L

    def unapply(l: Long): Option[(Long, Long, Long, Long)] =
      Some(
       (l & timestampMask) >> 22,
       (l & datacenterMask) >> 17,
       (l & workerMask) >> 12,
        l & sequenceMask)
  }

  case class State(lts: Long, seq: Long) {
    def next(timestamp: Long, tick: => Long): State =
      if (lts == timestamp)
        ((seq + 1) & sequenceMask) match {
          case 0  =>
            @annotation.tailrec
            def wind(ts: Long): Long =
              if (ts > lts) ts else wind(tick)
            State(wind(timestamp), 0L)
          case s  =>
            State(timestamp, s)
        } else State(timestamp, 0L)
  }

  val defaultTwepoch = 1288834974657L // Tue, 21 Mar 2006 20:50:14.000 GMT

  private[this] val workerIdBits = 5L
  private[this] val datacenterIdBits = 5L
  private[this] val maxWorkerId = -1L ^ (-1L << workerIdBits)
  private[this] val maxDatacenterId = -1L ^ (-1L << datacenterIdBits)
  private[this] val sequenceBits = 12L
  private[seqd] val workerIdShift = sequenceBits
  private[seqd] val datacenterIdShift = sequenceBits + workerIdBits
  private[seqd] val timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits
  private[seqd] val sequenceMask = -1L ^ (-1L << sequenceBits)

  def apply
   (twepoch: Long      = defaultTwepoch,
    workerId: Long     = 0,
    datacenterId: Long = 0,
    prev: State        = State(-1, 0),
    clock: Clock       = Clock.default): Either[String, Generator] =
    if (workerId > maxWorkerId || workerId < 0)
      Left(s"worker Id can't be greater than $maxWorkerId or less than 0")
    else if (datacenterId > maxDatacenterId || datacenterId < 0)
      Left(s"datacenter Id can't be greater than $maxDatacenterId or less than 0")
    else
      Right(new Generator(twepoch, workerId, datacenterId, prev, clock))
}

class Generator private[seqd]
 (twepoch: Long,
  workerId: Long,
  datacenterId: Long,
  prev: Generator.State,
  clock: Clock) {
  import Generator._
  private[this] var state = prev

  def next(): Either[String, Long] = synchronized {
    var timestamp = clock()
    if (timestamp < state.lts)
      Left(s"clock is moving backwards. Refusing to generate id for ${state.lts - timestamp} milliseconds")
    else {
      state = state.next(timestamp, clock())
      Right(
       ((state.lts - twepoch) << timestampLeftShift)
        | (datacenterId << datacenterIdShift)
        | (workerId << workerIdShift)
        | state.seq)
    }
  }
}
