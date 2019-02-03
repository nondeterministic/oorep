package dao

import org.multics.baueran.frep.backend.controllers.Get
import org.multics.baueran.frep.backend.dao.CazeDao
import org.multics.baueran.frep.shared.Caze
import org.multics.baueran.frep.backend.db.db.DBContext

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

// see also: https://www.playframework.com/documentation/2.6.x/ScalaTestingWithScalaTest

class Caze_and_CazeDao extends PlaySpec with GuiceOneAppPerTest with Injecting {

  var caze: Caze = _

  "Caze " should {

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
      val dbContext = app.injector.instanceOf[DBContext]
      val cazeDao = new CazeDao(dbContext)
      assert(cazeDao.get("a", -4711).head == caze)
    }

    "be able to delete Caze from DB" in {
      val dbContext = app.injector.instanceOf[DBContext]
      val cazeDao = new CazeDao(dbContext)
      cazeDao.delete("a", -4711)
    }

  }

}
