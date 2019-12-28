package dao

import org.multics.baueran.frep.shared.{Remedy}
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._

// /////////////////////////////////////////////////////////////////////////////////////
// see also: https://www.playframework.com/documentation/2.6.x/ScalaTestingWithScalaTest
//
// Individual test classes can be called, e.g.:
// testOnly dao.Caze_and_CazeDao
// /////////////////////////////////////////////////////////////////////////////////////

class RepertoryDatastructures extends PlaySpec with GuiceOneAppPerTest with Injecting {

  val caust1 = Remedy("Caust", 0, "Caust", "Causticum")
  val caust1_same = Remedy("Caust", 0, "Caust", "Causticum")
  val sil = Remedy("Sil", 1, "Sil", "Silicea terra")

  "Remedy " should {

    "be the same, if it is the same" in {
      assert(caust1 == caust1_same)
    }

    "not be the same, if it isn't the same" in {
      assert(caust1 != sil)
    }
  }

}
