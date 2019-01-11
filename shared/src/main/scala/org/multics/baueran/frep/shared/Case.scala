package org.multics.baueran.frep.shared

import java.sql.Date

case class Case(id: String,
                owner: Member,
                date: Date,
                description: String,
                results: List[CaseRubric])
{

}
