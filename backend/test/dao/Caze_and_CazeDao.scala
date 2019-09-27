package dao

import org.multics.baueran.frep.backend.controllers.Get
import org.multics.baueran.frep.backend.dao.CazeDao
import org.multics.baueran.frep.shared.{CaseRubric, Caze}
import org.multics.baueran.frep.backend.db.db.DBContext
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

// /////////////////////////////////////////////////////////////////////////////////////
// see also: https://www.playframework.com/documentation/2.6.x/ScalaTestingWithScalaTest
//
// Individual test classes can be called, e.g.:
// testOnly dao.Caze_and_CazeDao
// /////////////////////////////////////////////////////////////////////////////////////

class Caze_and_CazeDao extends PlaySpec with GuiceOneAppPerTest with Injecting {

  var caze: Caze = _

  val testCaze11 = Caze(1, "header", 1, "date1", "descr", List())
  val testCaze12 = Caze(1, "header", 1, "date1", "descr", List())

  val testCaze2 = Caze(1, "header", 1, "date1", "descr", List())
  val testCaze3 = Caze(1, "header", 1, "date2", "descr", List())

  val testCaze4 = Caze(1, "header1", 1, "date1", "descr", List())
  val testCaze5 = Caze(1, "header2", 1, "date2", "descr", List())

  "Caze " should {

    "treat two cazes as equal if they are" in {
      assert(testCaze11 == testCaze12)
    }

    "treat two cazes as equal if they are EXCEPT for their date" in {
      assert(testCaze2 == testCaze3)
    }

    "treat two cazes as NOT equal if they aren't in fact equal" in {
      assert(testCaze4 != testCaze5)
    }

    "decode JSON-representation of Caze" in {
      io.circe.parser.parse(Defs.jsonCaze) match {
        case Right(json) => json.hcursor.as[Caze] match {
          case Right(c) => caze = c
          case Left(err) => println("Decoding of Caze failed: " + err)
        }
        case Left(err) => println("Parsing of Caze failed: " + err)
      }
    }

    "encode JSON-representation of Caze" in {
      assert(Caze.encoder(caze).toString().trim == Defs.jsonCaze.trim)
    }

  }

  "CazeDao " should {

    "be able to insert Caze into DB" in {
      val dbContext = app.injector.instanceOf[DBContext]
      val cazeDao = new CazeDao(dbContext)
      cazeDao.insert(caze)
    }

    "be able to get Caze from DB" in {
//      val dbContext = app.injector.instanceOf[DBContext]
//      val cazeDao = new CazeDao(dbContext)
//      assert(cazeDao.get(0, -4711).head == caze)
    }

    "be able to delete Caze from DB" in {
//      val dbContext = app.injector.instanceOf[DBContext]
//      val cazeDao = new CazeDao(dbContext)
//      cazeDao.delete(0, -4711)
    }

  }

}
