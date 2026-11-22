package org.multics.baueran.frep.backend.security

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.mvc.{RequestHeader, Result, Results}

import java.net.InetAddress
import java.security.SecureRandom
import scala.util.Try

import redis.clients.jedis.{DefaultJedisClientConfig, HostAndPort, JedisPooled}

sealed trait SessionGuardDecision
object SessionGuardDecision {
  case class Allowed(sessionId: String, newSession: Boolean) extends SessionGuardDecision
  case class Rejected(result: Result) extends SessionGuardDecision
}

@Singleton
class SessionGuard @Inject()(config: Configuration) {

  private val logger = Logger(this.getClass)
  private val random = new SecureRandom()

  private val enabled: Boolean = config.getOptional[Boolean]("oorep.security.session_guard.enabled").getOrElse(true)
  private val failOpen: Boolean = config.getOptional[Boolean]("oorep.security.session_guard.fail_open").getOrElse(true)
  private val logOnly: Boolean = config.getOptional[Boolean]("oorep.security.session_guard.log_only").getOrElse(false)

  private val redisHost: String = config.getOptional[String]("oorep.security.session_guard.redis.host").getOrElse("localhost")
  private val redisPort: Int = config.getOptional[Int]("oorep.security.session_guard.redis.port").getOrElse(6379)
  private val redisDb: Int = config.getOptional[Int]("oorep.security.session_guard.redis.db").getOrElse(0)
  private val redisPassword: Option[String] = config.getOptional[String]("oorep.security.session_guard.redis.password").filter(_.trim.nonEmpty).filter(_ != "null")
  private val redisTimeoutMs: Int = config.getOptional[Int]("oorep.security.session_guard.redis.timeout_ms").getOrElse(2000)

  private val sessionTtlSeconds: Int = config.getOptional[Int]("oorep.security.session_guard.session_ttl_seconds").getOrElse(86400)
  private val windowSeconds: Int = config.getOptional[Int]("oorep.security.session_guard.rate_limit.window_seconds").getOrElse(60)
  private val generalPerIp: Int = config.getOptional[Int]("oorep.security.session_guard.rate_limit.general_per_ip").getOrElse(120)
  private val generalPerSid: Int = config.getOptional[Int]("oorep.security.session_guard.rate_limit.general_per_sid").getOrElse(120)
  private val expensivePerIp: Int = config.getOptional[Int]("oorep.security.session_guard.rate_limit.expensive_per_ip").getOrElse(60)
  private val expensivePerSid: Int = config.getOptional[Int]("oorep.security.session_guard.rate_limit.expensive_per_sid").getOrElse(60)
  private val softIpBinding: Boolean = config.getOptional[Boolean]("oorep.security.session_guard.soft_ip_binding").getOrElse(true)
  private val trustedProxies: Seq[String] = {
    val csv = config.getOptional[String]("oorep.security.session_guard.trusted_proxies_csv")
      .map(_.split(",").toSeq.map(_.trim).filter(_.nonEmpty))
      .filter(_.nonEmpty)
    csv.getOrElse(
      config.getOptional[Seq[String]]("oorep.security.session_guard.trusted_proxies")
        .getOrElse(Seq("127.0.0.1", "::1"))
        .map(_.trim)
        .filter(_.nonEmpty)
    )
  }

  private val redis: Option[JedisPooled] =
    if (!enabled) None
    else Try {
      val builder = DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(redisTimeoutMs)
        .socketTimeoutMillis(redisTimeoutMs)
        .database(redisDb)
      redisPassword.foreach(pw => builder.password(pw))
      val clientConfig = builder.build()
      val client = new JedisPooled(new HostAndPort(redisHost, redisPort), clientConfig)
      client.ping()
      logger.info(s"SessionGuard: Redis connected at ${redisHost}:${redisPort}/db${redisDb}")
      client
    }.toOption.orElse {
      logger.warn(s"SessionGuard: could not connect to Redis at ${redisHost}:${redisPort}/db${redisDb}; enabled=${enabled} failOpen=${failOpen}")
      None
    }

  def isEnabled: Boolean = enabled

  private def newSessionId(): String = {
    val bytes = Array.fill[Byte](32)(0)
    random.nextBytes(bytes)
    bytes.map(b => f"$b%02x").mkString
  }

  private def inetAddress(ip: String): Option[InetAddress] =
    Try(InetAddress.getByName(ip.trim)).toOption

  private def ipInCidr(ip: InetAddress, cidr: String): Boolean = {
    cidr.split("/", 2).toList match {
      case baseStr :: prefixStr :: Nil =>
        (for {
          base <- inetAddress(baseStr)
          prefix <- Try(prefixStr.toInt).toOption
          ipBytes = ip.getAddress
          baseBytes = base.getAddress
          if ipBytes.length == baseBytes.length
          if prefix >= 0 && prefix <= ipBytes.length * 8
        } yield {
          var bits = prefix
          var i = 0
          var ok = true
          while (i < ipBytes.length && ok) {
            val mask = if (bits >= 8) 0xff else if (bits <= 0) 0x00 else (0xff << (8 - bits)) & 0xff
            if ((ipBytes(i) & mask) != (baseBytes(i) & mask)) ok = false
            bits -= 8
            i += 1
          }
          ok
        }).getOrElse(false)
      case _ => false
    }
  }

  private def isTrustedProxyIp(ip: String): Boolean = {
    inetAddress(ip).exists { addr =>
      trustedProxies.exists { pattern =>
        if (pattern.contains("/")) ipInCidr(addr, pattern)
        else inetAddress(pattern).exists(_.getAddress.sameElements(addr.getAddress))
      }
    }
  }

  private def isTrustedProxy(remoteAddress: String): Boolean = isTrustedProxyIp(remoteAddress)

  private def forwardedClientIp(request: RequestHeader): Option[String] = {
    val xff = request.headers.get("X-Forwarded-For")
      .toSeq
      .flatMap(_.split(","))
      .map(_.trim)
      .filter(_.nonEmpty)
      .filter(ip => inetAddress(ip).isDefined)

    // Walk right-to-left: skip trusted proxies, return first untrusted client IP.
    // This prevents clients from spoofing the leftmost XFF entry through a trusted proxy.
    val fromXff = xff.reverseIterator.find(ip => !isTrustedProxyIp(ip))
    fromXff.orElse {
      request.headers.get("X-Real-IP").map(_.trim).filter(ip => inetAddress(ip).isDefined)
    }
  }

  private def clientIp(request: RequestHeader): String = {
    val remote = request.remoteAddress
    if (isTrustedProxy(remote)) forwardedClientIp(request).getOrElse(remote)
    else remote
  }

  private def sessionKey(sid: String): String = s"oorep:sess:${sid}"
  private def rlKey(scope: String, id: String, window: Long): String = s"oorep:rl:${scope}:${id}:${window}"

  private def withRedis[T](fallback: => T)(f: JedisPooled => T): T = {
    redis match {
      case None =>
        if (failOpen) fallback else throw new RuntimeException("SessionGuard: Redis unavailable and failOpen=false")
      case Some(j) =>
        try f(j)
        catch {
          case e: Throwable =>
            logger.warn(s"SessionGuard: Redis error: ${e.getMessage}")
            if (failOpen) fallback else throw e
        }
    }
  }

  /** Ensure there is a session id; create it if missing. Used on page/app entry points. */
  def ensureSession(request: RequestHeader): (String, Boolean) = {
    val existing = request.session.get("sid").filter(_.nonEmpty)
    existing match {
      case Some(sid) =>
        withRedis((sid, false)) { j =>
          val key = sessionKey(sid)
          val exists = j.exists(key)
          if (exists) {
            j.hset(key, "last_seen", System.currentTimeMillis().toString)
            j.expire(key, sessionTtlSeconds.toLong)
            (sid, false)
          } else {
            val nsid = newSessionId()
            createSessionRecord(j, nsid, clientIp(request), request.headers.get("User-Agent").getOrElse(""))
            (nsid, true)
          }
        }
      case None =>
        withRedis {
          val sid = newSessionId()
          (sid, true)
        } { j =>
          val sid = newSessionId()
          createSessionRecord(j, sid, clientIp(request), request.headers.get("User-Agent").getOrElse(""))
          (sid, true)
        }
    }
  }

  private def createSessionRecord(j: JedisPooled, sid: String, ip: String, ua: String): Unit = {
    val key = sessionKey(sid)
    val now = System.currentTimeMillis().toString
    j.hset(key, java.util.Map.of(
      "created_at", now,
      "last_seen", now,
      "ip_first", ip,
      "ip_last", ip,
      "ip_changes", "0",
      "ua_hash", Integer.toHexString(ua.hashCode)
    ))
    j.expire(key, sessionTtlSeconds.toLong)
  }

  private def rateLimitExceeded(j: JedisPooled, scope: String, id: String, limit: Int): Boolean = {
    if (limit <= 0) return false
    val nowWindow = System.currentTimeMillis() / 1000L / windowSeconds.toLong
    val key = rlKey(scope, id, nowWindow)
    val count = j.incr(key)
    if (count == 1L) j.expire(key, (windowSeconds.toLong * 2) + 5)
    count > limit.toLong
  }

  def checkPublicApi(request: RequestHeader, expensive: Boolean = false): SessionGuardDecision = {
    if (!enabled) return SessionGuardDecision.Allowed(request.session.get("sid").getOrElse(""), newSession = false)

    val sidOpt = request.session.get("sid").filter(_.nonEmpty)
    val ip = clientIp(request)

    withRedis[SessionGuardDecision] {
      SessionGuardDecision.Allowed(sidOpt.getOrElse(""), newSession = false)
    } { j =>
      sidOpt match {
        case None =>
          logger.info(s"SessionGuard: missing sid for ${request.method} ${request.path} from ${ip}")
          val res = Results.Unauthorized("Missing session. Please reload the application page.")
          if (logOnly) SessionGuardDecision.Allowed("", newSession = false) else SessionGuardDecision.Rejected(res)
        case Some(sid) =>
          val key = sessionKey(sid)
          if (!j.exists(key)) {
            // stale/expired sid - reissue transparently by allowing but marking newSession; caller must set cookie
            logger.info(s"SessionGuard: stale sid for ${request.method} ${request.path} from ${ip}; reissuing")
            val nsid = newSessionId()
            createSessionRecord(j, nsid, ip, request.headers.get("User-Agent").getOrElse(""))
            // still rate-limit the new session/ip below
            val ipLimit = if (expensive) expensivePerIp else generalPerIp
            val sidLimit = if (expensive) expensivePerSid else generalPerSid
            val ipHit = rateLimitExceeded(j, "ip", ip, ipLimit)
            val sidHit = rateLimitExceeded(j, "sid", nsid, sidLimit)
            if (ipHit || sidHit) {
              logger.warn(s"SessionGuard: rate limit (new session) ${request.method} ${request.path} ip=${ip} sid=${nsid.take(8)} ipHit=${ipHit} sidHit=${sidHit}")
              val res = Results.TooManyRequests("Too many requests. Please wait and try again.").withHeaders("Retry-After" -> windowSeconds.toString)
              if (logOnly) SessionGuardDecision.Allowed(nsid, newSession = true) else SessionGuardDecision.Rejected(res)
            } else {
              SessionGuardDecision.Allowed(nsid, newSession = true)
            }
          } else {
            // existing session
            val lastIp = Option(j.hget(key, "ip_last")).getOrElse("")
            if (softIpBinding && lastIp.nonEmpty && lastIp != ip) {
              j.hincrBy(key, "ip_changes", 1L)
              j.hset(key, "ip_last", ip)
              logger.info(s"SessionGuard: soft IP change sid=${sid.take(8)} ${lastIp} -> ${ip} ${request.method} ${request.path}")
            } else if (lastIp.isEmpty) {
              j.hset(key, "ip_last", ip)
            }
            j.hset(key, "last_seen", System.currentTimeMillis().toString)
            j.expire(key, sessionTtlSeconds.toLong)

            val ipLimit = if (expensive) expensivePerIp else generalPerIp
            val sidLimit = if (expensive) expensivePerSid else generalPerSid
            val ipHit = rateLimitExceeded(j, "ip", ip, ipLimit)
            val sidHit = rateLimitExceeded(j, "sid", sid, sidLimit)
            if (ipHit || sidHit) {
              logger.warn(s"SessionGuard: rate limit ${request.method} ${request.path} ip=${ip} sid=${sid.take(8)} ipHit=${ipHit} sidHit=${sidHit}")
              val res = Results.TooManyRequests("Too many requests. Please wait and try again.").withHeaders("Retry-After" -> windowSeconds.toString)
              if (logOnly) SessionGuardDecision.Allowed(sid, newSession = false) else SessionGuardDecision.Rejected(res)
            } else {
              SessionGuardDecision.Allowed(sid, newSession = false)
            }
          }
      }
    }
  }
}
