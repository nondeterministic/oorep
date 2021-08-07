import org.multics.baueran.frep.shared.Paginator
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._

// /////////////////////////////////////////////////////////////////////////////////////
// see also: https://www.playframework.com/documentation/2.6.x/ScalaTestingWithScalaTest
//
// Individual test classes can be called, e.g.:
// testOnly dao.Caze_and_CazeDao
// /////////////////////////////////////////////////////////////////////////////////////

class PaginatorTest extends PlaySpec with GuiceOneAppPerTest with Injecting {

  s"Pagination with 5 maxActivePages: " should {

    val maxActivePages = 5

    "page 1/4" in {
      val p = new Paginator(4, 1, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 4 && pagination.middle.size == 0 && pagination.right.size == 0)
    }

  }

  s"Pagination with 3 maxActivePages: " should {

    val maxActivePages = 3

    "page 1/1" in {
      val p = new Paginator(1, 0, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == 0)
    }

    "page 1/2" in {
      val p = new Paginator(2, 0, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 2 && pagination.middle.size == 0 && pagination.right.size == 0)
    }

    "page 3/3" in {
      val p = new Paginator(3, 2, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == maxActivePages && pagination.middle.size == 0 && pagination.right.size == 0)
    }

    "page 1/187" in {
      val p = new Paginator(187, 0, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == maxActivePages && pagination.middle.size == 0 && pagination.right.size == 1)
    }

    "page 2/187" in {
      val p = new Paginator(187, 1, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == maxActivePages && pagination.middle.size == 0 && pagination.right.size == 1)
    }

    "page 3/187" in {
      val p = new Paginator(187, 2, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == maxActivePages + 1 && pagination.middle.size == 0 && pagination.right.size == 1)
    }

    "page 4/187" in {
      val p = new Paginator(187, 3, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 5/187" in {
      val p = new Paginator(187, 4, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 6/187" in {
      val p = new Paginator(187, 5, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 7/187" in {
      val p = new Paginator(187, 6, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 71/187" in {
      val p = new Paginator(187, 70, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 182/187" in {
      val p = new Paginator(187, 181, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 183/187" in {
      val p = new Paginator(187, 182, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(true)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 184/187" in {
      val p = new Paginator(187, 183, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 185/187" in {
      val p = new Paginator(187, 184, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == maxActivePages + 1)
    }

    "page 186/187" in {
      val p = new Paginator(187, 185, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == maxActivePages)
    }

    "page 187/187" in {
      val p = new Paginator(187, 186, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == maxActivePages)
    }

  }

  s"Pagination with 5 maxActivePages: " should {

    val maxActivePages = 5

    "page 1/1" in {
      val p = new Paginator(1, 0, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == 0)
    }

    "page 1/2" in {
      val p = new Paginator(2, 0, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 2 && pagination.middle.size == 0 && pagination.right.size == 0)
    }

    "page 3/3" in {
      val p = new Paginator(3, 2, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 3 && pagination.middle.size == 0 && pagination.right.size == 0)
    }

    "page 1/187" in {
      val p = new Paginator(187, 0, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == maxActivePages && pagination.middle.size == 0 && pagination.right.size == 1)
    }

    "page 2/187" in {
      val p = new Paginator(187, 1, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == maxActivePages && pagination.middle.size == 0 && pagination.right.size == 1)
    }

    "page 3/187" in {
      val p = new Paginator(187, 2, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == maxActivePages && pagination.middle.size == 0 && pagination.right.size == 1)
    }

    "page 4/187" in {
      val p = new Paginator(187, 3, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == maxActivePages + 1 && pagination.middle.size == 0 && pagination.right.size == 1)
    }

    "page 5/187" in {
      val p = new Paginator(187, 4, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 6/187" in {
      val p = new Paginator(187, 5, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 7/187" in {
      val p = new Paginator(187, 6, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 71/187" in {
      val p = new Paginator(187, 70, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 182/187" in {
      val p = new Paginator(187, 181, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 183/187" in {
      val p = new Paginator(187, 182, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(true)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 184/187" in {
      val p = new Paginator(187, 183, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == maxActivePages + 1)
    }

    "page 185/187" in {
      val p = new Paginator(187, 184, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == maxActivePages)
    }

    "page 186/187" in {
      val p = new Paginator(187, 185, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == maxActivePages)
    }

    "page 187/187" in {
      val p = new Paginator(187, 186, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == maxActivePages)
    }

  }

  s"Pagination with 7 maxActivePages: " should {

    val maxActivePages = 7

    "page 1/1" in {
      val p = new Paginator(1, 0, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == 0)
    }

    "page 1/2" in {
      val p = new Paginator(2, 0, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 2 && pagination.middle.size == 0 && pagination.right.size == 0)
    }

    "page 3/3" in {
      val p = new Paginator(3, 2, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 3 && pagination.middle.size == 0 && pagination.right.size == 0)
    }

    "page 1/187" in {
      val p = new Paginator(187, 0, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == maxActivePages && pagination.middle.size == 0 && pagination.right.size == 1)
    }

    "page 2/187" in {
      val p = new Paginator(187, 1, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == maxActivePages && pagination.middle.size == 0 && pagination.right.size == 1)
    }

    "page 3/187" in {
      val p = new Paginator(187, 2, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == maxActivePages && pagination.middle.size == 0 && pagination.right.size == 1)
    }

    "page 4/187" in {
      val p = new Paginator(187, 3, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == maxActivePages && pagination.middle.size == 0 && pagination.right.size == 1)
    }

    "page 5/187" in {
      val p = new Paginator(187, 4, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == maxActivePages + 1 && pagination.middle.size == 0 && pagination.right.size == 1)
    }

    "page 6/187" in {
      val p = new Paginator(187, 5, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 7/187" in {
      val p = new Paginator(187, 6, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 71/187" in {
      val p = new Paginator(187, 70, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 182/187" in {
      val p = new Paginator(187, 181, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == maxActivePages && pagination.right.size == 1)
    }

    "page 183/187" in {
      val p = new Paginator(187, 182, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(true)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == maxActivePages + 1)
    }

    "page 184/187" in {
      val p = new Paginator(187, 183, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == maxActivePages)
    }

    "page 185/187" in {
      val p = new Paginator(187, 184, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == maxActivePages)
    }

    "page 186/187" in {
      val p = new Paginator(187, 185, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == maxActivePages)
    }

    "page 187/187" in {
      val p = new Paginator(187, 186, maxActivePages)
      val pagination = p.getPagination()
      println(pagination)
      assert(pagination.left.size == 1 && pagination.middle.size == 0 && pagination.right.size == maxActivePages)
    }

  }

}
