import { defineConfig } from 'cypress'

export default defineConfig({
  port: 8888,
  defaultBrowser: 'chrome',
  e2e: {
    specPattern: 'tests/**/*.cy.ts',
    supportFile: false,
    fixturesFolder: 'fixtures',
    screenshotsFolder: 'screenshots',
  },
})
