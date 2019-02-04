package dao

import org.multics.baueran.frep.shared.FIle
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

class File_and_FileDao extends PlaySpec with GuiceOneAppPerTest with Injecting {

  var fIle: Option[FIle] = None

  "File " should {

    "decode JSON-representation of File" in {
      io.circe.parser.parse(Defs.jsonFIle) match {
        case Right(json) => json.hcursor.as[FIle] match {
          case Right(f) => fIle = Some(f)
          case Left(err) => println("Decoding of FIle failed: " + err)
        }
        case Left(err) => println("Parsing of FIle failed: " + err)
      }
    }

    "encode JSON-representation of FIle" in {
      assert(FIle.encoder(fIle.get).toString().trim == Defs.jsonFIle.trim)
    }

  }

}
