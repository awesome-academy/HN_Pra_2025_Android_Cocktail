package com.example.cocktaildb.screen.checkmark

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class CheckmarkPresenterTest {

    private lateinit var context: Context
    private lateinit var cocktailRepository: CocktailRepository
    private lateinit var view: CheckmarkContract.View
    private lateinit var presenter: CheckmarkPresenter

    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockConnectivityManager: ConnectivityManager
    private lateinit var mockNetworkInfo: NetworkInfo

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock context and system services
        context = mockk(relaxed = true)
        mockSharedPreferences = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        mockConnectivityManager = mockk(relaxed = true)
        mockNetworkInfo = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager

        // Setup network availability using getActiveNetworkInfo
        every { mockConnectivityManager.activeNetworkInfo } returns mockNetworkInfo
        every { mockNetworkInfo.isConnected } returns true

        // Initialize Firebase App manually for testing
        try {
            val options = FirebaseOptions.Builder()
                .setApplicationId("test-app-id")
                .setApiKey("test-api-key")
                .setProjectId("test-project-id")
                .build()
            FirebaseApp.initializeApp(context, options)
        } catch (e: Exception) {
            // Firebase app might already be initialized
        }

        // Mock Firebase Auth
        mockAuth = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockAuth
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "test-user-id"

        // Mock repository and view
        cocktailRepository = mockk(relaxed = true)
        view = mockk(relaxed = true)

        // Create presenter
        presenter = CheckmarkPresenter(context, cocktailRepository)
        presenter.setView(view)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        presenter.onStop()
        unmockkAll()
        try {
            FirebaseApp.getInstance().delete()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    private fun dummyCocktail(id: String = "1", name: String = "Drink $id"): Cocktail =
        Cocktail(
            idDrink = id,
            strDrink = name,
            strCategory = "Category",
            strAlcoholic = "Alcoholic",
            strGlass = "Glass",
            strInstructions = "Instr",
            strDrinkThumb = null,
            ingredients = emptyList(),
            measures = emptyList()
        )

    @Test
    fun loadCheckmarks_offline_loadsLocalList() = testScope.runTest {
        // Given
        val localList = listOf(dummyCocktail("10", "Local A"), dummyCocktail("11", "Local B"))
        every { mockAuth.currentUser } returns null
        every { cocktailRepository.getCheckmarksFromLocal(context) } returns localList

        // When
        presenter.loadCheckmarks()
        advanceUntilIdle() // Ensure coroutines complete

        // Then
        verify { view.displayLoading(true) }
        verify { view.displayCheckmarks(localList) }
        verify { view.displayLoading(false) }
        verify(exactly = 0) { view.displayEmptyState() }
    }

    @Test
    fun loadCheckmarks_noNetwork_loadsLocalList() = testScope.runTest {
        // Given
        val localList = listOf(dummyCocktail("20", "Local C"))
        every { mockConnectivityManager.activeNetworkInfo } returns null
        every { cocktailRepository.getCheckmarksFromLocal(context) } returns localList

        // When
        presenter.loadCheckmarks()
        advanceUntilIdle()

        // Then
        verify { view.displayLoading(true) }
        verify { view.displayCheckmarks(localList) }
        verify { view.displayLoading(false) }
        verify(exactly = 0) { view.displayEmptyState() }
    }

    @Test
    fun addToCheckmarks_noUser_addsToLocalOnly() = testScope.runTest {
        // Given
        val cocktail = dummyCocktail("30", "Test Cocktail")
        val currentCheckmarks = emptyList<Cocktail>()
        every { mockAuth.currentUser } returns null
        every { cocktailRepository.getCheckmarksFromLocal(context) } returns currentCheckmarks
        every { cocktailRepository.saveCheckmarksToLocal(context, any()) } just Runs

        // When
        presenter.addToCheckmarks(cocktail)
        advanceUntilIdle()

        // Then
        verify { cocktailRepository.saveCheckmarksToLocal(context, listOf(cocktail)) }
        verify(exactly = 0) { view.showCheckmarkAdded(cocktail) }
    }


    @Test
    fun removeFromCheckmarks_noUser_removesFromLocalOnly() = testScope.runTest {
        // Given
        val cocktail = dummyCocktail("40", "Test Remove")
        val currentCheckmarks = listOf(cocktail)
        every { mockAuth.currentUser } returns null
        every { cocktailRepository.getCheckmarksFromLocal(context) } returns currentCheckmarks
        every { cocktailRepository.saveCheckmarksToLocal(context, any()) } just Runs

        // When
        presenter.removeFromCheckmarks(cocktail)
        advanceUntilIdle()

        // Then
        verify { cocktailRepository.saveCheckmarksToLocal(context, emptyList()) }
        verify { view.showCheckmarkRemoved(cocktail) }
        verify { view.displayEmptyState() }
    }

    @Test
    fun clearAllCheckmarks_clearsLocalStorage() = testScope.runTest {
        // Given
        every { mockAuth.currentUser } returns null
        every { cocktailRepository.clearAllCheckmarksFromLocal(context) } just Runs

        // When
        presenter.clearAllCheckmarks()
        advanceUntilIdle()

        // Then
        verify { cocktailRepository.clearAllCheckmarksFromLocal(context) }
        verify { view.displayEmptyState() }
        verify { view.showSyncStatus("Local checkmarks cleared") }
    }

    @Test
    fun toggleCheckmark_existingCocktail_removesIt() = testScope.runTest {
        // Given
        val cocktail = dummyCocktail("50", "Toggle Test")
        val currentCheckmarks = listOf(cocktail)
        every { mockAuth.currentUser } returns null
        every { cocktailRepository.getCheckmarksFromLocal(context) } returns currentCheckmarks
        every { cocktailRepository.saveCheckmarksToLocal(context, any()) } just Runs

        // Load checkmarks first to populate cache
        presenter.loadCheckmarks()
        advanceUntilIdle()

        // When
        presenter.toggleCheckmark(cocktail)
        advanceUntilIdle()

        // Then
        verify { cocktailRepository.saveCheckmarksToLocal(context, emptyList()) }
        verify { view.showCheckmarkRemoved(cocktail) }
        verify { view.displayEmptyState() }
    }

    @Test
    fun presenter_implements_correct_interface() {
        // Then
        assert(presenter is CheckmarkContract.Presenter)
    }

    @Test
    fun view_interface_has_required_methods() {
        // Then
        val view: CheckmarkContract.View = object : CheckmarkContract.View {
            override fun displayLoading(show: Boolean) {}
            override fun displayCheckmarks(checkmarks: List<Cocktail>) {}
            override fun displayEmptyState() {}
            override fun displayError(message: String) {}
            override fun showCheckmarkAdded(cocktail: Cocktail) {}
            override fun showCheckmarkRemoved(cocktail: Cocktail) {}
            override fun showSyncStatus(message: String) {}
        }

        assert(view is CheckmarkContract.View)
    }

    @Test
    fun syncCheckmarksIfNeeded_noUser_showsOfflineStatus() = testScope.runTest {
        // Given
        every { mockAuth.currentUser } returns null

        // When
        presenter.syncCheckmarksIfNeeded()
        advanceUntilIdle()

        // Then
        verify { view.showSyncStatus("Offline mode - showing local checkmarks") }
    }
}
