package controllers

import org.multics.baueran.frep.backend.controllers.Get
import org.multics.baueran.frep.backend.dao.CazeDao
import org.multics.baueran.frep.shared.Caze
import org.multics.baueran.frep.backend.db.db.DBContext
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */

// see also: https://www.playframework.com/documentation/2.6.x/ScalaTestingWithScalaTest

class HomeControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  val jsonString =
"""
{
  "header" : "a",
  "member_id" : -1,
  "date" : "2019-01-30T19:42:01.299Z",
  "description" : "b",
  "results" : [
    {
      "rubric" : {
        "id" : 61832,
        "mother" : null,
        "isMother" : null,
        "chapterId" : 32,
        "fullPath" : "Throat, pain, splinter, as from a, swallowing, on",
        "path" : null,
        "textt" : null
      },
      "repertoryAbbrev" : "kent",
      "rubricWeight" : 1,
      "weightedRemedies" : [
        {
          "remedy" : {
            "id" : 305,
            "nameAbbrev" : "Hep.",
            "nameLong" : "Hepar Sulphur"
          },
          "weight" : 3
        },
        {
          "remedy" : {
            "id" : 60,
            "nameAbbrev" : "Apis",
            "nameLong" : "Apis Mellifera"
          },
          "weight" : 2
        },
        {
          "remedy" : {
            "id" : 71,
            "nameAbbrev" : "Arg-n.",
            "nameLong" : "Argentum Nitricum"
          },
          "weight" : 2
        }
      ]
    },
    {
      "rubric" : {
        "id" : 61831,
        "mother" : null,
        "isMother" : null,
        "chapterId" : 32,
        "fullPath" : "Throat, pain, splinter, as from a, oesophagus",
        "path" : null,
        "textt" : null
      },
      "repertoryAbbrev" : "kent",
      "rubricWeight" : 1,
      "weightedRemedies" : [
        {
          "remedy" : {
            "id" : 73,
            "nameAbbrev" : "Ars.",
            "nameLong" : "Arsenicum Album"
          },
          "weight" : 1
        }
      ]
    }
  ]
}
"""

  "CazeDao " should {
    "decode JSON-representation of List[CaseRubric]" in {
      println("********************************************************")
      val dbContext = app.injector.instanceOf[DBContext]
      val cazeDao = new CazeDao(dbContext)
      println("********************************************************")

      io.circe.parser.parse(jsonString) match {
        case Right(json) => json.hcursor.as[Caze] match {
          case Right(caze) => println("FUCK YEAH: " + caze.toString)
          case _ => println("Decoding failed.")
        }
        case _ => println("Parsing failed.")
      }
    }
  }

//  "HomeController GET" should {
//
//    "render the index page from a new instance of controller" in {
//      val controller = new Get(stubControllerComponents(), null)
//      val home = controller.index().apply(FakeRequest(GET, "/"))
//
//      status(home) mustBe OK
//      contentType(home) mustBe Some("text/html")
//      contentAsString(home) must include ("Welcome to Play")
//    }
//
//    "render the index page from the application" in {
//      val controller = inject[Get]
//      val home = controller.index().apply(FakeRequest(GET, "/"))
//
//      status(home) mustBe OK
//      contentType(home) mustBe Some("text/html")
//      contentAsString(home) must include ("Welcome to Play")
//    }
//
//    "render the index page from the router" in {
//      val request = FakeRequest(GET, "/")
//      val home = route(app, request).get
//
//      status(home) mustBe OK
//      contentType(home) mustBe Some("text/html")
//      contentAsString(home) must include ("Welcome to Play")
//    }
//  }
}
