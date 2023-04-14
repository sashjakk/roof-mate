package com.github.sashjakk.user

import java.util.UUID

final case class User(id: UUID, name: String, surname: String, phone: String)
