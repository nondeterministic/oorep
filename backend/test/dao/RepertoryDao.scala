package dao

import org.multics.baueran.frep.backend.db.db.DBContext
import org.multics.baueran.frep.backend.repertory.RepDatabase
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

class RepertoryDao extends PlaySpec with GuiceOneAppPerTest with Injecting {

    "RepertoryDao " should {
      "yield same result for splinter in publicum as direct memory look-up" in {
        RepDatabase.repertory("publicum") match {
          case Some(loadedRepertory) =>
            val resultRubricsMem = loadedRepertory.findRubrics("splinter").filter(_.chapterId >= 0)
            val dbContext = app.injector.instanceOf[DBContext]
            val repertoryDao = new org.multics.baueran.frep.backend.dao.RepertoryDao(dbContext)
            val resultRubricsDao = repertoryDao.filterRubric("publicum", "splinter")

            assert(resultRubricsDao.forall(resultRubricsMem.contains(_)))
          case None => false
        }
      }
    }

}
