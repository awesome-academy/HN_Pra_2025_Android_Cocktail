package com.example.cocktaildb.screen.profile

import android.content.Context
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ProfilePresenterTest {

    private lateinit var context: Context
    private lateinit var closeable: AutoCloseable

    @Mock lateinit var cocktailRepository: CocktailRepository
    @Mock lateinit var authRepository: AuthRepository
    @Mock lateinit var view: ProfileContract.View

    private lateinit var presenter: ProfilePresenter

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        context = mockk<Context>(relaxed = true)
        
        // Mock SharedPreferences
        val mockPrefs = mockk<SharedPreferences>(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.getString(any(), any()) } returns ""
        every { mockPrefs.edit() } returns mockk(relaxed = true)
        
        presenter = ProfilePresenter(context, cocktailRepository, authRepository)
        presenter.setView(view)
    }

    @After
    fun tearDown() {
        closeable.close()
    }

    // 1) loadUserProfile
    @Test
    fun loadUserProfile_showsGuest_whenNoUserLoggedIn() {
        `when`(authRepository.getCurrentUser()).thenReturn(null)


        presenter.setView(view = view)


        verify(view).displayLoading(false)
        verify(view).showUserProfile(
            userName = "Guest User",
            userBio = "Please sign in to see your profile",
            profileImageUrl = null
        )

    }

    // 2) onMyRecipesClicked
    @Test
    fun onMyRecipesClicked_navigates() {

        presenter.setView(view)
        presenter.onMyRecipesClicked()

        verify(view).navigateToMyRecipes()
    }

    // 3) onHistoryClicked
    @Test
    fun onHistoryClicked_navigates() {
        presenter.setView(view)
        presenter.onHistoryClicked()

        verify(view).navigateToHistory()
    }

    // 4) onCheckmarkClicked
    @Test
    fun onCheckmarkClicked_navigates() {
        presenter.setView(view)
        presenter.onCheckmarkClicked()

        verify(view).navigateToCheckmarks()
    }

    // 5) onLogoutClicked: signOut + clearAllLocalData + navigate
    @Test
    fun onLogoutClicked_signsOut_clearsData_and_navigates() {
        presenter.setView(view)
        presenter.onLogoutClicked()

        verify(authRepository).signOut()
        verify(cocktailRepository).clearAllLocalData(context)
        verify(view).navigateToLogin()
    }
}
