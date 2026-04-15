import { useState } from 'react'
import { useAuth0 } from '@auth0/auth0-react'
import Navbar   from './components/Navbar'
import Feed     from './components/Feed'
import Composer from './components/Composer'

export default function App() {
  const { isLoading } = useAuth0()
  const [refreshSignal, setRefreshSignal] = useState(0)

  if (isLoading) {
    return <div className="loading" style={{ paddingTop: '4rem' }}>Cargando...</div>
  }

  return (
    <>
      <Navbar />
      <main className="layout">
        <Composer onPostCreated={() => setRefreshSignal(s => s + 1)} />
        <Feed refreshSignal={refreshSignal} />
      </main>
    </>
  )
}
