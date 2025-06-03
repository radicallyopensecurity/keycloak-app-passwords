import { defineConfig } from 'cypress'

export default defineConfig({
  port: 8888,
  e2e: {
    specPattern: 'tests/**/*.cy.ts',
    supportFile: false,
    fixturesFolder: 'fixtures',
    screenshotsFolder: 'screenshots',
  },
})
