package com.example.cocktaildb.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.service.CheckmarkFirebaseService
import com.example.cocktaildb.data.service.FavoriteFirebaseService
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CocktailRepositoryLocalTest {

    private lateinit var context: Context
    private lateinit var repo: CocktailRepository

    private val fakeFavoriteService: FavoriteFirebaseService = mock(FavoriteFirebaseService::class.java)
    private val fakeCheckmarkService: CheckmarkFirebaseService = mock(CheckmarkFirebaseService::class.java)

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        
        // Mock SharedPreferences
        val mockPrefs = mockk<SharedPreferences>(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.getString(any(), any()) } returns ""
        every { mockPrefs.edit() } returns mockk(relaxed = true)
        
        repo = CocktailRepository(
            dataSource = com.example.cocktaildb.data.repository.source.remote.CocktailRemoteDataSource(),
            favoriteFirebaseService = fakeFavoriteService,
            checkmarkFirebaseService = fakeCheckmarkService
        )
        repo.clearAllLocalData(context)
    }

    @Test
    fun favorites_encodeDecode_roundTrip() {
        val items = listOf(
            Cocktail(idDrink = "1", strDrink = "A", strDrinkThumb = "http://img/a.png", strCategory = "CatA", strAlcoholic = "Alcoholic", strGlass = "Highball", strInstructions = "Mix", ingredients = listOf("Gin"), dateModified = "2024-01-01"),
            Cocktail(idDrink = "2", strDrink = "B", strDrinkThumb = null, strCategory = null, strAlcoholic = null, strGlass = null, strInstructions = null, ingredients = emptyList(), dateModified = null)
        )

        repo.saveFavoritesToLocal(context, items)
        val decoded = repo.getFavoritesFromLocal(context)

        assertEquals(2, decoded.size)
        assertEquals("1", decoded[0].idDrink)
        assertEquals("A", decoded[0].strDrink)
        assertEquals("http://img/a.png", decoded[0].strDrinkThumb)
        assertEquals("CatA", decoded[0].strCategory)
        assertEquals("Alcoholic", decoded[0].strAlcoholic)
        assertEquals("Highball", decoded[0].strGlass)
        assertEquals("Mix", decoded[0].strInstructions)
        assertEquals(listOf("Gin"), decoded[0].ingredients)
        assertEquals("2024-01-01", decoded[0].dateModified)

        assertEquals("2", decoded[1].idDrink)
        assertEquals("B", decoded[1].strDrink)
        assertEquals(null, decoded[1].strDrinkThumb)
        assertEquals(null, decoded[1].strCategory)
        assertEquals(null, decoded[1].strAlcoholic)
        assertEquals(null, decoded[1].strGlass)
        assertEquals(null, decoded[1].strInstructions)
        assertEquals(emptyList<String>(), decoded[1].ingredients)
        assertEquals(null, decoded[1].dateModified)
    }

    @Test
    fun history_encodeDecode_roundTrip() {
        // Mock SharedPreferences for this specific test
        val mockPrefs = mockk<SharedPreferences>(relaxed = true)
        val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
        
        every { context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE) } returns mockPrefs
        every { mockPrefs.getString("cocktail_history", "") } returns "10|:|NameX|:|http://img/x.jpg"
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
        
        val list = repo.getHistoryCocktails(context)
        assertEquals(1, list.size)
        assertEquals("10", list[0].idDrink)
        assertEquals("NameX", list[0].strDrink)
        assertEquals("http://img/x.jpg", list[0].strDrinkThumb)

        repo.clearHistory(context)
        assertTrue(repo.getHistoryCocktails(context).isEmpty())
    }
}
