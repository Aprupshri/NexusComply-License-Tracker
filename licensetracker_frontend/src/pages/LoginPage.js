import { useState, useContext } from 'react';
import api from '../api/auth.js';
import { AuthContext } from '../context/AuthContext.js';
import { useNavigate } from 'react-router-dom';


export default function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const { login } = useContext(AuthContext);
    const navigate = useNavigate();


    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const res = await api.login(email, password);
            login(res);
            navigate('/dashboard');
        } catch (err) {
            setError('Invalid email or password');
        }
    };


    return (
        <div className="container mt-5" style={{ maxWidth: 400 }}>
            <h3 className="text-center mb-3">License Tracker Login</h3>
            <form onSubmit={handleSubmit}>
                <div className="mb-3">
                    <label>Email</label>
                    <input className="form-control" value={email} onChange={e => setEmail(e.target.value)} />
                </div>
                <div className="mb-3">
                    <label>Password</label>
                    <input type="password" className="form-control" value={password} onChange={e => setPassword(e.target.value)} />
                </div>
                {error && <div className="alert alert-danger">{error}</div>}
                <button className="btn btn-primary w-100">Login</button>
            </form>
        </div>
    );
}