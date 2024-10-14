package org.multics.baueran.frep.shared

abstract class Rx[T](var value: T) {
  def triggerLater(): Unit

  def set(newValue: T) = {
    value = newValue
    triggerLater()
  }

  def get() = {
    value
  }
}