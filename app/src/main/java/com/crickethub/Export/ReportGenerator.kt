package com.crickethub.export

import android.content.Context
import android.net.Uri
import com.crickethub.data.model.*
import java.text.SimpleDateFormat
import java.util.*

// ── Data containers for reports ──────────────────────────────

data class MatchReportData(
    val match: Match,
    val team1: Team,
    val team2: Team,
    val innings: List<Innings>,
    val ballsByInnings: Map<String, List<Ball>>,   // inningsId -> balls
    val playerNames: Map<String, String>,           // playerId -> name
    val awards: List<String> = emptyList()
)

data class TournamentReportData(
    val tournament: Tournament,
    val teams: List<Team>,
    val matches: List<Match>,
    val pointsTable: List<PointsRow> = emptyList(),
    val playerNames: Map<String, String> = emptyMap()
)

data class PointsRow(
    val teamName: String,
    val played: Int, val won: Int, val lost: Int, val tied: Int, val nr: Int,
    val nrr: Double, val points: Int
)

data class PlayerReportData(
    val player: Player,
    val team: Team?,
    val stats: PlayerStats,
    val hatTricks: Int = 0,
    val fastestFifty: Int = 0,
    val fastestHundred: Int = 0
)

data class TeamReportData(
    val team: Team,
    val matches: Int, val won: Int, val lost: Int, val tied: Int,
    val winPct: Double,
    val totalRuns: Int, val totalWickets: Int,
    val highestTotal: Int, val lowestTotal: Int,
    val currentStreak: String
)

data class ComparisonReportData(
    val player1: Player, val player2: Player,
    val team1: Team?, val team2: Team?,
    val stats1: PlayerStats, val stats2: PlayerStats
)

// ══════════════════════════════════════════════════════════════
// MATCH REPORT
// ══════════════════════════════════════════════════════════════

object MatchReportGenerator {

    fun generatePdf(context: Context, data: MatchReportData): Uri {
        val pdf = PdfBuilder(context)
        val m = data.match
        val dateStr = try {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(m.createdAt ?: "") ?: Date()
            )
        } catch (e: Exception) { m.createdAt ?: "" }

        pdf.drawBrandedHeader(
            title = "${data.team1.name} vs ${data.team2.name}",
            subtitle = "Match Scorecard",
            info = listOf(
                "${m.matchType} • ${m.totalOvers} Overs",
                "Date: $dateStr",
                m.venue ?: ""
            ).filter { it.isNotBlank() }
        )

        // Toss info
        val tossWinner = if (m.tossWinnerId == data.team1.id) data.team1.name else data.team2.name
        val tossDecision = if (m.battingFirstId == m.tossWinnerId) "bat" else "bowl"
        pdf.drawText("Toss: $tossWinner won the toss and elected to $tossDecision", size = 9f)
        pdf.space(4f)

        // Result banner
        m.resultText?.let { pdf.drawResultBanner(it) }

        // Each innings
        data.innings.sortedBy { it.inningsNo }.forEach { inn ->
            val balls = data.ballsByInnings[inn.id] ?: emptyList()
            val battingTeam = if (inn.battingTeamId == data.team1.id) data.team1.name else data.team2.name
            val bowlingTeam = if (inn.bowlingTeamId == data.team1.id) data.team1.name else data.team2.name
            val legalBalls = balls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
            val overs = "${legalBalls / 6}.${legalBalls % 6}"

            pdf.drawSectionTitle("${ordinal(inn.inningsNo)} Innings — $battingTeam: ${inn.totalRuns}/${inn.totalWickets} ($overs ov)")

            // Batting scorecard
            val batsmen = balls.filter { it.batsmanId != null }
                .groupBy { it.batsmanId!! }
            val batRows = batsmen.map { (bId, bBalls) ->
                val name = data.playerNames[bId] ?: bId.take(8)
                val runs = bBalls.sumOf { it.runsOffBat.toLong() }.toInt()
                val faced = bBalls.count { it.extrasType != "wide" }
                val fours = bBalls.count { it.isBoundary && !it.isSix }
                val sixes = bBalls.count { it.isSix }
                val sr = if (faced > 0) "%.1f".format(runs * 100.0 / faced) else "0.0"
                val isOut = bBalls.any { it.isWicket && it.wicketType != "retired_hurt" }
                val howOut = if (isOut) bBalls.firstOrNull { it.isWicket }?.wicketType?.replace("_", " ") ?: "out" else "not out"
                listOf(name, howOut, "$runs", "$faced", "$fours", "$sixes", sr)
            }.sortedByDescending { it[2].toIntOrNull() ?: 0 }

            if (batRows.isNotEmpty()) {
                pdf.drawTable(
                    headers = listOf("Batter", "Dismissal", "R", "B", "4s", "6s", "SR"),
                    rows = batRows,
                    colWidths = listOf(3.5f, 3f, 1f, 1f, 1f, 1f, 1.5f)
                )
            }

            // Bowling scorecard
            val bowlers = balls.filter { it.bowlerId != null }
                .groupBy { it.bowlerId!! }
            val bowlRows = bowlers.map { (bId, bBalls) ->
                val name = data.playerNames[bId] ?: bId.take(8)
                val legal = bBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
                val ov = "${legal / 6}.${legal % 6}"
                val runs = bBalls.sumOf { b: Ball ->
                    when (b.extrasType) {
                        "bye", "leg_bye" -> 0
                        else -> b.runsOffBat + (b.extrasRuns ?: 0)
                    }.toLong()
                }.toInt()
                val wkts = bBalls.count { it.isWicket && it.wicketType !in listOf("run_out", "obstructing", "retired_hurt") }
                val eco = if (legal > 0) "%.1f".format(runs * 6.0 / legal) else "0.0"
                // Maidens
                val maidenCount = bBalls.groupBy { it.overNo }.count { (_, ob) ->
                    ob.all { b -> b.runsOffBat == 0 && (b.extrasRuns ?: 0) == 0 && b.extrasType != "wide" && b.extrasType != "no_ball" } && ob.count { b -> b.extrasType != "wide" && b.extrasType != "no_ball" } >= 6
                }
                val dots = bBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" && it.runsOffBat == 0 && (it.extrasRuns ?: 0) == 0 }
                listOf(name, ov, "$maidenCount", "$runs", "$wkts", "$dots", eco)
            }

            if (bowlRows.isNotEmpty()) {
                pdf.space(4f)
                pdf.drawTable(
                    headers = listOf("Bowler", "O", "M", "R", "W", "Dots", "Eco"),
                    rows = bowlRows,
                    colWidths = listOf(3.5f, 1.2f, 1f, 1f, 1f, 1f, 1.3f)
                )
            }

            // Extras
            val wides = balls.count { it.extrasType == "wide" }
            val noBalls = balls.count { it.extrasType == "no_ball" }
            val byes = balls.sumOf { (if (it.extrasType == "bye") (it.extrasRuns ?: 0) else 0).toLong() }.toInt()
            val legByes = balls.sumOf { (if (it.extrasType == "leg_bye") (it.extrasRuns ?: 0) else 0).toLong() }.toInt()
            val totalExtras = wides + noBalls + byes + legByes +
                    balls.filter { it.extrasType == "wide" }.sumOf { (it.extrasRuns ?: 1).toLong() }.toInt() +
                    balls.filter { it.extrasType == "no_ball" }.sumOf { 1.toLong() }.toInt()
            pdf.drawText("Extras: $totalExtras (W $wides, NB $noBalls, B $byes, LB $legByes)", size = 8f)

            // Fall of wickets
            val wickets = balls.filter { it.isWicket && it.wicketType != "retired_hurt" }
                .sortedWith(compareBy({ it.overNo }, { it.ballNo }))
            if (wickets.isNotEmpty()) {
                var cumRuns = 0
                val fowParts = mutableListOf<String>()
                wickets.forEachIndexed { idx, w ->
                    cumRuns = balls.filter { b ->
                        b.overNo < w.overNo || (b.overNo == w.overNo && b.ballNo <= w.ballNo)
                    }.sumOf { b: Ball ->
                        when {
                            b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
                            b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
                            else -> b.runsOffBat + (b.extrasRuns ?: 0)
                        }.toLong()
                    }.toInt()
                    fowParts.add("${idx + 1}-$cumRuns (${w.overNo}.${w.ballNo})")
                }
                pdf.drawText("FOW: ${fowParts.joinToString(", ")}", size = 7.5f)
            }
            pdf.space(8f)
        }

        // Awards
        if (data.awards.isNotEmpty()) {
            pdf.drawSectionTitle("Awards")
            data.awards.forEach { pdf.drawText("★ $it", size = 9f) }
        }

        return pdf.save("match_${data.match.id.take(8)}.pdf")
    }

    fun generateCsv(context: Context, data: MatchReportData): Uri {
        val sb = StringBuilder()
        sb.appendLine("CricketHub Match Report")
        sb.appendLine("${data.team1.name} vs ${data.team2.name}")
        sb.appendLine("${data.match.matchType},${data.match.totalOvers} overs")
        sb.appendLine("Result,${data.match.resultText ?: ""}")
        sb.appendLine()

        data.innings.sortedBy { it.inningsNo }.forEach { inn ->
            val balls = data.ballsByInnings[inn.id] ?: emptyList()
            val teamName = if (inn.battingTeamId == data.team1.id) data.team1.name else data.team2.name
            sb.appendLine("${ordinal(inn.inningsNo)} Innings - $teamName: ${inn.totalRuns}/${inn.totalWickets}")
            sb.appendLine()

            // Batting
            sb.appendLine("Batter,Dismissal,Runs,Balls,4s,6s,SR")
            balls.filter { it.batsmanId != null }.groupBy { it.batsmanId!! }.forEach { (bId, bBalls) ->
                val name = data.playerNames[bId] ?: bId.take(8)
                val runs = bBalls.sumOf { it.runsOffBat.toLong() }.toInt()
                val faced = bBalls.count { it.extrasType != "wide" }
                val fours = bBalls.count { it.isBoundary && !it.isSix }
                val sixes = bBalls.count { it.isSix }
                val sr = if (faced > 0) "%.1f".format(runs * 100.0 / faced) else "0.0"
                val isOut = bBalls.any { it.isWicket && it.wicketType != "retired_hurt" }
                val howOut = if (isOut) bBalls.firstOrNull { it.isWicket }?.wicketType?.replace("_", " ") ?: "out" else "not out"
                sb.appendLine("$name,$howOut,$runs,$faced,$fours,$sixes,$sr")
            }
            sb.appendLine()

            // Bowling
            sb.appendLine("Bowler,Overs,Maidens,Runs,Wickets,Economy")
            balls.filter { it.bowlerId != null }.groupBy { it.bowlerId!! }.forEach { (bId, bBalls) ->
                val name = data.playerNames[bId] ?: bId.take(8)
                val legal = bBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
                val ov = "${legal / 6}.${legal % 6}"
                val runs = bBalls.sumOf { b: Ball ->
                    when (b.extrasType) { "bye", "leg_bye" -> 0; else -> b.runsOffBat + (b.extrasRuns ?: 0) }.toLong()
                }.toInt()
                val wkts = bBalls.count { it.isWicket && it.wicketType !in listOf("run_out", "obstructing", "retired_hurt") }
                val eco = if (legal > 0) "%.1f".format(runs * 6.0 / legal) else "0.0"
                sb.appendLine("$name,$ov,0,$runs,$wkts,$eco")
            }
            sb.appendLine()
        }
        return saveCsvFile(context, "match_${data.match.id.take(8)}.csv", sb.toString())
    }

    private fun ordinal(n: Int) = when (n) { 1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${n}th" }
}

// ══════════════════════════════════════════════════════════════
// TOURNAMENT REPORT
// ══════════════════════════════════════════════════════════════

object TournamentReportGenerator {

    fun generatePdf(context: Context, data: TournamentReportData): Uri {
        val pdf = PdfBuilder(context)
        val t = data.tournament

        pdf.drawBrandedHeader(
            title = t.name,
            subtitle = "Tournament Report",
            info = listOf(
                "Format: ${t.format ?: "N/A"}",
                "Status: ${t.status.replaceFirstChar { it.uppercase() }}",
                t.startDate?.let { "Start: $it" } ?: ""
            ).filter { it.isNotBlank() }
        )

        // Summary
        pdf.drawSectionTitle("Summary")
        pdf.drawKeyValuePairs(listOf(
            "Teams" to "${data.teams.size}",
            "Matches" to "${data.matches.size}",
            "Completed" to "${data.matches.count { it.status == "completed" }}",
            "Status" to t.status.replaceFirstChar { it.uppercase() }
        ))

        // Points Table
        if (data.pointsTable.isNotEmpty()) {
            pdf.drawSectionTitle("Points Table")
            val rows = data.pointsTable.map { r ->
                listOf(r.teamName, "${r.played}", "${r.won}", "${r.lost}", "${r.tied}",
                    "${r.nr}", "%.3f".format(r.nrr), "${r.points}")
            }
            pdf.drawTable(
                headers = listOf("Team", "P", "W", "L", "T", "NR", "NRR", "Pts"),
                rows = rows,
                colWidths = listOf(3f, 1f, 1f, 1f, 1f, 1f, 1.5f, 1f)
            )
        }

        // Fixtures / Results
        pdf.drawSectionTitle("Fixtures & Results")
        val matchRows = data.matches.sortedBy { it.matchNumber }.map { m ->
            val t1 = data.teams.find { it.id == m.team1Id }?.shortName ?: m.team1Id.take(6)
            val t2 = data.teams.find { it.id == m.team2Id }?.shortName ?: m.team2Id.take(6)
            listOf(
                "M${m.matchNumber ?: ""}",
                "$t1 vs $t2",
                m.status.replaceFirstChar { it.uppercase() },
                m.resultText?.take(40) ?: "—"
            )
        }
        if (matchRows.isNotEmpty()) {
            pdf.drawTable(
                headers = listOf("#", "Match", "Status", "Result"),
                rows = matchRows,
                colWidths = listOf(1f, 2.5f, 1.5f, 5f),
                rightAlignFrom = 99 // left-align all
            )
        }

        return pdf.save("tournament_${t.id.take(8)}.pdf")
    }

    fun generateCsv(context: Context, data: TournamentReportData): Uri {
        val sb = StringBuilder()
        sb.appendLine("CricketHub Tournament Report")
        sb.appendLine("Name,${data.tournament.name}")
        sb.appendLine("Format,${data.tournament.format ?: ""}")
        sb.appendLine("Status,${data.tournament.status}")
        sb.appendLine()

        if (data.pointsTable.isNotEmpty()) {
            sb.appendLine("Points Table")
            sb.appendLine("Team,Played,Won,Lost,Tied,NR,NRR,Points")
            data.pointsTable.forEach { r ->
                sb.appendLine("${r.teamName},${r.played},${r.won},${r.lost},${r.tied},${r.nr},${"%.3f".format(r.nrr)},${r.points}")
            }
            sb.appendLine()
        }

        sb.appendLine("Fixtures")
        sb.appendLine("Match No,Team 1,Team 2,Status,Result")
        data.matches.sortedBy { it.matchNumber }.forEach { m ->
            val t1 = data.teams.find { it.id == m.team1Id }?.name ?: ""
            val t2 = data.teams.find { it.id == m.team2Id }?.name ?: ""
            sb.appendLine("${m.matchNumber ?: ""},${t1},${t2},${m.status},${m.resultText ?: ""}")
        }
        return saveCsvFile(context, "tournament_${data.tournament.id.take(8)}.csv", sb.toString())
    }
}

// ══════════════════════════════════════════════════════════════
// PLAYER CAREER REPORT
// ══════════════════════════════════════════════════════════════

object PlayerReportGenerator {

    fun generatePdf(context: Context, data: PlayerReportData): Uri {
        val pdf = PdfBuilder(context)
        val p = data.player
        val s = data.stats

        pdf.drawBrandedHeader(
            title = p.fullName,
            subtitle = "Career Report",
            info = listOf(
                "Team: ${data.team?.name ?: "N/A"}",
                "Role: ${p.role?.replaceFirstChar { it.uppercase() } ?: "N/A"}",
                p.battingHand?.let { "Batting: ${it.replaceFirstChar { c -> c.uppercase() }}-hand" } ?: "",
                p.bowlingStyle?.let { "Bowling: $it" } ?: ""
            ).filter { it.isNotBlank() }
        )

        // Career overview
        pdf.drawSectionTitle("Career Overview")
        pdf.drawKeyValuePairs(listOf(
            "Matches" to "${s.matches}", "Runs" to "${s.runs}",
            "Wickets" to "${s.wickets}", "Catches" to "${s.catches}"
        ))

        // Batting stats
        pdf.drawSectionTitle("Batting Statistics")
        pdf.drawTable(
            headers = listOf("Stat", "Value"),
            rows = listOf(
                listOf("Innings", "${s.innings}"), listOf("Runs", "${s.runs}"),
                listOf("Balls Faced", "${s.ballsFaced}"), listOf("Not Outs", "${s.notOuts}"),
                listOf("Highest Score", "${s.highestScore}"),
                listOf("Average", "%.2f".format(s.average)),
                listOf("Strike Rate", "%.2f".format(s.strikeRate)),
                listOf("50s / 100s", "${s.fifties} / ${s.hundreds}"),
                listOf("4s / 6s", "${s.fours} / ${s.sixes}"),
                listOf("Boundary %", "%.1f%%".format(s.boundaryPercent)),
                listOf("Dot Ball %", "%.1f%%".format(s.dotBallPercent)),
                listOf("Ducks", "${s.ducks}")
            ),
            colWidths = listOf(3f, 2f),
            rightAlignFrom = 1
        )

        // Bowling stats
        pdf.drawSectionTitle("Bowling Statistics")
        pdf.drawTable(
            headers = listOf("Stat", "Value"),
            rows = listOf(
                listOf("Overs", "%.1f".format(s.oversBowled)),
                listOf("Wickets", "${s.wickets}"),
                listOf("Runs Conceded", "${s.runsConceded}"),
                listOf("Economy", "%.2f".format(s.economy)),
                listOf("Average", "%.2f".format(s.bowlingAverage)),
                listOf("Strike Rate", "%.2f".format(s.bowlingStrikeRate)),
                listOf("Best Bowling", s.bestBowling),
                listOf("Maidens", "${s.maidens}"),
                listOf("3W / 5W Hauls", "${s.threeWicketHauls} / ${s.fiveWicketHauls}"),
                listOf("Wides / No Balls", "${s.wides} / ${s.noBalls}")
            ),
            colWidths = listOf(3f, 2f),
            rightAlignFrom = 1
        )

        // Fielding
        pdf.drawSectionTitle("Fielding Statistics")
        pdf.drawKeyValuePairs(listOf(
            "Catches" to "${s.catches}", "Run Outs" to "${s.runOuts}",
            "Stumpings" to "${s.stumpings}", "Missed" to "${s.missedChances}"
        ))

        // Records
        pdf.drawSectionTitle("Records")
        pdf.drawKeyValuePairs(listOf(
            "Fastest 50" to if (data.fastestFifty > 0) "${data.fastestFifty} balls" else "N/A",
            "Fastest 100" to if (data.fastestHundred > 0) "${data.fastestHundred} balls" else "N/A",
            "Hat-tricks" to "${data.hatTricks}",
            "Highest Score" to "${s.highestScore}"
        ))

        return pdf.save("player_${p.id.take(8)}.pdf")
    }

    fun generateCsv(context: Context, data: PlayerReportData): Uri {
        val p = data.player; val s = data.stats
        val sb = StringBuilder()
        sb.appendLine("CricketHub Player Career Report")
        sb.appendLine("Name,${p.fullName}")
        sb.appendLine("Team,${data.team?.name ?: ""}")
        sb.appendLine("Role,${p.role ?: ""}")
        sb.appendLine()
        sb.appendLine("Batting")
        sb.appendLine("Stat,Value")
        sb.appendLine("Matches,${s.matches}")
        sb.appendLine("Innings,${s.innings}")
        sb.appendLine("Runs,${s.runs}")
        sb.appendLine("Balls Faced,${s.ballsFaced}")
        sb.appendLine("Highest Score,${s.highestScore}")
        sb.appendLine("Average,${"%.2f".format(s.average)}")
        sb.appendLine("Strike Rate,${"%.2f".format(s.strikeRate)}")
        sb.appendLine("50s,${s.fifties}")
        sb.appendLine("100s,${s.hundreds}")
        sb.appendLine("4s,${s.fours}")
        sb.appendLine("6s,${s.sixes}")
        sb.appendLine()
        sb.appendLine("Bowling")
        sb.appendLine("Stat,Value")
        sb.appendLine("Overs,${"%.1f".format(s.oversBowled)}")
        sb.appendLine("Wickets,${s.wickets}")
        sb.appendLine("Runs Conceded,${s.runsConceded}")
        sb.appendLine("Economy,${"%.2f".format(s.economy)}")
        sb.appendLine("Best Bowling,${s.bestBowling}")
        sb.appendLine()
        sb.appendLine("Fielding")
        sb.appendLine("Catches,${s.catches}")
        sb.appendLine("Run Outs,${s.runOuts}")
        sb.appendLine("Stumpings,${s.stumpings}")
        return saveCsvFile(context, "player_${p.id.take(8)}.csv", sb.toString())
    }
}

// ══════════════════════════════════════════════════════════════
// PLAYER COMPARISON REPORT
// ══════════════════════════════════════════════════════════════

object ComparisonReportGenerator {

    fun generatePdf(context: Context, data: ComparisonReportData): Uri {
        val pdf = PdfBuilder(context)
        val s1 = data.stats1; val s2 = data.stats2

        pdf.drawBrandedHeader(
            title = "${data.player1.fullName} vs ${data.player2.fullName}",
            subtitle = "Player Comparison Report",
            info = listOf(
                "${data.team1?.name ?: "N/A"} vs ${data.team2?.name ?: "N/A"}"
            )
        )

        // Batting comparison
        pdf.drawSectionTitle("Batting Comparison")
        pdf.drawTable(
            headers = listOf("Stat", data.player1.fullName.take(15), data.player2.fullName.take(15)),
            rows = listOf(
                listOf("Matches", "${s1.matches}", "${s2.matches}"),
                listOf("Innings", "${s1.innings}", "${s2.innings}"),
                listOf("Runs", "${s1.runs}", "${s2.runs}"),
                listOf("Highest", "${s1.highestScore}", "${s2.highestScore}"),
                listOf("Average", "%.2f".format(s1.average), "%.2f".format(s2.average)),
                listOf("SR", "%.2f".format(s1.strikeRate), "%.2f".format(s2.strikeRate)),
                listOf("50s", "${s1.fifties}", "${s2.fifties}"),
                listOf("100s", "${s1.hundreds}", "${s2.hundreds}"),
                listOf("4s", "${s1.fours}", "${s2.fours}"),
                listOf("6s", "${s1.sixes}", "${s2.sixes}"),
                listOf("Ducks", "${s1.ducks}", "${s2.ducks}")
            ),
            colWidths = listOf(2f, 2f, 2f),
            rightAlignFrom = 1
        )

        // Bowling comparison
        pdf.drawSectionTitle("Bowling Comparison")
        pdf.drawTable(
            headers = listOf("Stat", data.player1.fullName.take(15), data.player2.fullName.take(15)),
            rows = listOf(
                listOf("Overs", "%.1f".format(s1.oversBowled), "%.1f".format(s2.oversBowled)),
                listOf("Wickets", "${s1.wickets}", "${s2.wickets}"),
                listOf("Economy", "%.2f".format(s1.economy), "%.2f".format(s2.economy)),
                listOf("Avg", "%.2f".format(s1.bowlingAverage), "%.2f".format(s2.bowlingAverage)),
                listOf("SR", "%.2f".format(s1.bowlingStrikeRate), "%.2f".format(s2.bowlingStrikeRate)),
                listOf("Best", s1.bestBowling, s2.bestBowling),
                listOf("5W", "${s1.fiveWicketHauls}", "${s2.fiveWicketHauls}")
            ),
            colWidths = listOf(2f, 2f, 2f),
            rightAlignFrom = 1
        )

        // Fielding comparison
        pdf.drawSectionTitle("Fielding Comparison")
        pdf.drawTable(
            headers = listOf("Stat", data.player1.fullName.take(15), data.player2.fullName.take(15)),
            rows = listOf(
                listOf("Catches", "${s1.catches}", "${s2.catches}"),
                listOf("Run Outs", "${s1.runOuts}", "${s2.runOuts}"),
                listOf("Stumpings", "${s1.stumpings}", "${s2.stumpings}")
            ),
            colWidths = listOf(2f, 2f, 2f),
            rightAlignFrom = 1
        )

        return pdf.save("comparison_${data.player1.id.take(4)}_${data.player2.id.take(4)}.pdf")
    }

    fun generateCsv(context: Context, data: ComparisonReportData): Uri {
        val s1 = data.stats1; val s2 = data.stats2
        val n1 = data.player1.fullName; val n2 = data.player2.fullName
        val sb = StringBuilder()
        sb.appendLine("CricketHub Player Comparison")
        sb.appendLine("Stat,$n1,$n2")
        sb.appendLine("Team,${data.team1?.name ?: ""},${data.team2?.name ?: ""}")
        sb.appendLine()
        sb.appendLine("Batting")
        sb.appendLine("Stat,$n1,$n2")
        sb.appendLine("Matches,${s1.matches},${s2.matches}")
        sb.appendLine("Runs,${s1.runs},${s2.runs}")
        sb.appendLine("Avg,${"%.2f".format(s1.average)},${"%.2f".format(s2.average)}")
        sb.appendLine("SR,${"%.2f".format(s1.strikeRate)},${"%.2f".format(s2.strikeRate)}")
        sb.appendLine("50s,${s1.fifties},${s2.fifties}")
        sb.appendLine("100s,${s1.hundreds},${s2.hundreds}")
        sb.appendLine()
        sb.appendLine("Bowling")
        sb.appendLine("Wickets,${s1.wickets},${s2.wickets}")
        sb.appendLine("Economy,${"%.2f".format(s1.economy)},${"%.2f".format(s2.economy)}")
        sb.appendLine("Best,${s1.bestBowling},${s2.bestBowling}")
        return saveCsvFile(context, "comparison_${data.player1.id.take(4)}_vs_${data.player2.id.take(4)}.csv", sb.toString())
    }
}

// ══════════════════════════════════════════════════════════════
// TEAM STATS REPORT
// ══════════════════════════════════════════════════════════════

object TeamReportGenerator {

    fun generatePdf(context: Context, data: TeamReportData): Uri {
        val pdf = PdfBuilder(context)

        pdf.drawBrandedHeader(
            title = data.team.name,
            subtitle = "Team Statistics Report",
            info = listOf(
                "Category: ${data.team.category ?: "N/A"}",
                data.team.shortName?.let { "Short Name: $it" } ?: ""
            ).filter { it.isNotBlank() }
        )

        pdf.drawSectionTitle("Match Summary")
        pdf.drawKeyValuePairs(listOf(
            "Played" to "${data.matches}", "Won" to "${data.won}",
            "Lost" to "${data.lost}", "Tied" to "${data.tied}",
            "Win %" to "%.1f%%".format(data.winPct),
            "Current Streak" to data.currentStreak
        ))

        pdf.drawSectionTitle("Performance")
        pdf.drawKeyValuePairs(listOf(
            "Total Runs" to "${data.totalRuns}",
            "Total Wickets" to "${data.totalWickets}",
            "Highest Total" to "${data.highestTotal}",
            "Lowest Total" to "${data.lowestTotal}"
        ))

        return pdf.save("team_${data.team.id.take(8)}.pdf")
    }

    fun generateCsv(context: Context, data: TeamReportData): Uri {
        val sb = StringBuilder()
        sb.appendLine("CricketHub Team Report")
        sb.appendLine("Team,${data.team.name}")
        sb.appendLine()
        sb.appendLine("Stat,Value")
        sb.appendLine("Matches,${data.matches}")
        sb.appendLine("Won,${data.won}")
        sb.appendLine("Lost,${data.lost}")
        sb.appendLine("Tied,${data.tied}")
        sb.appendLine("Win %,${"%.1f".format(data.winPct)}")
        sb.appendLine("Total Runs,${data.totalRuns}")
        sb.appendLine("Total Wickets,${data.totalWickets}")
        sb.appendLine("Highest Total,${data.highestTotal}")
        sb.appendLine("Lowest Total,${data.lowestTotal}")
        sb.appendLine("Current Streak,${data.currentStreak}")
        return saveCsvFile(context, "team_${data.team.id.take(8)}.csv", sb.toString())
    }
}