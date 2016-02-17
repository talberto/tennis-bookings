package controllers

import org.scalatest.concurrent.ScaledTimeSpans

trait ConfigurableScaleFactor extends ScaledTimeSpans {

  override def spanScaleFactor: Double = TestConstants.ScaleFactor
}