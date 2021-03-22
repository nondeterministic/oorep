package org.multics.baueran.frep.backend.dao

import io.getquill.{Insert, Query}
import org.multics.baueran.frep.backend.db
import org.multics.baueran.frep.shared.PasswordChangeRequest

class PasswordChangeRequestDao(dbContext: db.db.DBContext) {

  import dbContext._

  def insert(pcr: PasswordChangeRequest) = {
    val rawInsert = quote {
      infix"""INSERT INTO passwordchangerequest(id, date_, member_id) VALUES(crypt('#${pcr.id}', gen_salt('bf')), '#${pcr.date_}', #${pcr.member_id});"""
        .as[Insert[PasswordChangeRequest]]
    }
    run(rawInsert)
  }

  /**
    * This query is empty, if the unencrypted random-id, id, doesn't match the stored encrypted version of it.
    * Otherwise the query has exactly one result row with the unencrypted ID in it, which this function returns
    * as datatype List[PasswordChangeRequest].
    *
    * (cf. https://stackoverflow.com/questions/18656528/how-do-i-encrypt-passwords-with-postgresql/18660103)
    *
    * @return list containing the corresponding PasswordChangeRequest with the DECRYPTED ID on success, empty
    *         list on failure.
    */

  def get(id: String) = {
    val cleanId = id.replaceAll("[ ;']", "")

    val rawSelect = quote {
      infix"""SELECT * FROM
             (SELECT CASE WHEN (id=crypt('#${cleanId}', id))='t' THEN '#${cleanId}' ELSE 'f' END AS id, date_, member_id FROM passwordchangerequest)
             AS foobar WHERE id='#${cleanId}'"""
        .as[Query[PasswordChangeRequest]]
    }
    run(rawSelect)
  }

  def deleteForMemberId(memberId: Int) = {
    val delete = quote {
      query[PasswordChangeRequest]
        .filter(_.member_id == lift(memberId))
        .delete
    }
    run(delete)
  }

  // Return number of deleted password change requests.

  def delete(id: String) = {
    get(id) match {
      case pcr :: Nil => deleteForMemberId(pcr.member_id).toInt
      case _ => 0
    }
  }

}
