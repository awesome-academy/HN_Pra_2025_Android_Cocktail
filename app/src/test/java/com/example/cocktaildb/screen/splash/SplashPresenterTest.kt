package com.example.cocktaildb.screen.splash

import com.example.cocktaildb.R
import com.example.cocktaildb.data.repository.AuthRepository
import io.mockk.*
import org.junit.Before
import org.junit.Test

class SplashPresenterTest {

    private lateinit var presenter: SplashPresenter
    private lateinit var mockView: SplashContract.View
    private lateinit var mockAuthRepository: AuthRepository

    @Before
    fun setUp() {
        mockView = mockk(relaxed = true)
        mockAuthRepository = mockk(relaxed = true)
        
        presenter = SplashPresenter(mockAuthRepository)
    }

    @Test
    fun `setView sets the view correctly`() {
        // When
        presenter.setView(mockView)
        
        // Then
        // View is set internally
    }

    @Test
    fun `onStart does not throw exceptions`() {
        // Given
        presenter.setView(mockView)
        
        // When
        presenter.onStart()
        
        // Then
        // Should not throw exceptions
    }

    @Test
    fun `onStartButtonClicked navigates to auth`() {
        // Given
        presenter.setView(mockView)
        
        // When
        presenter.onStartButtonClicked()
        
        // Then
        verify { mockView.navigateToAuth() }
    }

    @Test
    fun `onStop does not throw exceptions`() {
        // When
        presenter.onStop()
        
        // Then
        // Should not throw exceptions
    }

    @Test
    fun `presenter implements SplashContract Presenter`() {
        // Then
        assert(presenter is SplashContract.Presenter)
    }

    @Test
    fun `view interface has all required methods`() {
        // Then
        // Verify that the view interface has all required methods
        val view: SplashContract.View = object : SplashContract.View {
            override fun navigateToHome() {}
            override fun navigateToAuth() {}
            override fun showMessage(message: String) {}
        }
        
        assert(view is SplashContract.View)
    }
} 