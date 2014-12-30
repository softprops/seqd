package seqd

import org.scalatest.FunSpec

class GeneratorSpec extends FunSpec {
  describe("Generator") {
    it ("should require valid inputs") {
      assert(Generator(workerId = -1).isLeft)
      assert(Generator(datacenterId = -1).isLeft)
    }
  }
}
