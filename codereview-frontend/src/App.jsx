import { Routes, Route, Link } from 'react-router-dom'
import Login from './pages/Login'
import Repositories from './pages/Repositories.jsx'
import ReviewList from './pages/ReviewList'
import ReviewDetail from './pages/ReviewDetail'

function App() {
  return (
    <div>
      <nav>
        <Link to="/">Login</Link> | <Link to="/repositories">Repositories</Link>
      </nav>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/repositories" element={<Repositories />} />
        <Route path="/reviews" element={<ReviewList />} />
        <Route path="/reviews/:id" element={<ReviewDetail />} />
      </Routes>
    </div>
  )
}

export default App