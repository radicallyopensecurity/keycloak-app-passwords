import { deleteAllTokens } from '../support/utils'
import { User } from '../types'

const HIDDEN_PASSWORD = '●'.repeat(32)

before(() => {
  cy.fixture('user.json').then((user: User) => {
    deleteAllTokens(user)
  })
})

describe('keycloak-app-passwords', () => {
  it('e2e', () => {
    cy.intercept('POST', '**/realms/myrealm/app-password').as('regenerate')
    cy.intercept('DELETE', '**/realms/myrealm/app-password').as('delete')

    cy.log('It can access the app passwords console')
    cy.viewport(1920, 1080)
    cy.visit('http://localhost:8080/realms/myrealm/account')

    cy.fixture('user.json').then((user: User) => {
      cy.get('#username').type(user.username)
      cy.get('#password').type(user.password)
      cy.get('#kc-login').click()
    })

    cy.get('[data-testid=app-passwords]').click()

    cy.log('It can generate a password for the first time')
    cy.get('[data-testrole=regenerate]').first().click()
    cy.wait('@regenerate')

    cy.log('It returns a hidden password')
    cy.get('[data-testid=app-passwords-value]').should(
      'contain.text',
      HIDDEN_PASSWORD,
    )

    cy.log('It can reveal the password')
    cy.get('[data-testid=app-passwords-reveal]').first().click()

    cy.get('[data-testid=app-passwords-value]')
      .invoke('text')
      .then((password) => {
        expect(password).to.have.length(32)
        expect(password).to.not.eq(HIDDEN_PASSWORD)

        cy.wrap(password).as('password1')

        cy.window().then((win) => {
          cy.stub(win.navigator.clipboard, 'writeText').as('clipboardWrite')
        })

        cy.log('It can copy the password')
        cy.get('[data-testid=app-passwords-copy]').first().click()

        cy.get('@clipboardWrite').should('have.been.calledOnceWith', password)
      })

    cy.log('It can hide the password again')
    cy.get('[data-testid=app-passwords-reveal]').first().click()

    cy.get('[data-testid=app-passwords-value]').should(
      'contain.text',
      HIDDEN_PASSWORD,
    )

    cy.log('It can regenerate')
    cy.get('[data-testrole=regenerate]').first().click()

    cy.get('[data-testid=confirm]')
      .should('be.visible')
      .and('be.enabled')
      .click()

    cy.wait('@regenerate')

    cy.get('[data-testid=app-passwords-reveal]').first().click()

    cy.get('@password1').then((password1) => {
      cy.get('[data-testid=app-passwords-value]')
        .invoke('text')
        .then((password) => {
          expect(password).to.have.length(32)
          expect(password).to.not.eq(password1)
          expect(password).to.not.eq(HIDDEN_PASSWORD)
        })
    })

    cy.log('It can generate a second password')
    cy.get('[data-testrole=regenerate]').eq(1).click()
    cy.wait('@regenerate')

    cy.log('It only shows one password at a time')

    cy.get('[data-testrole=regenerate]').first().click()

    cy.get('[data-testid=confirm]')
      .should('be.visible')
      .and('be.enabled')
      .click()

    cy.wait('@regenerate')

    cy.get('.pf-v5-c-alert__title').should('contain.text', 'Email Password')

    cy.get('[data-testid=app-passwords-value]').should(
      'contain.text',
      HIDDEN_PASSWORD,
    )

    cy.get('[data-testrole=regenerate]').eq(1).click()

    cy.get('[data-testid=confirm]')
      .should('be.visible')
      .and('be.enabled')
      .click()

    cy.wait('@regenerate')

    cy.get('.pf-v5-c-alert__title').should('contain.text', 'SmsPassword')

    cy.get('[data-testid=app-passwords-value]').should(
      'contain.text',
      HIDDEN_PASSWORD,
    )
  })
})
