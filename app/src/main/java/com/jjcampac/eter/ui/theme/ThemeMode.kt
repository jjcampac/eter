/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.theme

/**
 * Modo de tema de la aplicación.
 *
 * - SISTEMA: respeta el tema del dispositivo (claro/oscuro del sistema).
 * - CLARO: fuerza tema claro independientemente del sistema.
 * - OSCURO: fuerza tema oscuro independientemente del sistema.
 */
enum class ThemeMode {
    SISTEMA,
    CLARO,
    OSCURO
}
