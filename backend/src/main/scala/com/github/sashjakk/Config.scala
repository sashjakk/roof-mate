package com.github.sashjakk

object Config {
  case class DatabaseConfig(url: String, name: String, user: String, password: String)
  case class AppConfig(database: DatabaseConfig)
}
