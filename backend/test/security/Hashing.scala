package security

import org.multics.baueran.frep.backend._
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._

// /////////////////////////////////////////////////////////////////////////////////////
// see also: https://www.playframework.com/documentation/2.6.x/ScalaTestingWithScalaTest
//
// Individual test classes can be called, e.g.:
// testOnly security.Hashing
// /////////////////////////////////////////////////////////////////////////////////////

// TODO: Remove this and entire file!

//class Hashing extends PlaySpec with GuiceOneAppPerTest with Injecting {
//  val password = "gjkwer43jndfJHKH%:2:::2"
//  val storedHash = "jbBZjmvqWnCq6mH3cTCQ9yNLNyHiuHw5GgBj5362sxefmjai8S/3gKtmjuqpD0klENLq8u+EOT4DULGZESQCKA=="
//  val storedSalt = "1a6o0P4nrFcpt4SUP9hx2X1EDRc="
//  val precomputedResults = "1000:" + storedSalt + ":" + storedHash
//
//  "Hashing " should {
//
//    "generate same hash for string when given same salt" in {
//      val dynamicallyComputedResults =
//        controllers.hashWithSalt(password, java.util.Base64.getDecoder.decode(storedSalt))
//
//      assert(precomputedResults == dynamicallyComputedResults)
//    }
//
//    "should generate random and different hash for same password when salt is not given (no collissions)" in {
//      val dynamicallyComputedResults =
//        controllers.getRandomHash(password)
//
//      assert(dynamicallyComputedResults != precomputedResults)
//    }
//
//  }
//
//}
