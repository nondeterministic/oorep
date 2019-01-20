package org.multics.baueran.frep.shared

import java.util.Date

case class Caze(id: String,
                member_id: Int,
                date: Date,
                description: String,
                results: List[CaseRubric])
{

}
