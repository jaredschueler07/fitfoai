package com.runningcoach.v2.presentation.screen.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.entity.DataSource
import com.runningcoach.v2.data.local.entity.RunSessionEntity
import com.runningcoach.v2.domain.repository.RunSession
import com.runningcoach.v2.domain.repository.RunSessionRepository
import com.runningcoach.v2.domain.repository.SessionSource
import com.runningcoach.v2.domain.usecase.GetRunSessionsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RunListItem(
    val id: Long,
    val date: String,
    val distanceMiles: String,
    val pacePerMile: String,
    val source: SessionSource
)

data class WeeklySourceStats(
    val fitfoMiles: Double = 0.0,
    val fitfoRuns: Int = 0,
    val fitMiles: Double = 0.0,
    val fitRuns: Int = 0
) {
    val totalMiles: Double get() = fitfoMiles + fitMiles
    val totalRuns: Int get() = fitfoRuns + fitRuns
}

class ProgressViewModel(
    private val getRunSessionsUseCase: GetRunSessionsUseCase,
    private val database: FITFOAIDatabase
) : ViewModel() {

    private val _runItems = MutableStateFlow<List<RunListItem>>(emptyList())
    val runItems: StateFlow<List<RunListItem>> = _runItems.asStateFlow()

    private val _weeklyStats = MutableStateFlow(WeeklySourceStats())
    val weeklyStats: StateFlow<WeeklySourceStats> = _weeklyStats.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = database.userDao().getCurrentUser().map { it }.catch { emit(null) }.collectLatest { userEntity ->
                val userId = userEntity?.id ?: return@collectLatest

                // Load recent runs (20)
                viewModelScope.launch(Dispatchers.IO) {
                    getRunSessionsUseCase.getRunSessions(userId, limit = 20).collect { sessions ->
                        _runItems.value = sessions.mapNotNull { it.toRunListItem() }
                    }
                }

                // Compute last 7 days stats per source directly from DAO
                viewModelScope.launch(Dispatchers.IO) {
                    val now = System.currentTimeMillis()
                    val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
                    val dao = database.runSessionDao()
                    val all = dao.getSessionsInDateRange(userId, sevenDaysAgo, now)
                    _weeklyStats.value = computeWeeklyStats(all)
                }
            }
        }
    }

    private fun computeWeeklyStats(list: List<RunSessionEntity>): WeeklySourceStats {
        var fitfoMiles = 0.0
        var fitfoRuns = 0
        var fitMiles = 0.0
        var fitRuns = 0

        list.forEach { e ->
            if (e.distance > 0f) {
                val miles = e.distance / 1609.344
                when (e.source) {
                    DataSource.GOOGLE_FIT -> {
                        fitMiles += miles
                        fitRuns += 1
                    }
                    else -> {
                        fitfoMiles += miles
                        fitfoRuns += 1
                    }
                }
            }
        }
        return WeeklySourceStats(
            fitfoMiles = round1(fitfoMiles),
            fitfoRuns = fitfoRuns,
            fitMiles = round1(fitMiles),
            fitRuns = fitRuns
        )
    }

    private fun round1(v: Double): Double = kotlin.math.round(v * 10.0) / 10.0
}

private fun RunSession.toRunListItem(): RunListItem? {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val dateStr = sdf.format(Date(this.startTime))
    val dist = this.distance ?: 0f
    val miles = dist / 1609.344f
    val paceMinPerKm = this.averagePace ?: 0f
    val pacePerMile = if (paceMinPerKm > 0f) paceMinPerKm / 0.621371f else 0f
    val paceStr = if (pacePerMile > 0f) {
        val min = pacePerMile.toInt()
        val sec = ((pacePerMile - min) * 60f).toInt()
        String.format("%d:%02d /mi", min, sec)
    } else "--:-- /mi"
    val src = this.source ?: SessionSource.FITFOAI
    return RunListItem(
        id = this.id,
        date = dateStr,
        distanceMiles = String.format(Locale.US, "%.2f mi", miles),
        pacePerMile = paceStr,
        source = src
    )
}

