# Fetch Rewards Coding Exercise

[Screen recording](docs/screen-20241002-160728.mp4)

The project uses the following technologies:

- Jetpack Compose
- Hilt
- Retrofit
- Coroutines
- [Slack Circuit](https://slackhq.github.io/circuit/) for presentation and navigation management
- For unit tests: JUnit, Mockk, and KoTest

Some potential improvements:

- Add more tests (including end-to-end tests with mocked network)
- Use a more user-friendly UI
- Add proper UI for tablets or other bigger screens
- Add offline support
- Better display errors
- Add proper handling if the server is down (5** errors)
- Don't expose network entities outside the network layer
- Use proper logging that can be disabled in production
