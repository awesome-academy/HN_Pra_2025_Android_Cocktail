package com.example.cocktaildb.screen.auth

import com.example.cocktaildb.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuthPresenterTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK(relaxed = true)
    lateinit var authRepository: AuthRepository

    @MockK(relaxed = true)
    lateinit var view: AuthContract.View

    private lateinit var presenter: AuthPresenter

    @Before
    fun setUp() {
        clearMocks(authRepository, view)
        presenter = AuthPresenter(authRepository)
        presenter.setView(view)
    }

    // validateLoginInput
    @Test
    fun `validateLoginInput returns false and shows message when email empty`() {
        val result = presenter.validateLoginInput(email = "", password = "123")
        assertFalse(result)
        verify { view.showMessage(any()) }
        verify { authRepository wasNot Called }
    }

    @Test
    fun `validateLoginInput returns false and shows message when password empty`() {
        val result = presenter.validateLoginInput(email = "a@b.com", password = "")
        assertFalse(result)
        verify { view.showMessage(any()) }
        verify { authRepository wasNot Called }
    }

    @Test
    fun `validateLoginInput returns true when both fields provided`() {
        val result = presenter.validateLoginInput(email = "a@b.com", password = "123")
        assertTrue(result)
        verify(exactly = 0) { view.showMessage(any()) }
    }

    // loginWithEmail
    @Test
    fun `loginWithEmail success calls onLoginSuccess and hides loading`() {
        val testEmail = "user@gmail.com"
        val testPassword = "password123"
        val mockUser: FirebaseUser = mockk(relaxed = true)

        every { authRepository.loginWithEmail(any(), any(), any()) } answers {
            lastArg<(FirebaseUser?, String?) -> Unit>().invoke(mockUser, null)
        }

        presenter.loginWithEmail(testEmail, testPassword)

        verifyOrder {
            view.showLoading()
            view.hideLoading()
            view.onLoginSuccess()
        }
        verify(exactly = 0) { view.onLoginFailure(any()) }
    }

    @Test
    fun `loginWithEmail failure calls onLoginFailure with error and hides loading`() {
        val errorMsg = "Invalid credentials"
        every { authRepository.loginWithEmail(any(), any(), any()) } answers {
            lastArg<(FirebaseUser?, String?) -> Unit>().invoke(null, errorMsg)
        }

        presenter.loginWithEmail("user@gmail.com", "wrong")

        verifyOrder {
            view.showLoading()
            view.hideLoading()
            view.onLoginFailure(errorMsg)
        }
        verify(exactly = 0) { view.onLoginSuccess() }
    }

    // validateSignUpInput
    @Test
    fun `validateSignUpInput returns false and shows message when password mismatch`() {
        val result = presenter.validateSignUpInput(
            name = "abc",
            email = "abc@gmail.com",
            password = "123456",
            confirmPassword = "12345",
            termsAccepted = true
        )
        assertFalse(result)
        verify { view.showMessage(any()) }
        verify { authRepository wasNot Called }
    }

    @Test
    fun `validateSignUpInput returns false and shows message when terms not accepted`() {
        val result = presenter.validateSignUpInput(
            name = "abc",
            email = "abc@gmail.com",
            password = "123456",
            confirmPassword = "123456",
            termsAccepted = false
        )
        assertFalse(result)
        verify { view.showMessage(any()) }
        verify { authRepository wasNot Called }
    }

    // signUpWithEmail
    @Test
    fun `signUpWithEmail success calls onLoginSuccess and hides loading`() {
        val mockUser: FirebaseUser = mockk(relaxed = true)
        every { authRepository.signUpWithEmail(any(), any(), any(), any()) } answers {
            lastArg<(FirebaseUser?, String?) -> Unit>().invoke(mockUser, null)
        }

        presenter.signUpWithEmail(
            name = "abc",
            email = "abc@gmail.com",
            password = "12345@",
            confirmPassword = "12345@",
            termsAccepted = true
        )

        verifyOrder {
            view.showLoading()
            view.hideLoading()
            view.onLoginSuccess()
        }
        verify(exactly = 0) { view.onLoginFailure(any()) }
    }

    @Test
    fun `signUpWithEmail failure calls onLoginFailure and hides loading`() {
        val errorMsg = "Sign up failed"
        every { authRepository.signUpWithEmail(any(), any(), any(), any()) } answers {
            lastArg<(FirebaseUser?, String?) -> Unit>().invoke(null, errorMsg)
        }

        presenter.signUpWithEmail(
            name = "abc",
            email = "abc@gmail.com",
            password = "123456",
            confirmPassword = "123456",
            termsAccepted = true
        )

        verifyOrder {
            view.showLoading()
            view.hideLoading()
            view.onLoginFailure(errorMsg)
        }
        verify(exactly = 0) { view.onLoginSuccess() }
    }

    // forgotPassword
    @Test
    fun `forgotPassword success shows success message`() {
        every { authRepository.sendPasswordResetEmail(any(), any()) } answers {
            lastArg<(Boolean, String?) -> Unit>().invoke(true, null)
        }

        presenter.forgotPassword("user@gmail.com")

        verifyOrder {
            view.showLoading()
            view.hideLoading()
            view.showMessage(any())
        }
    }

    @Test
    fun `forgotPassword failure shows error message`() {
        every { authRepository.sendPasswordResetEmail(any(), any()) } answers {
            lastArg<(Boolean, String?) -> Unit>().invoke(false, "error")
        }

        presenter.forgotPassword("user@gmail.com")

        verifyOrder {
            view.showLoading()
            view.hideLoading()
            view.showMessage(any())
        }
    }

    // Early return guards
    @Test
    fun `loginWithEmail with invalid input does not call repository or show loading`() {
        presenter.loginWithEmail(email = "", password = "")
        verify(exactly = 0) { view.showLoading() }
        verify { authRepository wasNot Called }
    }

    @Test
    fun `signUpWithEmail with password mismatch does not call repository or show loading`() {
        presenter.signUpWithEmail(
            name = "abc",
            email = "abc@gmail.com",
            password = "123456",
            confirmPassword = "654321",
            termsAccepted = true
        )
        verify(exactly = 0) { view.showLoading() }
        verify { authRepository wasNot Called }
    }
}
