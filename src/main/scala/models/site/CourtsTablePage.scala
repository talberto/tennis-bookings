package models.site

import javax.inject.{Inject, Singleton}

import models.db.Booking
import org.joda.time.{Days, LocalDate}
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.{By, WebDriver, WebElement}
import play.api.Logger

import scala.collection.JavaConversions._

@Singleton
class CourtsTablePage @Inject() (val driver: WebDriver, val screenshotsHelper: ScreenshotsHelper) extends Page {
  val logger: Logger = Logger(this.getClass)
  val actions: Actions = new Actions(driver)
  
  def book(booking: Booking) = {
    logger.trace(s"book($booking)")
    logger.debug(s"Trying to book court [${booking.court}] at date [${booking.date}] and hour [${booking.time}]")
    val daysFromTodayToBooking = Days.daysBetween(LocalDate.now, booking.date)
    logger.debug(s"Days of difference between today and the booking date [${daysFromTodayToBooking.getDays}]")
    require(daysFromTodayToBooking.isLessThan(TennisSite.DaysOfDifference.plus(Days.ONE)), s"The booking date is past today plus ${TennisSite.DaysOfDifference.getDays()} days")
    require(daysFromTodayToBooking.isGreaterThan(Days.ZERO.minus(Days.ONE)), "The booking date has already passed");
    // Move to the corresponding day
    moveDays(daysFromTodayToBooking.getDays())
    if(!canBook(booking)) {
      throw new AlreadyBookedException(booking)
    }
    val courtElem = findCourt(booking)
    // Make double click
    logger.debug(s"Double clicking on element [identified by css selector [$courtElem]")
    actions.doubleClick(courtElem).perform()
    screenshotsHelper.takeScreenshot()

    // Check if we reached the limit of bookings
    if(driver.findElements(By.cssSelector(".erreur")).nonEmpty) {
      throw new BookingsLimitReachedException("Apparently reached the bookings limit");
    }
  }
  
  def moveDays(days: Int) {
    logger.trace(s"moveDays($days)")
    screenshotsHelper.takeScreenshot()
    if(days == 1) {
      goTomorrow
    } else if(days == -1) {
      goYesterday
    } else if(days > 0) {
      goTomorrow
      moveDays(days - 1)
    } else if(days < 0) {
      goYesterday
      moveDays(days + 1)
    } 
  }
  
  protected def goTomorrow = {
    logger.trace(s"goTomorrow()")
    driver.findElement(By.id("btn_plus")).click
  }
  
  protected def goYesterday = {
    logger.trace(s"goYesterday()")
    driver.findElement(By.id("btn_moins")).click
  }
  
  /**
   * Determines if we can book or not
   */
  def canBook(booking: Booking) = {
    logger.trace(s"canBook($booking)")
    val courtElem = findCourt(booking)
    val backGroundColor = courtElem.getCssValue("background-color")
    logger.debug(s"background-color = [$backGroundColor]")
    backGroundColor == "rgba(153, 255, 153, 1)"
  }
  
  protected def findCourt(booking: Booking): WebElement = {
    logger.trace(s"findCourt($booking)")
    logger.debug(s"Trying find the court [${booking.court}] at hour [${booking.time}]")
    val courtNo = booking.court
    val hourNo = booking.time.getHourOfDay()
    
    // Find the box representing the court
    val selector = s"${hourNo}_0_${courtNo}"
    logger.debug(s"Searching element with selector [$selector]")
    driver.findElement(By.id(selector))
  }
  
  def isCurrentPage: Boolean = driver.findElements(By.id("workpage")).nonEmpty
}