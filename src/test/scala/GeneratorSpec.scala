package seqd

import org.scalatest.FunSpec
import scala.util.Right

class GeneratorSpec extends FunSpec {
  describe("Generator") {
    it ("should require valid inputs") {
      assert(Generator(workerId = -1).isLeft)
      assert(Generator(datacenterId = -1).isLeft)
    }

    it ("should recognize backwards winding clocks") {
      Generator(prev = Generator.State(Clock.default() + 1000, 0))
       .fold(fail(_), { gen =>
          assert(gen.next().isLeft)
       })
    }
    
    it ("should encode worker and data center ids") {
      Generator(workerId = 3, datacenterId = 5)
        .fold(fail(_), { gen =>
          gen.next.fold(fail(_), { id =>
            id match {
              case Generator.Id(_, datacenter, worker, _) =>
                assert(datacenter == 5)
                assert(worker == 3)
            }
          })
        })
    }

    it ("should seq at ids that are requested at the same time") {
      val frozen = new Clock {
        val apply = System.currentTimeMillis
      }
      Generator(clock = frozen)
       .fold(fail(_), { gen =>
         def index() = gen.next().right.map { case Generator.Id(_, _, _, seq) => seq }
         (0 to 10).foreach { idx =>
           assert(index() == Right(idx))
         }
       })
    }

    it ("should permit the use of a custom twepoch") {
      val twepoch = System.currentTimeMillis
      val frozen = new Clock {
        val apply = System.currentTimeMillis
      }
      Generator(twepoch = twepoch, clock = frozen)
        .fold(fail(_), { gen =>
          gen.next().right.foreach { case Generator.Id(ts, _, _, _) => assert(ts == (frozen.apply - twepoch)) }
        })
    }
  }
}
