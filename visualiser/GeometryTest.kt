/*
 * Copyright (C) 2021 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.studio.visualiser

import androidx.compose.ui.geometry.Offset
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertTrue

class GeometryTest {
    @Test
    fun rayGradient1() {
        val ray = Ray(origin = Offset.Zero, directionVector = Offset(x = 1f, y = 0f))
        assertFloatEquals(expected = 0f, actual = ray.gradient)
    }

    @Test
    fun rayGradient2() {
        val ray = Ray(origin = Offset.Zero, directionVector = Offset(x = -1f, y = 0f))
        assertFloatEquals(expected = 0f, actual = ray.gradient)
    }

    @Test
    fun rayGradient3() {
        val ray = Ray(origin = Offset.Zero, directionVector = Offset(x = 1f, y = 1f))
        assertFloatEquals(expected = 1f, actual = ray.gradient)
    }

    @Test
    fun rayGradient4() {
        val ray = Ray(origin = Offset.Zero, directionVector = Offset(x = 2f, y = 3f))
        assertFloatEquals(expected = 1.5f, actual = ray.gradient)
    }

    @Test
    fun rayGradient5() {
        val ray = Ray(origin = Offset.Zero, directionVector = Offset(x = -6f, y = 66f))
        assertFloatEquals(expected = -11f, actual = ray.gradient)
    }

    @Test
    fun rayYIntercept1() {
        val ray = Ray(origin = Offset.Zero, directionVector = Offset(x = 1f, y = 0f))
        assertFloatEquals(expected = 0f, actual = ray.yIntercept)
    }

    @Test
    fun rayYIntercept2() {
        val ray = Ray(origin = Offset(x = 0f, y = -4f), directionVector = Offset(x = -3f, y = -5f))
        assertFloatEquals(expected = -4f, actual = ray.yIntercept)
    }

    @Test
    fun rayYIntercept3() {
        val ray = Ray(origin = Offset(x = 10f, y = 0f), directionVector = Offset(x = 1f, y = -2f))
        assertFloatEquals(expected = 20f, actual = ray.yIntercept)
    }

    @Test
    fun sweepAngle1() {
        assertFloatEquals(expected = 90f, actual = sweepAngle(from = 10f, to = 100f, direction = AngularDirection.Clockwise))
    }

    @Test
    fun sweepAngle2() {
        assertFloatEquals(expected = -270f, actual = sweepAngle(from = 10f, to = 100f, direction = AngularDirection.CounterClockwise))
    }

    @Test
    fun sweepAngle3() {
        assertFloatEquals(expected = 45f, actual = sweepAngle(from = -10f, to = 35f, direction = AngularDirection.Clockwise))
    }

    @Test
    fun sweepAngle4() {
        assertFloatEquals(expected = -45f, actual = sweepAngle(from = 35f, to = -10f, direction = AngularDirection.CounterClockwise))
    }

    @Test
    fun sweepAngle5() {
        assertFloatEquals(expected = -135f, actual = sweepAngle(from = 135f, to = 0f, direction = AngularDirection.CounterClockwise))
    }

    @Test
    fun quadraticRootsTwoSolutions() {
        assertFloatSetEquals(setOf(-1f/3, 1f), quadraticRoots(3f, -2f, -1f))
    }

    @Test
    fun quadraticRootsOneSolution() {
        assertFloatSetEquals(setOf(-4f), quadraticRoots(1f, 8f, 16f))
    }

    @Test
    fun quadraticRootsNoSolution() {
        assertFloatSetEquals(emptySet(), quadraticRoots(2f, 2f, 3f))
    }

    @Test
    fun rayCircleIntersect1() {
        val ray = Ray(origin = Offset.Zero, directionVector = Offset(x = 1f, y = 0f))
        val circle = Circle(x = 0f, y = 0f, r = 1f)
        assertOffsetSetEquals(expected = setOf(Offset(-1f, 0f), Offset(1f, 0f)), actual = rayCircleIntersect(ray, circle))
    }

    @Test
    fun rayCircleIntersect2() {
        val ray = Ray(origin = Offset.Zero, directionVector = Offset(x = 1f, y = 0f))
        val circle = Circle(x = -1f, y = -1f, r = 1f)
        assertOffsetSetEquals(expected = setOf(Offset(-1f, 0f)), actual = rayCircleIntersect(ray, circle))
    }

    @Test
    fun rayCircleIntersect3() {
        val ray = Ray(origin = Offset.Zero, directionVector = Offset(x = 1f, y = 0f))
        val circle = Circle(x = 5f, y = 4f, r = 5f)
        assertOffsetSetEquals(expected = setOf(Offset(2f, 0f), Offset(8f, 0f)), actual = rayCircleIntersect(ray, circle))
    }

    @Test
    fun rayCircleIntersect4() {
        val ray = Ray(origin = Offset(x = 0f, y = 1f), directionVector = Offset(x = 1f, y = 0f))
        val circle = Circle(x = 1f, y = 1f, r = 1f)
        assertOffsetSetEquals(expected = setOf(Offset(0f, 1f), Offset(2f, 1f)), actual = rayCircleIntersect(ray, circle))
    }

    private fun assertFloatEquals(expected: Float, actual: Float, precision: Float = 0.0001f) {
        assert(abs(expected - actual) < precision) { "Expected $expected, actual $actual" }
    }

    private fun <T : Comparable<T>> assertSetEquals(expected: Set<T>, actual: Set<T>, equalityFn: (a: T, b: T) -> Boolean) {
        assertTrue(message = "Expected $expected, actual $actual") {
            if (expected.size != actual.size) return@assertTrue false
            val expectedSorted = expected.sorted()
            val actualSorted = actual.sorted()
            expectedSorted.forEachIndexed { idx, value ->
                if (!equalityFn(value, actualSorted[idx])) return@assertTrue false
            }
            return@assertTrue true
        }
    }

    private fun assertFloatSetEquals(expected: Set<Float>, actual: Set<Float>, precision: Float = 0.0001f)
    = assertSetEquals(expected, actual) { a, b -> abs(a - b) < precision }

    private fun assertOffsetSetEquals(expected: Set<Offset>, actual: Set<Offset>, precision: Float = 0.0001f) {
        assertTrue(message = "Expected $expected, actual $actual") {
            if (expected.size != actual.size) return@assertTrue false
            val expectedSorted = expected.sortedBy { it.y }.sortedBy { it.x }
            val actualSorted = actual.sortedBy { it.y }.sortedBy { it.x }
            expectedSorted.forEachIndexed { idx, value ->
                if (abs(value.x - actualSorted[idx].x) > precision || abs(value.y - actualSorted[idx].y) > precision)
                    return@assertTrue false
            }
            return@assertTrue true
        }
    }
}
