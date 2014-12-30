package seqd

import org.scalatest.FunSpec

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
         println(gen.next())
         println(gen.next())
       })
    }
  }
}
