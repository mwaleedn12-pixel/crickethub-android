package com.crickethub.data.local

import com.crickethub.data.model.Ball
import com.crickethub.data.model.BallInsert
import com.crickethub.data.model.Innings
import java.util.UUID

// ── Ball ↔ BallEntity ──

fun Ball.toEntity(syncStatus: String = SyncStatus.SYNCED): BallEntity = BallEntity(
    id = id,
    inningsId = inningsId,
    overNo = overNo,
    ballNo = ballNo,
    deliveryNo = deliveryNo,
    batsmanId = batsmanId,
    nonStrikerId = nonStrikerId,
    bowlerId = bowlerId,
    runsOffBat = runsOffBat,
    extrasRuns = extrasRuns,
    extrasType = extrasType,
    isWicket = isWicket,
    wicketType = wicketType,
    fielderName = fielderName,
    isBoundary = isBoundary,
    isSix = isSix,
    inningsPhase = inningsPhase,
    commentary = commentary,
    dismissedBatsmanId = dismissedBatsmanId,
    createdAt = createdAt,
    syncStatus = syncStatus
)

fun BallEntity.toBall(): Ball = Ball(
    id = id,
    inningsId = inningsId,
    overNo = overNo,
    ballNo = ballNo,
    deliveryNo = deliveryNo,
    batsmanId = batsmanId,
    nonStrikerId = nonStrikerId,
    bowlerId = bowlerId,
    runsOffBat = runsOffBat,
    extrasRuns = extrasRuns,
    extrasType = extrasType,
    isWicket = isWicket,
    wicketType = wicketType,
    fielderName = fielderName,
    isBoundary = isBoundary,
    isSix = isSix,
    inningsPhase = inningsPhase,
    commentary = commentary,
    dismissedBatsmanId = dismissedBatsmanId,
    createdAt = createdAt
)

/** Create a BallEntity from a BallInsert (no server id yet → generate a local UUID). */
fun BallInsert.toEntity(): BallEntity = BallEntity(
    id = UUID.randomUUID().toString(),
    inningsId = inningsId,
    overNo = overNo,
    ballNo = ballNo,
    deliveryNo = deliveryNo,
    batsmanId = batsmanId,
    nonStrikerId = nonStrikerId,
    bowlerId = bowlerId,
    runsOffBat = runsOffBat,
    extrasRuns = extrasRuns,
    extrasType = extrasType,
    isWicket = isWicket,
    wicketType = wicketType,
    fielderName = fielderName,
    isBoundary = isBoundary,
    isSix = isSix,
    inningsPhase = inningsPhase,
    commentary = commentary,
    dismissedBatsmanId = dismissedBatsmanId,
    syncStatus = SyncStatus.PENDING
)

// ── Innings ↔ InningsEntity ──

fun Innings.toEntity(syncStatus: String = SyncStatus.SYNCED): InningsEntity = InningsEntity(
    id = id,
    matchId = matchId,
    inningsNo = inningsNo,
    battingTeamId = battingTeamId,
    bowlingTeamId = bowlingTeamId,
    totalRuns = totalRuns,
    totalWickets = totalWickets,
    totalBalls = totalBalls,
    extrasTotal = extrasTotal,
    wides = wides,
    noBalls = noBalls,
    byes = byes,
    legByes = legByes,
    status = status,
    syncStatus = syncStatus
)

fun InningsEntity.toInnings(): Innings = Innings(
    id = id,
    matchId = matchId,
    inningsNo = inningsNo,
    battingTeamId = battingTeamId,
    bowlingTeamId = bowlingTeamId,
    totalRuns = totalRuns,
    totalWickets = totalWickets,
    totalBalls = totalBalls,
    extrasTotal = extrasTotal,
    wides = wides,
    noBalls = noBalls,
    byes = byes,
    legByes = legByes,
    status = status
)