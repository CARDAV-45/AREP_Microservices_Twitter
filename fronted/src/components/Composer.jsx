import { useState } from 'react'
import { useAuth0 } from '@auth0/auth0-react'

const API = import.meta.env.VITE_API_URL || ''
const MAX = 140

export default function Composer({ onPostCreated }) {
  const { isAuthenticated, user, loginWithRedirect, getAccessTokenSilently } = useAuth0()
  const [text,    setText]    = useState('')
  const [loading, setLoading] = useState(false)
  const [error,   setError]   = useState('')

  const remaining = MAX - text.length
  const overLimit = remaining < 0

  async function handleSubmit(e) {
    e.preventDefault()
    if (!text.trim() || overLimit) return
    setLoading(true)
    setError('')
    try {
      const token = await getAccessTokenSilently()
      const res = await fetch(`${API}/api/posts`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ content: text.trim() })
      })
      if (!res.ok) throw new Error(`Error ${res.status}`)
      setText('')
      onPostCreated?.()
    } catch (err) {
      setError('No se pudo publicar. Intenta de nuevo.')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  if (!isAuthenticated) {
    return (
      <div className="login-banner">
        <p>Inicia sesión para publicar.</p>
        <button className="btn btn-primary" onClick={() => loginWithRedirect()}>
          Iniciar sesión
        </button>
      </div>
    )
  }

  return (
    <form className="composer" onSubmit={handleSubmit}>
      {user.picture
        ? <img src={user.picture} alt="avatar" className="avatar" />
        : <div className="avatar-placeholder">{user.name?.[0]?.toUpperCase()}</div>
      }
      <div className="composer-body">
        <textarea
          placeholder="¿Qué está pasando?"
          value={text}
          onChange={e => setText(e.target.value)}
          maxLength={MAX + 10}
          disabled={loading}
        />
        <hr className="composer-divider" />
        {error && <p className="error-msg">{error}</p>}
        <div className="composer-footer">
          <span className={`char-count ${remaining <= 20 && remaining >= 0 ? 'warn' : ''} ${overLimit ? 'over' : ''}`}>
            {remaining}
          </span>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={!text.trim() || overLimit || loading}
          >
            {loading ? 'Publicando...' : 'Publicar'}
          </button>
        </div>
      </div>
    </form>
  )
}
