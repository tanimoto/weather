package utils

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import com.github.tototoshi.csv._
import com.github.tototoshi.slick.JodaSupport._

import models._

object TMY {
  def parseTmy(usaf: String, body: String): Seq[TmyWeather] = {
    def parseDateTime(date: String, time: String): DateTime = {
      val str = s"$date $time UTC"
      val pat = "MM/dd/YYYY kk:mm z"
      DateTime.parse(str, DateTimeFormat.forPattern(pat))
    }

    def parseRow(row: Seq[String]): TmyWeather = {
      TmyWeather(
        usaf = usaf,
        datetime = parseDateTime(row(0), row(1)),
        tempDry = row(31).toDouble,
        tempDew = row(34).toDouble,
        humi = row(37).toDouble
      )
    }

    val strReader = new java.io.StringReader(body)
    val csvReader = CSVReader.open(strReader)
    val list = csvReader.all().drop(2).map(parseRow)
    csvReader.close()
    list
  }
}
