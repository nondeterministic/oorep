package dao

object Defs {

  val jsonCaze = """
{
  "id" : 0,
  "header" : "a",
  "member_id" : -4711,
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

  val jsonFIle =
"""
{
  "header" : "Test",
  "member_id" : 2,
  "date" : "2019-02-04T20:05:56.138Z",
  "description" : "Me",
  "cazes" : [
  ]
}
"""

}