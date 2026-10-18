package com.example.data.repository

import com.example.data.user.Favorito
import com.example.data.user.FavoritoDao

/**
 * Acceso genérico a los favoritos del usuario (`user.db`) para tipos que no
 * pertenecen a un repositorio de contenido — p. ej. el Evangelio del día,
 * cuyo corazón en el header guarda con la clave `evangelio-<fecha>` (igual
 * que la RN en AsyncStorage).
 */
class FavoritosRepository(private val favoritoDao: FavoritoDao) {

    suspend fun getFavorito(id: String): Favorito? = favoritoDao.getFavoritoById(id)

    /**
     * Guarda [favorito] si no existía o lo elimina si ya estaba guardado.
     * Devuelve `true` cuando queda guardado y `false` cuando se eliminó.
     */
    suspend fun toggleFavorito(favorito: Favorito): Boolean {
        val existing = favoritoDao.getFavoritoById(favorito.id)
        return if (existing != null) {
            favoritoDao.deleteFavoritoById(favorito.id)
            false
        } else {
            favoritoDao.insertFavorito(favorito)
            true
        }
    }
}
