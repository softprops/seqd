package seqd

object Generator {
  case class State(lts: Long, seq: Long)

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
 (twepoch: Long, workerId: Long, datacenterId: Long, prev: Generator.State, clock: Clock) {
  import Generator._
  private[this] var state = prev

  def next(): Either[String, Long] = synchronized {
    var timestamp = clock()
    if (timestamp < state.lts)
      Left(s"clock is moving backwards. Refusing to generate id for ${state.lts - timestamp} milliseconds")
    else {
      state = if (state.lts == timestamp)
        ((state.seq + 1) & sequenceMask) match {
          case 0  =>
            @annotation.tailrec
            def tick(ts: Long): Long =
              if (ts > state.lts) ts else tick(clock())
            State(tick(timestamp), 0L)
          case s  =>
            State(timestamp, s)
        } else State(timestamp, 0L)
      Right(
       ((state.lts - twepoch) << timestampLeftShift)
        | (datacenterId << datacenterIdShift)
        | (workerId << workerIdShift)
        | state.seq)
    }
  }
}
