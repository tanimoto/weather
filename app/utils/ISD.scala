package utils

import scala.util.{Try, Success, Failure}
import scala.collection.JavaConversions._

import scalax.io.Resource
import scalax.io.JavaConverters._

import resource._

import java.io.InputStream
import java.util.zip.GZIPInputStream

import org.apache.commons.net.ftp._

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC

import models.IsdStation
import models.IsdWeather

class IsdFtp {
  private val ftp = new FTPClient()
  private val dataPath = "/pub/data/noaa/isd-lite"

  /**
    * Connection
    */
  def connect(): Unit = {
    ftp.connect("ftp.ncdc.noaa.gov")
    val login = ftp.login("anonymous", "anonymous")
    val reply = ftp.getReplyCode()
    if (login && FTPReply.isPositiveCompletion(reply)) {
      // Disable buffering
      ftp.setBufferSize(0)
      // Transfer as binary
      ftp.setFileType(FTP.BINARY_FILE_TYPE)
      // Set timeout to 5 minutes
      ftp.setControlKeepAliveTimeout(300)
      // Enable passive mode
      ftp.enterLocalPassiveMode()
    } else {
      disconnect()
    }
  }

  def disconnect(): Unit = {
    ftp.disconnect()
  }

  def requireConnection(): Unit = {
    if (!ftp.isConnected())
      connect()
  }

  /**
    * NOAA Identifiers
    */
  private def checkUsaf(usaf: String): Unit = {
    require(usaf.length == 6)
    val usafNum = usaf.toInt
    require(0 <= usafNum && usafNum <= 999999)
  }

  private def checkWban(wban: String): Unit = {
    require(wban.length == 5)
    val wbanNum = wban.toInt
    require(0 <= wbanNum && wbanNum <= 99999)
  }

  private def checkYear(year: Int): Unit = {
    require(1901 <= year && year < 3000)
  }

  private def makeFileName(usaf: String, wban: String, year: Int): String = {
    checkYear(year)
    checkUsaf(usaf)
    checkWban(wban)

    s"${usaf}-${wban}-${year}.gz"
  }

  private def makeFilePath(usaf: String, wban: String, year: Int): String = {
    val name = makeFileName(usaf, wban, year)
    s"${dataPath}/${year}/${name}"
  }

  /**
    * FTP
    */
  def listFiles(path: String): Seq[FTPFile] = {
    requireConnection()

    ftp.listFiles(path).toSeq
  }

  def listNames(path: String): Seq[String] = {
    requireConnection()

    ftp.listNames(path).toSeq
  }

  def listYear(year: Int): Seq[String] = {
    checkYear(year)

    val path = s"${dataPath}/${year}"
    listNames(path)
  }

  def downloadFile(infile: String, outfile: String): Unit = {
    requireConnection()

    for {
      out <- managed(new java.io.FileOutputStream(outfile))
    } {
      ftp.retrieveFile(infile, out)
    }
  }

  def downloadStream(filepath: String): Option[InputStream] = {
    requireConnection()

    Option(ftp.retrieveFileStream(filepath))
  }

  def decompressStream(stream: InputStream): java.io.BufferedInputStream = {
    val gzip = new GZIPInputStream(stream)
    val buff = new java.io.BufferedInputStream(gzip)
    buff
  }

  /*
   * Weather
   */
  def getWeatherAsLines(usaf: String, wban: String, year: Int): Option[Seq[String]] = {
    val filepath = makeFilePath(usaf, wban, year)

    for {
      stream <- downloadStream(filepath)
    } yield {
      val buff = decompressStream(stream)
      val input = buff.asInput
      val lines = input.lines().toSeq
      ftp.completePendingCommand()
      lines
    }
  }

  def getWeatherAsMap(usaf: String, wban: String, year: Int):
      Option[Seq[Map[String, String]]] = {
    for {
      lines <- getWeatherAsLines(usaf, wban, year)
    } yield {
      lines.map(parseLine)
    }
  }

  def getWeather(usaf: String, wban: String, year: Int):
      Option[Seq[IsdWeather]] = {
    for {
      lines <- getWeatherAsMap(usaf, wban, year)
    } yield {
      lines.map { m =>
        IsdWeather(
          usaf = usaf,
          wban = wban,
          datetime = new DateTime(
            m("year").toInt, m("month").toInt, m("day").toInt,
            m("hour").toInt, 0, 0, 0, UTC),
          tempDry = if (m("air temp") == "") 0.0d else m("air temp").toDouble,
          tempDew = if (m("dew temp") == "") 0.0d else m("dew temp").toDouble
        )
      }
    }
  }

  /**
    * Parsing
    */
  def parseLine(line: String): Map[String, String] = {

    case class Field(
      val start: Int,
      val end: Int,
      val length: Int,
      val name: String,
      val description: String
    )

    val fields = Seq(
      Field( 1,  4, 4, "year",       "Year"),
      Field( 6,  7, 2, "month",      "Month"),
      Field( 9, 11, 2, "day",        "Day"),
      Field(12, 13, 2, "hour",       "Hour"),
      Field(14, 19, 6, "air temp",   "Air Temperature (C)"),
      Field(20, 25, 6, "dew temp",   "Dew Point Temperature (C)"),
      Field(26, 31, 6, "pressure",   "Sea Level Pressure (hPa)"),
      Field(32, 37, 6, "wind dir",   "Wind Direction (Degrees)"),
      Field(38, 43, 6, "wind speed", "Wind Speed Rate (m/s)"),
      Field(44, 49, 6, "sky cond",   "Sky Condition"),
      Field(50, 55, 6, "precip 1h",  "Precipitation 1-Hour (mm)"),
      Field(56, 61, 6, "precip 6h",  "Precipitation 6-Hour (mm)")
    )

    val empty = Map[String, String]().withDefaultValue("")

    if (line.length >= 61) {
      fields.foldLeft(empty){ (m,f) =>
        val value = line.substring(f.start-1, f.end).trim
        val recoded = if (value == "-9999") "" else value
        m.updated(f.name, recoded)
      }
    } else {
      empty
    }
  }

}

/*
object NoaaIsdFtpExample {

  val usaf = "010230"
  val wban = "99999"
  val year = 1960

  val noaa = new NoaaIsdFtp()
  for {
    data <- noaa.getWeatherAsMap(usaf, wban, year)
  } {
    println(data.take(10))
  }

}
*/
