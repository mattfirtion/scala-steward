/*
 * Copyright 2018-2020 Scala Steward contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.scalasteward.core.repoconfig

import cats.implicits._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import org.scalasteward.core.data.Update
import org.scalasteward.core.update.FilterAlg.{
  FilterResult,
  IgnoredByConfig,
  NotAllowedByConfig,
  VersionPinnedByConfig
}

final case class UpdatesConfig(
    pin: List[UpdatePattern] = List.empty,
    allow: List[UpdatePattern] = List.empty,
    ignore: List[UpdatePattern] = List.empty,
    limit: Option[Int] = None
) {
  def keep(update: Update.Single): FilterResult =
    isAllowed(update) *> isPinned(update) *> isIgnored(update)

  private def isAllowed(update: Update.Single): FilterResult = {
    lazy val matched = UpdatePattern.findMatch(allow, update)
    val isIncluded = allow.isEmpty || matched.byVersion.nonEmpty
    Either.cond(isIncluded, update, NotAllowedByConfig(update))
  }

  private def isPinned(update: Update.Single): FilterResult = {
    val m = UpdatePattern.findMatch(pin, update)
    Either.cond(
      m.byArtifactId.isEmpty || m.byVersion.nonEmpty,
      update,
      VersionPinnedByConfig(update)
    )
  }

  private def isIgnored(update: Update.Single): FilterResult = {
    val m = UpdatePattern.findMatch(ignore, update)
    if (m.byVersion.nonEmpty) Left(IgnoredByConfig(update)) else Right(update)
  }
}

object UpdatesConfig {
  implicit val customConfig: Configuration =
    Configuration.default.withDefaults

  implicit val updatesConfigDecoder: Decoder[UpdatesConfig] =
    deriveConfiguredDecoder

  implicit val updatesConfigEncoder: Encoder[UpdatesConfig] =
    deriveConfiguredEncoder
}
