package com.example

import com.example.data.AuthUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun authStateUsesEmailNameAndInitials() {
    val state = AuthUiState(email = "cook@example.com")

    assertEquals("cook", state.displayLabel)
    assertEquals("C", state.initials)
  }
}
