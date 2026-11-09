/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter

import com.jjcampac.eter.ui.components.calcularIniciales
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas de la función de iniciales del icono de respaldo de EstacionIcono.
 * Cubre los ejemplos del requisito y los casos límite (vacío, números, emojis).
 */
class EstacionIconoTest {

    @Test
    fun `iniciales de ejemplo del requisito`() {
        assertEquals("LS", calcularIniciales("La Ser"))
        assertEquals("RR", calcularIniciales("RNE Radio 3"))
    }

    @Test
    fun `dos palabras normales`() {
        assertEquals("SG", calcularIniciales("SomaFM Groove Salad"))
        assertEquals("EF", calcularIniciales("Europa FM"))
    }

    @Test
    fun `una sola palabra usa dos primeras letras`() {
        assertEquals("RA", calcularIniciales("Radio"))
        assertEquals("CA", calcularIniciales("Cadena"))
    }

    @Test
    fun `nombre vacio o sin letras devuelve interrogante`() {
        assertEquals("?", calcularIniciales(""))
        assertEquals("?", calcularIniciales("   "))
        assertEquals("?", calcularIniciales("😀"))
    }

    @Test
    fun `las palabras de solo numeros o emojis no aportan inicial`() {
        assertEquals("PR", calcularIniciales("40 Principales"))
        assertEquals("LO", calcularIniciales("Los 40"))
        assertEquals("RA", calcularIniciales("😀 Radio"))
        assertEquals("R3", calcularIniciales("R3"))
    }

    @Test
    fun `los simbolos no cuentan como palabra`() {
        assertEquals("LS", calcularIniciales("La—Ser"))
        assertEquals("CE", calcularIniciales("Cadena *100* España"))
    }
}
