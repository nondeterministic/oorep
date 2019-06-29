package org.multics.baueran.frep.backend.repertory

import java.io.{ PrintWriter, File }
import scala.io.Source
import io.circe._, io.circe.parser._
import io.circe.syntax._

import scala.collection.mutable.ListBuffer

import org.multics.baueran.frep.shared._

// ----------------------------------------------------------------------
// Save and load repertory data in JSON
// ----------------------------------------------------------------------
// Needs something like this in order to function:
// 
//   implicit def repertoryToRepertory(r: Repertory) = new IoRepertory(r)
// ----------------------------------------------------------------------

class IoRepertory(rep: Repertory) {
    def saveRemediesTo(path: String) = {
    val pw = new PrintWriter(new File(path))
    pw.write(rep.remedies.asJson.toString())
    pw.close
  }

  def saveRubricsTo(path: String) = {
    val pw = new PrintWriter(new File(path))
    pw.write(rep.rubrics.asJson.toString())
    pw.close
  }

  def saveChaptersTo(path: String) = {
    val pw = new PrintWriter(new File(path))
    pw.write(rep.chapters.asJson.toString())
    pw.close
  }

  def saveChapterRemediesTo(path: String) = {
    val pw = new PrintWriter(new File(path))
    pw.write(rep.chapterRemedies.asJson.toString())
    pw.close
  }

  def saveRubricRemediesTo(path: String) = {
    val pw = new PrintWriter(new File(path))
    pw.write(rep.rubricRemedies.asJson.toString())
    pw.close
  }

  def saveInfoTo(path: String) = {
    val pw = new PrintWriter(new File(path))
    pw.write(rep.info.asJson.toString())
    pw.close
  }

  def saveTo(path: String) = {
    saveRemediesTo(path + rep.info.abbrev + "_" + "remedies.json")  
    saveRubricsTo(path + rep.info.abbrev + "_" + "rubrics.json")  
    saveChaptersTo(path + rep.info.abbrev + "_" + "chapters.json")  
    saveChapterRemediesTo(path + rep.info.abbrev + "_" + "chapter_remedies.json")  
    saveRubricRemediesTo(path + rep.info.abbrev + "_" + "rubric_remedies.json")
    saveInfoTo(path + rep.info.abbrev + "_" + "info.json")
  }
}

object Repertory {

  def loadFrom(path: String, abbrev: String) = {

    def loadRemedies(): List[Remedy] = {
      val wholePath = path + abbrev + "_remedies.json"
      val lines = Source.fromFile(wholePath).getLines.map(_.trim).mkString(" ")
      parse(lines) match {
        case Right(json) => {
          val cursor = json.hcursor
          cursor.as[List[Remedy]] match {
            case Right(content) => content
            case Left(_) => println("Remedies parsing of JSON failed: wrong data?"); List()
          }
        }
        case Left(_) => println("Remedies parsing failed: no JSON-input?"); List()
      }
    }

    def loadRubrics(): List[Rubric] = {
      val wholePath = path + abbrev + "_rubrics.json"
      val lines = Source.fromFile(wholePath).getLines.map(_.trim).mkString(" ")
      parse(lines) match {
        case Right(json) => {
          val cursor = json.hcursor
          cursor.as[List[Rubric]] match {
            case Right(content) => content
            case Left(_) => println("Rubrics parsing of JSON failed: wrong data?"); List()
          }
        }
        case Left(_) => println("Rubrics parsing failed: no JSON-input?"); List()
      }
    }

    def loadInfo(): Option[Info] = {
      val wholePath = path + abbrev + "_info.json"
      val lines = Source.fromFile(wholePath).getLines.map(_.trim).mkString(" ")
      parse(lines) match {
        case Right(json) => {
          val cursor = json.hcursor
          cursor.as[Info] match {
            case Right(content) => Some(content)
            case Left(_) => println("Info parsing of JSON failed: wrong data?"); None
          }
        }
        case Left(_) => println("Info parsing failed: no JSON-input?"); None
      }
    }

    def loadChapterRemedies(): List[ChapterRemedy] = {
      val wholePath = path + abbrev + "_chapter_remedies.json"
      val lines = Source.fromFile(wholePath).getLines.map(_.trim).mkString(" ")
      parse(lines) match {
        case Right(json) => {
          val cursor = json.hcursor
          cursor.as[List[ChapterRemedy]] match {
            case Right(content) => content
            case Left(_) => println("ChapterRemedies parsing of JSON failed: wrong data?"); List()
          }
        }
        case Left(_) => println("ChapterRemedies parsing failed: no JSON-input?"); List()
      }
    }

    def loadChapters(): List[Chapter] = {
      val wholePath = path + abbrev + "_chapters.json"
      val lines = Source.fromFile(wholePath).getLines.map(_.trim).mkString(" ")
      parse(lines) match {
        case Right(json) => {
          val cursor = json.hcursor
          cursor.as[List[Chapter]] match {
            case Right(content) => content
            case Left(_) => println("Chapters parsing of JSON failed: wrong data?"); List()
          }
        }
        case Left(_) => println("Chapters parsing failed: no JSON-input?"); List()
      }
    }

    def loadRubricRemedies(): List[RubricRemedy] = {
      val wholePath = path + abbrev + "_rubric_remedies.json"
      val lines = Source.fromFile(wholePath).getLines.map(_.trim).mkString(" ")
      parse(lines) match {
        case Right(json) => {
          val cursor = json.hcursor
          cursor.as[List[RubricRemedy]] match {
            case Right(content) => content
            case Left(_) => println("RubricRemedies parsing of JSON failed: wrong data?"); List()
          }
        }
        case Left(_) => println("RubricRemedies parsing failed: no JSON-input?"); List()
      }
    }

    val info = loadInfo() match {
      case Some(i) => i
      case None => println("No Info loaded!"); Info(abbrev, abbrev, "en", None, None, None, None, None, None, RepAccess.Private)
    }

    new Repertory(info, loadChapters(), loadRemedies(), loadChapterRemedies(), loadRubrics(), loadRubricRemedies())
  }
}
