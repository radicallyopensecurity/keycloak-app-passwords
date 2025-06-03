import { User } from '../types'

export const deleteAllTokens = (user: User) => {
  return cy
    .request({
      method: 'POST',
      url: 'http://localhost:8080/realms/myrealm/protocol/openid-connect/token',
      form: true,
      body: {
        client_id: 'admin-cli',
        username: user.username,
        password: user.password,
        grant_type: 'password',
      },
    })
    .then((response) => {
      const token = response.body.access_token

      // Delete emailPassword
      cy.request({
        method: 'DELETE',
        url: 'http://localhost:8080/realms/myrealm/app-password',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: { name: 'emailPassword' },
        failOnStatusCode: false, // in case it doesn't exist
      })

      // Delete smsPassword
      cy.request({
        method: 'DELETE',
        url: 'http://localhost:8080/realms/myrealm/app-password',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: { name: 'smsPassword' },
        failOnStatusCode: false, // in case it doesn't exist
      })
    })
}
