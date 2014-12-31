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

    it ("should generate many ids quickly") {
      Generator()
        .fold(fail(_), { gen =>
          val t = System.currentTimeMillis
          for (i <- 1 to 1000000) {
            gen.next()
          }
          val t2 = System.currentTimeMillis
          val elapsed = t2 - t
          println("generated 1000000 ids in %d ms, or %,.0f ids/second".format(elapsed, 1000000000.0/ elapsed))
          assert(elapsed < 300)
        })
    }

    it ("should generate only unique ids") {
      Generator()
        .fold(fail(_), { gen =>
          val n = 2000000
          var set = new scala.collection.mutable.HashSet[Long]()
          (1 to n).foreach { i =>
            gen.next().right.foreach(set +=)
          }
          assert(set.size === n)
        })
    }
  }
}
