package me.avo.realworld.kotlin.ktor.server

import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.locations.Locations
import io.ktor.routing.Routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import me.avo.realworld.kotlin.ktor.auth.JwtConfig
import me.avo.realworld.kotlin.ktor.model.*
import me.avo.realworld.kotlin.ktor.repository.*
import me.avo.realworld.kotlin.ktor.util.serialization.register
import org.slf4j.event.Level

fun startServer() = embeddedServer(Netty, 5000) {
    Setup()
    install(CallLogging) {
        level = Level.INFO
    }
    install(DefaultHeaders)
    install(Locations)
    install(ContentNegotiation) {
        gson {
            serializeNulls()
            register<LoginCredentials>()
            register<RegistrationDetails>()
            register<User>()
            register<ArticleDetails>()
            register<Profile>()
        }
    }

    install(StatusPages) {
        setup()
    }

    val articleRepository: ArticleRepository = ArticleRepositoryImpl()
    val profileRepository: ProfileRepository = ProfileRepositoryImpl()
    val userRepository: UserRepository = UserRepositoryImpl()

    install(Authentication) {
        jwt {
            verifier(JwtConfig.verifier)
            realm = JwtConfig.realm
            validate {
                val email = it.payload.getClaim("email").toString()
                userRepository.findUser(email)?.let { user ->
                    val token = JwtConfig.makeToken(user)
                    user.copy(token = token)
                }
            }
        }

    }

    install(Routing) {
        setup(userRepository, articleRepository, profileRepository)
    }

}.start(wait = true)

