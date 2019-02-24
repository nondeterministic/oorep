package dao

import org.multics.baueran.frep.backend.db.db.DBContext
import org.multics.baueran.frep.backend.repertory.RepDatabase
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

class RepertoryDao extends PlaySpec with GuiceOneAppPerTest with Injecting {

    "RepertoryDao " should {
      "yield same results for publicum in DB as direct memory look-up" in {
        RepDatabase.repertory("publicum") match {
          case Some(loadedRepertory) =>
            val dbContext = app.injector.instanceOf[DBContext]
            val repertoryDao = new org.multics.baueran.frep.backend.dao.RepertoryDao(dbContext)

            var resultRubricsMem = loadedRepertory.findRubrics("splinter").filter(_.chapterId >= 0)
            var resultRubricsDao = repertoryDao.filterRubric("publicum", "splinter")
            assert(resultRubricsDao.forall(resultRubricsMem.contains(_)))

            resultRubricsMem = loadedRepertory.findRubrics("throat pain").filter(_.chapterId >= 0)
            resultRubricsDao = repertoryDao.filterRubric("publicum", "throat pain")
            assert(resultRubricsDao.forall(resultRubricsMem.contains(_)))

            resultRubricsMem = loadedRepertory.findRubrics("sdjfhsdjkfh").filter(_.chapterId >= 0)
            resultRubricsDao = repertoryDao.filterRubric("publicum", "sdjfhsdjkfh")
            assert(resultRubricsDao.size == 0 && resultRubricsDao.size == resultRubricsMem.size)

          case None => false
        }
      }
    }

}
