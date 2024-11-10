import { CognitoUserPool, CognitoUser, AuthenticationDetails } from 'amazon-cognito-identity-js';

const poolData = {
  UserPoolId: import.meta.env.VITE_COGNITO_USER_POOL_ID,
  ClientId: import.meta.env.VITE_COGNITO_CLIENT_ID
};

const userPool = new CognitoUserPool(poolData);

export const auth = {
  signUp: (username, email, password) => {
    return new Promise((resolve, reject) => {
      userPool.signUp(username, password, [{ Name: 'email', Value: email }], null, (err, result) => {
        if (err) {
          reject(err);
        } else {
          resolve({ user: result.user, userConfirmed: result.userConfirmed });
        }
      });
    });
  },

  signIn: (username, password) => {
    return new Promise((resolve, reject) => {
      const authenticationDetails = new AuthenticationDetails({
        Username: username,
        Password: password,
      });

      const cognitoUser = new CognitoUser({
        Username: username,
        Pool: userPool
      });

      cognitoUser.authenticateUser(authenticationDetails, {
        onSuccess: (result) => {
          const userAttributes = result.getIdToken().payload;
          const token = result.getIdToken().getJwtToken();
          setSession({ idToken: token });
          resolve({
            username: userAttributes['cognito:username'],
            email: userAttributes.email,
            token,
          });
        },
        onFailure: (err) => {
          reject(err);
        },
      });
    });
  },

  signOut: () => {
    const cognitoUser = userPool.getCurrentUser();
    if (cognitoUser) {
      cognitoUser.signOut();
    }
  },

  getCurrentUser: () => {
    return new Promise((resolve, reject) => {
      const cognitoUser = userPool.getCurrentUser();
      if (!cognitoUser) {
        resolve(null);
      }

      cognitoUser.getSession((err, session) => {
        if (err) {
          reject(err);
        } else {
          cognitoUser.getUserAttributes((err, attributes) => {
            if (err) {
              reject(err);
            } else {
              const userData = attributes.reduce((acc, attribute) => {
                acc[attribute.Name] = attribute.Value;
                return acc;
              }, {});
              resolve({
                username: userData['cognito:username'] || userData.sub,
                email: userData.email,
                // Add any other attributes you want to include
              });
            }
          });
        }
      });
    });
  },

  confirmSignUp: (username, code) => {
    return new Promise((resolve, reject) => {
      const cognitoUser = new CognitoUser({
        Username: username,
        Pool: userPool
      });

      cognitoUser.confirmRegistration(code, true, (err, result) => {
        if (err) {
          reject(err);
        } else {
          resolve(result);
        }
      });
    });
  },

  setSession: (session) => {
    localStorage.setItem('userSession', JSON.stringify(session));
  },

  getSession: () => {
    const session = localStorage.getItem('userSession');
    return session ? JSON.parse(session) : null;
  },

  clearSession: () => {
    localStorage.removeItem('userSession');
  },
};
