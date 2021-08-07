import org.multics.baueran.frep.shared.{MMSearchResult, MMSection}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

// /////////////////////////////////////////////////////////////////////////////////////
// see also: https://www.playframework.com/documentation/2.6.x/ScalaTestingWithScalaTest
//
// Individual test classes can be called, e.g.:
// testOnly dao.Caze_and_CazeDao
// /////////////////////////////////////////////////////////////////////////////////////

class MMDataStructuresTest extends PlaySpec with GuiceOneAppPerTest with Injecting {

  s"MMSection" should {

    val s1 = MMSection(1, 1, 1, None, None, Some("foo"), Some("poo"))
    val s1_prime = MMSection(1, 1, 1, None, None, Some("foo"), Some("poo"))
    val s3 = MMSection(1, 2, 1, None, None, Some("foo"), Some("poo"))

    "should be the same if it is the same" in {
      assert(s1 == s1)
    }

    "should be the same if content is the same" in {
      assert(s1 == s1_prime)
    }

    "should NOT be the same if it isn't the same" in {
      assert(s1 != s3)
    }

  }

  s"MMSearchResult" should {

    val s1 = MMSection(1, 1, 1, None, None, Some("foo"), Some("poo"))
    val s2 = MMSection(1, 1, 1, None, None, Some("foo2"), Some("poo2"))
    val s3 = MMSection(1, 2, 1, None, None, Some("foo"), Some("poo"))

    "should be the same if it is the same" in {
      val sr1 = MMSearchResult("kent", 1, "Sulphur", List(s3, s2, s1))
      val sr2 = MMSearchResult("kent", 1, "Sulphur", List(s2, s1, s3))

      assert(sr1 == sr2)
    }

    "should NOT be the same if it isn't the same" in {
      val sr1 = MMSearchResult("kent", 1, "Sulphur", List(s3, s2))
      val sr2 = MMSearchResult("kent", 1, "Sulphur", List(s1, s3))

      assert(sr1 != sr2)
    }

  }

}
