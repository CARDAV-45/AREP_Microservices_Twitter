import { useEffect, useState, useCallback } from 'react'

const API = import.meta.env.VITE_API_URL || ''

function timeAgo(dateStr) {
  const diff = (Date.now() - new Date(dateStr)) / 1000
  if (diff < 60)   return `${Math.floor(diff)}s`
  if (diff < 3600) return `${Math.floor(diff / 60)}m`
  if (diff < 86400)return `${Math.floor(diff / 3600)}h`
  return new Date(dateStr).toLocaleDateString('es-CO', { day: 'numeric', month: 'short' })
}

export default function Feed({ refreshSignal }) {
  const [posts,   setPosts]   = useState([])
  const [loading, setLoading] = useState(true)
  const [error,   setError]   = useState('')

  const fetchPosts = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await fetch(`${API}/api/posts`)
      if (!res.ok) throw new Error(`Error ${res.status}`)
      setPosts(await res.json())
    } catch {
      setError('No se pudo cargar el stream.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchPosts() }, [fetchPosts, refreshSignal])

  if (loading) return <div className="loading">Cargando stream...</div>
  if (error)   return <div className="loading">{error}</div>

  return (
    <section>
      <div className="feed-header">Stream público</div>
      {posts.length === 0
        ? <div className="empty-feed">Aún no hay posts. ¡Sé el primero!</div>
        : posts.map(post => (
          <article className="post-card" key={post.id}>
            {post.authorPicture
              ? <img src={post.authorPicture} alt="avatar" className="avatar" />
              : <div className="avatar-placeholder">{post.authorName?.[0]?.toUpperCase()}</div>
            }
            <div className="post-body">
              <span className="post-author">{post.authorName}</span>
              <span className="post-time">{timeAgo(post.createdAt)}</span>
              <p className="post-content">{post.content}</p>
            </div>
          </article>
        ))
      }
    </section>
  )
}
