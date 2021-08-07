package org.multics.baueran.frep.shared

case class PaginationResult(left: List[Int], middle: List[Int], right: List[Int], currPage: Int) {
  def isEmpty() = left.size == 0 && middle.size == 0 && right.size == 0

  def pageMin = (left ::: middle ::: right).min
  def pageMax = (left ::: middle ::: right).max

  override def toString: String = {
    if (isEmpty())
      "[ ]"
    else {
      var result = "["

      if (left.size > 0)
        result += left.sorted.mkString(", ")

      if (middle.size > 0) {
        result += ", ..., "
        result += middle.sorted.mkString(", ")
      }

      if (right.size > 0) {
        result += ", ..., "
        result += right.sorted.mkString(", ")
      }

      result += "]"
      result
    }
  }

}

/**
  * Creates a pagination
  *
  * @param totalNumberOfPages
  * @param currPage must be greater or equal to 0 - although first displayed page starts from 1, of course.
  * @param maxActivePages must be UNEVEN!
  */

class Paginator(totalNumberOfPages: Int, currPageRaw: Int, maxActivePages: Int) {
  val currPage = currPageRaw + 1

  // For example [ 1 ... 15, 16, (17), 18, 19 ... 187 ] has 5 maxActivePages in the middle
  //
  // The logic / cases are then as follows:
  //
  //  1) [ (1), 2, 3, 4, 5 ... 187 ]
  //  2) [ 1, (2), 3, 4, 5 ... 187 ]
  //  3) [ 1, 2, (3), 4, 5 ... 187 ]
  //  4) [ 1, 2, 3, (4), 5, 6 ... 187 ]
  //  5) [ 1 ... 3, 4, (5), 6, 7 ... 187 ]
  //  6) [ 1 ... 4, 5, (6), 7, 8 ... 187 ]
  //     ...
  //  7) [ 1 ... 181, 182, (183), 184, 185 ... 187 ]
  //  8) [ 1 ... 182, 183, (184), 185, 186, 187 ]
  //  9) [ 1 ... 183, 184, (185), 186, 187 ]
  // 10) [ 1 ... 183, 184, 185, (186), 187 ]
  // 11) [ 1 ... 183, 184, 185, 186, (187) ]

  // Return true if input values are 'sane'
  private def inputsSane() = {
    currPage <= totalNumberOfPages && (maxActivePages % 2 != 0)
  }

  // When page = 7, in the above example, this method returns 5, 6, 7, 8, 9
  private def closure(page: Int) = {
    var result: List[Int] = List.empty

    for (i <- 1 to (maxActivePages - 1) / 2) {
      result ::= math.min((page + i), totalNumberOfPages)
      result ::= math.max((page - i), 1)
    }
    result ::= page

    if (result.distinct.size < maxActivePages) {
      if (result.contains(totalNumberOfPages))
        result = List.range(math.max(1, totalNumberOfPages - (maxActivePages - 1)), totalNumberOfPages + 1)
      else if (result.contains(1))
        result = List.range(1, math.min(maxActivePages, totalNumberOfPages) + 1)
    }

    result.distinct.sorted
  }

  def getPagination() = {
    if (inputsSane()) {
      if (totalNumberOfPages <= maxActivePages) {
        PaginationResult(closure(currPage), List.empty, List.empty, currPage)
      }
      else {
        if (currPage <= maxActivePages && (currPage - ((maxActivePages - 1) / 2)) <= 2) {
          PaginationResult((1 :: closure(currPage)).distinct.sorted, List.empty, List(totalNumberOfPages), currPage)
        }
        else {
          if (totalNumberOfPages - currPage > (maxActivePages - 1) / 2 + 1) {
            PaginationResult(List(1), closure(currPage), List(totalNumberOfPages), currPage)
          }
          else {
            PaginationResult(List(1), List.empty, (totalNumberOfPages :: closure(currPage)).distinct.sorted, currPage)
          }
        }
      }
    }
    else
      PaginationResult(List.empty, List.empty, List.empty, currPage)
  }

}
