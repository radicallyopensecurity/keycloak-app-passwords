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
    cy.intercept(
      'POST',
      'http://localhost:8080/realms/myrealm/app-password',
    ).as('regenerate')

    cy.intercept(
      'DELETE',
      'http://localhost:8080/realms/myrealm/app-password',
    ).as('delete')

    cy.log('It can access the app passwords console')
    cy.viewport(1920, 1080)
    cy.visit('http://localhost:8080/realms/myrealm/account')

    cy.fixture('user.json').then((user: User) => {
      cy.get('#username').type(user.username)
      cy.get('#password').type(user.password)
      cy.get('#kc-login').click()
    })

    cy.get('[data-testid=app-passwords]').click()

    cy.log('It can generates password for the first time')
    cy.get('[data-testrole=regenerate]').first().click()
    cy.wait('@regenerate')

    cy.log('It returns a hidden password')
    cy.get('[data-testid=app-passwords-value]').contains(HIDDEN_PASSWORD)

    cy.log('It can reveal the password')
    let password1 = ''
    cy.get('[data-testid=app-passwords-reveal').first().click()
    cy.get('[data-testid=app-passwords-value]')
      .invoke('text')
      .then((password) => {
        expect(password.length).to.eq(32)
        expect(password).to.not.eq(HIDDEN_PASSWORD)

        password1 = password

        cy.log('It can copy the password')
        cy.get('[data-testid=app-passwords-copy').first().click()

        return cy.window().then((win) => {
          return win.navigator.clipboard.readText().then((clipboard) => {
            expect(clipboard).to.eq(password)
          })
        })
      })

    cy.log('It can hide the password again')
    cy.get('[data-testid=app-passwords-reveal').first().click()
    cy.get('[data-testid=app-passwords-value]').contains(HIDDEN_PASSWORD)

    cy.log('It can regenerate')
    cy.get('[data-testrole=regenerate]').first().click()
    cy.get('[data-testid=confirm]').click()
    cy.wait('@regenerate')
    cy.get('[data-testid=app-passwords-reveal').first().click()
    cy.get('[data-testid=app-passwords-value]')
      .invoke('text')
      .then((password) => {
        expect(password.length).to.eq(32)
        expect(password).to.not.eq(password1)
        expect(password).to.not.eq(HIDDEN_PASSWORD)
      })

    cy.log('It can delete')
    cy.get('[data-testrole=delete]').first().click()
    cy.get('[data-testid=confirm]').click()
    cy.wait('@delete')
    cy.get('[data-testid=app-passwords-value]').should('not.exist')

    cy.log('It can generate 2nd')
    cy.get('[data-testrole=regenerate]').eq(1).click()
    cy.wait('@regenerate')

    cy.log('It can delete 2nd')
    cy.get('[data-testrole=delete]').eq(1).click()
    cy.get('[data-testid=confirm]').click()
    cy.wait('@delete')
    cy.get('[data-testid=app-passwords-value]').should('not.exist')

    cy.log('It only shows 1 password at a time')
    cy.get('[data-testrole=regenerate]').first().click()
    cy.wait('@regenerate')
    cy.get('.pf-v5-c-alert__title')
      .invoke('text')
      .then((title) => {
        title.includes('Email Password')
      })
    cy.get('[data-testid=app-passwords-value]').contains(HIDDEN_PASSWORD)
    cy.get('[data-testrole=regenerate]').eq(1).click()
    cy.wait('@regenerate')
    cy.get('.pf-v5-c-alert__title')
      .invoke('text')
      .then((title) => {
        title.includes('smsPassword')
      })
    cy.get('[data-testid=app-passwords-value]').contains(HIDDEN_PASSWORD)
  })
})
