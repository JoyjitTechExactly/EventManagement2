package com.example.eventmanagement2.util

import android.graphics.Color
import com.example.eventmanagement2.data.model.Event
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.*

object ChartUtils {

    fun setupMonthlyEventsChart(
        chart: BarChart,
        events: List<Event>,
        month: Int,
        year: Int
    ) {
        val calendar = Calendar.getInstance()
        val daysInMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

        val days = (1..daysInMonth).map { it.toString() }
        val completedEntries = mutableListOf<BarEntry>()
        val upcomingEntries = mutableListOf<BarEntry>()
        val totalEntries = mutableListOf<BarEntry>()

        val currentTime = System.currentTimeMillis()
        val completedCounts = IntArray(daysInMonth) { 0 }
        val upcomingCounts = IntArray(daysInMonth) { 0 }
        val totalCounts = IntArray(daysInMonth) { 0 }

        // Count events per day (normalize to midnight)
        events.forEach { event ->
            calendar.time = event.date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            if (calendar.get(Calendar.MONTH) + 1 == month &&
                calendar.get(Calendar.YEAR) == year
            ) {
                val dayIndex = calendar.get(Calendar.DAY_OF_MONTH) - 1
                if (event.date.time >= currentTime) {
                    upcomingCounts[dayIndex]++
                } else {
                    completedCounts[dayIndex]++
                }
                totalCounts[dayIndex]++
            }
        }

        // Entries aligned to 0..daysInMonth-1 (so no +1 shift)
        for (i in 0 until daysInMonth) {
            completedEntries.add(BarEntry(i.toFloat(), completedCounts[i].toFloat()))
            upcomingEntries.add(BarEntry(i.toFloat(), upcomingCounts[i].toFloat()))
            totalEntries.add(BarEntry(i.toFloat(), totalCounts[i].toFloat()))
        }

        val completedDataSet = BarDataSet(completedEntries, "Completed").apply {
            color = Color.parseColor("#4CAF50")
            setDrawValues(false)
        }

        val upcomingDataSet = BarDataSet(upcomingEntries, "Upcoming").apply {
            color = Color.parseColor("#4FC3F7")
            setDrawValues(false)
        }

        val totalDataSet = BarDataSet(totalEntries, "Total").apply {
            color = Color.parseColor("#FFD700")
            valueTextColor = Color.GRAY
            valueTextSize = 10f
            setDrawValues(true)
        }

        val barData = BarData(completedDataSet, upcomingDataSet, totalDataSet)
        val groupSpace = 0.25f
        val barSpace = 0.05f
        val barWidth = 0.2f
        barData.barWidth = barWidth

        chart.apply {
            data = barData

            // X-Axis (labels are 1..days but entries start from 0)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = Color.WHITE
                axisLineColor = Color.WHITE
                valueFormatter = IndexAxisValueFormatter(days) // maps 0 -> "1", 1 -> "2", ...
                labelCount = days.size.coerceAtMost(10)
            }

            axisLeft.apply {
                axisMinimum = 0f
                val maxCount = totalCounts.maxOrNull() ?: 5
                axisMaximum = (maxCount + 1).toFloat()
                textColor = Color.WHITE
                gridColor = Color.GRAY
            }

            axisRight.isEnabled = false

            // Group bars starting at x = 0
            val groupWidth = barData.getGroupWidth(groupSpace, barSpace)
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = 0f + groupWidth * daysInMonth
            barData.groupBars(0f, groupSpace, barSpace)

            legend.apply {
                isEnabled = true
                textColor = Color.WHITE
                textSize = 12f
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }

            description.isEnabled = false

            setVisibleXRangeMaximum(7f)
            moveViewToX(0f)

            invalidate()
            animateY(800)
        }
    }
}