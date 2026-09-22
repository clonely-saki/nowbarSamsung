package com.nowbarsports.poc

import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class FootballApiClient(
    private val apiKey: String,
    private val endpoint: String = "https://v3.football.api-sports.io/fixtures"
) {
    fun fetchFixture(fixtureId: String): FootballFixtureDto {
        if (apiKey.isBlank()) {
            throw FootballApiException("未配置 API_FOOTBALL_KEY")
        }
        if (fixtureId.isBlank()) {
            throw FootballApiException("未配置 API_FOOTBALL_FIXTURE_ID")
        }

        val connection = (URL(endpoint + "?id=" + fixtureId).openConnection() as HttpsURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            doInput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("x-apisports-key", apiKey)
        }

        return try {
            val statusCode = connection.responseCode
            val body = (if (statusCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (statusCode !in 200..299) {
                throw FootballApiException("API 请求失败（HTTP $statusCode）", statusCode)
            }
            parseFixture(body)
        } catch (error: FootballApiException) {
            throw error
        } catch (error: IOException) {
            throw FootballApiException("网络请求失败：${error.message ?: "IO error"}", cause = error)
        } catch (error: Exception) {
            throw FootballApiException("API 响应解析失败", cause = error)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseFixture(body: String): FootballFixtureDto {
        if (body.isBlank()) {
            throw FootballApiException("API 返回为空")
        }

        val root = JSONObject(body)
        val errors = root.optJSONObject("errors")
        if (errors != null && errors.length() > 0) {
            throw FootballApiException("API 返回错误")
        }

        val results = root.optInt("results", 0)
        val response = root.optJSONArray("response")
        if (results <= 0 || response == null || response.length() == 0) {
            throw FootballApiException("没有找到该 fixture")
        }

        return response.getJSONObject(0).toFixtureDto()
    }
}

class FootballApiException(
    message: String,
    val httpStatus: Int? = null,
    cause: Throwable? = null
) : IOException(message, cause)

private fun JSONObject.toFixtureDto(): FootballFixtureDto {
    val fixture = getJSONObject("fixture")
    val status = fixture.optJSONObject("status") ?: JSONObject()
    val league = optJSONObject("league") ?: JSONObject()
    val teams = optJSONObject("teams") ?: JSONObject()
    val home = teams.optJSONObject("home") ?: JSONObject()
    val away = teams.optJSONObject("away") ?: JSONObject()
    val goals = optJSONObject("goals") ?: JSONObject()

    return FootballFixtureDto(
        fixture = FixtureInfoDto(
            id = fixture.getLong("id"),
            date = fixture.optNullableString("date"),
            timestamp = fixture.optNullableLong("timestamp"),
            status = FixtureStatusDto(
                short = status.optString("short", "UNKNOWN"),
                long = status.optNullableString("long"),
                elapsed = status.optNullableInt("elapsed")
            )
        ),
        league = LeagueInfoDto(
            name = league.optNullableString("name"),
            round = league.optNullableString("round"),
            logo = league.optNullableString("logo")
        ),
        teams = TeamsInfoDto(
            home = home.toTeamInfoDto(),
            away = away.toTeamInfoDto()
        ),
        goals = GoalsInfoDto(
            home = goals.optNullableInt("home"),
            away = goals.optNullableInt("away")
        )
    )
}

private fun JSONObject.toTeamInfoDto(): TeamInfoDto = TeamInfoDto(
    id = optNullableLong("id"),
    name = optNullableString("name"),
    code = optNullableString("code"),
    logo = optNullableString("logo")
)

private fun JSONObject.optNullableString(name: String): String? =
    if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

private fun JSONObject.optNullableInt(name: String): Int? =
    if (isNull(name) || !has(name)) null else optInt(name)

private fun JSONObject.optNullableLong(name: String): Long? =
    if (isNull(name) || !has(name)) null else optLong(name)
