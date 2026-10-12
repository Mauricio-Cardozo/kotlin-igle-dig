package com.example.data.user

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritoDao {
    @Query("SELECT * FROM favoritos ORDER BY timestamp DESC")
    fun getAllFavoritos(): Flow<List<Favorito>>

    @Query("SELECT * FROM favoritos WHERE tipo = :tipo ORDER BY timestamp DESC")
    fun getFavoritosByTipo(tipo: String): Flow<List<Favorito>>

    @Query("SELECT * FROM favoritos WHERE id = :id")
    fun getFavoritoByIdFlow(id: String): Flow<Favorito?>

    @Query("SELECT * FROM favoritos WHERE id = :id")
    suspend fun getFavoritoById(id: String): Favorito?

    @Query("SELECT * FROM favoritos WHERE id IN (:ids)")
    fun getFavoritosByIds(ids: List<String>): Flow<List<Favorito>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorito(favorito: Favorito)

    @Delete
    suspend fun deleteFavorito(favorito: Favorito)

    @Query("DELETE FROM favoritos WHERE id = :id")
    suspend fun deleteFavoritoById(id: String)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY label ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)
}
