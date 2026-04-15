import { useAuth0 } from '@auth0/auth0-react'

export default function Navbar() {
  const { isAuthenticated, user, loginWithRedirect, logout } = useAuth0()

  return (
    <nav className="navbar">
      <span className="navbar-brand">Twitter AREP</span>

      <div className="navbar-right">
        {isAuthenticated ? (
          <>
            {user.picture
              ? <img src={user.picture} alt="avatar" className="avatar" />
              : <div className="avatar-placeholder">{user.name?.[0]?.toUpperCase()}</div>
            }
            <span className="username">{user.name}</span>
            <button
              className="btn btn-danger"
              onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}
            >
              Salir
            </button>
          </>
        ) : (
          <button className="btn btn-primary" onClick={() => loginWithRedirect()}>
            Iniciar sesión
          </button>
        )}
      </div>
    </nav>
  )
}
