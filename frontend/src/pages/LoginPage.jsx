import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { authService } from "../api/authService";

const css = `
  @import url('https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=DM+Sans:opsz,wght@9..40,300;9..40,400;9..40,500;9..40,600&display=swap');

  * { box-sizing: border-box; margin: 0; padding: 0; }

  .ml-bg {
    min-height: 100vh;
    display: flex;
    background: #f5f2ec;
    font-family: 'DM Sans', sans-serif;
  }

  .ml-left {
    width: 42%;
    background: #1b2d52;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    padding: 52px 48px;
    position: relative;
    overflow: hidden;
  }

  .ml-left-circle1 {
    position: absolute;
    top: -100px; right: -100px;
    width: 320px; height: 320px;
    border-radius: 50%;
    background: rgba(200,168,100,0.07);
    pointer-events: none;
  }

  .ml-left-circle2 {
    position: absolute;
    bottom: -80px; left: -80px;
    width: 260px; height: 260px;
    border-radius: 50%;
    background: rgba(200,168,100,0.05);
    pointer-events: none;
  }

  .ml-logo-row {
    display: flex; align-items: center; gap: 13px; z-index: 1; position: relative;
  }

  .ml-logo-box {
    width: 46px; height: 46px;
    background: linear-gradient(135deg, #c8a864, #e8cd90);
    border-radius: 11px;
    display: flex; align-items: center; justify-content: center;
    flex-shrink: 0;
  }

  .ml-logo-text {
    font-family: 'DM Serif Display', serif;
    font-size: 17px;
    color: #fff;
    letter-spacing: 2px;
    line-height: 1.2;
  }

  .ml-logo-sub {
    font-size: 10px;
    color: rgba(255,255,255,0.4);
    letter-spacing: 1px;
    font-weight: 400;
  }

  .ml-hero { z-index: 1; position: relative; }

  .ml-hero-eyebrow {
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 3px;
    color: #c8a864;
    text-transform: uppercase;
    margin-bottom: 18px;
    display: flex; align-items: center; gap: 10px;
  }

  .ml-hero-eyebrow::before {
    content: '';
    width: 28px; height: 1.5px;
    background: #c8a864;
    display: inline-block;
  }

  .ml-hero-title {
    font-family: 'DM Serif Display', serif;
    font-size: 44px;
    color: #fff;
    line-height: 1.18;
    margin-bottom: 22px;
  }

  .ml-hero-title em { color: #c8a864; font-style: italic; }

  .ml-hero-desc {
    font-size: 14px;
    color: rgba(255,255,255,0.5);
    line-height: 1.85;
    max-width: 290px;
    font-weight: 300;
  }

  .ml-stats {
    display: flex; gap: 32px; z-index: 1; position: relative;
  }

  .ml-stat { border-top: 1px solid rgba(200,168,100,0.25); padding-top: 16px; }

  .ml-stat-n {
    font-family: 'DM Serif Display', serif;
    font-size: 28px; color: #c8a864;
  }

  .ml-stat-l {
    font-size: 11px; color: rgba(255,255,255,0.35);
    margin-top: 3px; letter-spacing: 0.5px;
  }

  .ml-right {
    flex: 1;
    display: flex; align-items: center; justify-content: center;
    padding: 48px 40px;
  }

  .ml-form-wrap { width: 100%; max-width: 390px; }

  .ml-pill {
    display: inline-flex; align-items: center; gap: 7px;
    background: #ede9df;
    border-radius: 999px;
    padding: 5px 15px;
    font-size: 11px; font-weight: 600;
    color: #8a6f3c; letter-spacing: 1.2px;
    text-transform: uppercase;
    margin-bottom: 18px;
  }

  .ml-pill-dot {
    width: 6px; height: 6px;
    background: #c8a864; border-radius: 50%;
  }

  .ml-form-title {
    font-family: 'DM Serif Display', serif;
    font-size: 32px; color: #1b2d52;
    margin-bottom: 7px; line-height: 1.2;
  }

  .ml-form-sub {
    font-size: 14px; color: #9199aa;
    margin-bottom: 32px; line-height: 1.6; font-weight: 300;
  }

  .ml-field { margin-bottom: 20px; }

  .ml-label {
    display: block;
    font-size: 11px; font-weight: 600;
    letter-spacing: 1.5px; color: #6b7485;
    text-transform: uppercase; margin-bottom: 8px;
  }

  .ml-pwd-label-row {
    display: flex; justify-content: space-between; align-items: center;
    margin-bottom: 8px;
  }

  .ml-reset {
    background: none; border: none;
    font-family: 'DM Sans', sans-serif;
    font-size: 12px; color: #c8a864;
    cursor: pointer; font-weight: 500;
    padding: 0;
  }
  .ml-reset:hover { text-decoration: underline; }

  .ml-input-wrap { position: relative; display: flex; align-items: center; }

  .ml-iicon {
    position: absolute; left: 15px;
    color: #b0b8c8; display: flex; align-items: center;
    pointer-events: none;
  }

  .ml-input {
    width: 100%;
    background: #fff;
    border: 1.5px solid #e8e3d8;
    border-radius: 11px;
    font-family: 'DM Sans', sans-serif;
    font-size: 14px; color: #1b2d52;
    padding: 13px 14px 13px 44px;
    outline: none;
    transition: border-color 0.2s, box-shadow 0.2s;
  }
  .ml-input::placeholder { color: #c0c8d5; }
  .ml-input:focus {
    border-color: #1b2d52;
    box-shadow: 0 0 0 3px rgba(27,45,82,0.08);
  }

  .ml-eye {
    position: absolute; right: 14px;
    background: none; border: none;
    cursor: pointer; color: #8a6f3c;
    font-size: 10px; font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 1px;
    display: flex; align-items: center; padding: 0;
  }

  .ml-alert {
    border-radius: 9px;
    font-size: 13px;
    padding: 11px 14px;
    margin-bottom: 16px;
    font-weight: 400;
  }
  .ml-alert-err { background: #fef3f2; border: 1px solid #fcc; color: #c0392b; }
  .ml-alert-ok  { background: #f0fdf6; border: 1px solid #bbf7d0; color: #166534; }

  .ml-submit {
    width: 100%;
    background: #1b2d52;
    border: none; border-radius: 11px;
    font-family: 'DM Sans', sans-serif;
    font-size: 15px; font-weight: 600;
    color: #fff;
    padding: 15px;
    cursor: pointer;
    display: flex; align-items: center; justify-content: center; gap: 10px;
    margin-top: 6px;
    transition: background 0.2s, transform 0.1s;
    letter-spacing: 0.2px;
  }
  .ml-submit:hover { background: #243868; }
  .ml-submit:active { transform: scale(0.99); }

  .ml-arrow-box {
    width: 26px; height: 26px;
    background: rgba(255,255,255,0.13);
    border-radius: 6px;
    display: flex; align-items: center; justify-content: center;
  }

  .ml-divider {
    display: flex; align-items: center; gap: 12px;
    margin: 22px 0;
  }
  .ml-divider-line { flex: 1; height: 1px; background: #e8e3d8; }
  .ml-divider span { font-size: 12px; color: #b0b8c8; }

  .ml-toggle-row { text-align: center; font-size: 13px; color: #9199aa; }

  .ml-toggle-btn {
    background: none; border: none;
    font-family: 'DM Sans', sans-serif;
    font-size: 13px; font-weight: 600;
    color: #1b2d52; cursor: pointer;
    text-decoration: underline;
    text-underline-offset: 3px;
    padding: 0;
  }

  .ml-footer {
    font-size: 11px; color: #b8bec8;
    text-align: center;
    margin-top: 30px;
    line-height: 1.8;
    border-top: 1px solid #e8e3d8;
    padding-top: 22px;
  }

  @media (max-width: 768px) {
    .ml-left { display: none; }
    .ml-right { padding: 32px 24px; background: #f5f2ec; }
  }
`;

export default function LoginPage() {
  const institutionalEmailRegex = /^[A-Za-z0-9._%+-]+@supcom\.tn$/;
  const [isRegister, setIsRegister] = useState(false);
  const [showPwd, setShowPwd] = useState(false);
  const [email, setEmail]     = useState("");
  const [password, setPassword] = useState("");
  const [name, setName]       = useState("");
  const [error, setError]     = useState("");
  const [success, setSuccess] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const navigate = useNavigate();

  useEffect(() => {
    const tag = document.createElement("style");
    tag.textContent = css;
    document.head.appendChild(tag);
    return () => document.head.removeChild(tag);
  }, []);

  const resetMessages = () => { setError(""); setSuccess(""); };

  const handleSubmit = async (e) => {
    e.preventDefault();
    resetMessages();

  const normalizedEmail = email.trim().toLowerCase();

  if (isRegister && !normalizedEmail.endsWith('@supcom.tn') && !normalizedEmail.endsWith('@gmail.com')){
    setError("Invalid domain. Please use @supcom.tn or @gmail.com for testing.");
    return;
  }

    setIsLoading(true);

    try {
      if (isRegister) {
        const res = await authService.register(email, password);
        if (res.success) {
          setSuccess(res.message || "Account created successfully. You can now sign in.");
          setIsRegister(false);
          setPassword("");
        } else {
          setError(res.message);
        }
      } else {
        const res = await authService.login(email, password);
        if (res.success) {
          setSuccess("Authentication successful. Redirecting...");
          setTimeout(() => navigate("/dashboard"), 1500);
        } else {
          setError(res.message || "Invalid email or password.");
        }
      }
    } catch (err) {
      setError(err.response?.data?.message || "Server unreachable. Please try again later.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="ml-bg">
      {/* Left panel */}
      <div className="ml-left">
        <div className="ml-left-circle1" />
        <div className="ml-left-circle2" />

        <div className="ml-logo-row">
          <div className="ml-logo-box">
            <svg width="22" height="22" fill="none" stroke="#1b2d52" strokeWidth="2" viewBox="0 0 24 24">
              <path d="M12 14l9-5-9-5-9 5 9 5z"/>
              <path d="M12 14l6.16-3.422a12.083 12.083 0 0112 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z"/>
            </svg>
          </div>
          <div>
            <div className="ml-logo-text">MEDIATRON</div>
            <div className="ml-logo-sub">SUP'COM - TUNISIA</div>
          </div>
        </div>

        <div className="ml-hero">
          <div className="ml-hero-eyebrow">Research Platform</div>
          <h2 className="ml-hero-title">
            Knowledge<br/>begins with<br/><em>access</em>
          </h2>
          <p className="ml-hero-desc">
            A centralized portal for researchers, PhD students, and collaborators of the MEDIATRON laboratory in Networks and Telecommunications.
          </p>
        </div>

        <div className="ml-stats">
          <div className="ml-stat">
            <div className="ml-stat-n">120+</div>
            <div className="ml-stat-l">Publications</div>
          </div>
          <div className="ml-stat">
            <div className="ml-stat-n">18</div>
            <div className="ml-stat-l">Researchers</div>
          </div>
          <div className="ml-stat">
            <div className="ml-stat-n">12</div>
            <div className="ml-stat-l">Projects</div>
          </div>
        </div>
      </div>

      {/* Right panel */}
      <div className="ml-right">
        <div className="ml-form-wrap">
          <div className="ml-pill">
            <span className="ml-pill-dot" />
            {isRegister ? "Registration" : "Secure Access"}
          </div>

          <h1 className="ml-form-title">
            {isRegister ? "Create account" : "Welcome back"}
          </h1>
          <p className="ml-form-sub">
            {isRegister
              ? "Join the MEDIATRON research platform"
              : "Sign in to access research data and publications"}
          </p>

          <form onSubmit={handleSubmit}>
            {isRegister && (
              <div className="ml-field">
                <label className="ml-label">Full Name</label>
                <div className="ml-input-wrap">
                  <span className="ml-iicon">
                    <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                      <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                      <circle cx="12" cy="7" r="4"/>
                    </svg>
                  </span>
                  <input className="ml-input" type="text" placeholder="Dr. Your Name"
                    value={name} onChange={e => setName(e.target.value)} />
                </div>
              </div>
            )}

            <div className="ml-field">
              <label className="ml-label">Professional Email</label>
              <div className="ml-input-wrap">
                <span className="ml-iicon">
                  <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                    <polyline points="22,6 12,13 2,6"/>
                  </svg>
                </span>
                <input className="ml-input" type="email" placeholder="name@supcom.tn"
                  value={email} onChange={e => setEmail(e.target.value)} required />
              </div>
            </div>

            {!isRegister && (
              <div className="ml-field">
                <div className="ml-pwd-label-row">
                  <label className="ml-label" style={{margin:0}}>Password</label>
                  <button type="button" className="ml-reset" onClick={() => navigate("/forgot-password")}>
                    Forgot password?
                  </button>
                </div>
                <div className="ml-input-wrap">
                  <span className="ml-iicon">
                    <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                      <path d="M7 11V7a5 5 0 0110 0v4"/>
                    </svg>
                  </span>
                  <input className="ml-input" type={showPwd ? "text" : "password"}
                    placeholder="password" value={password}
                    onChange={e => setPassword(e.target.value)}
                    style={{ paddingRight: "54px" }} required />
                  <button type="button" className="ml-eye" onClick={() => setShowPwd(!showPwd)}>
                    {showPwd ? "HIDE" : "SHOW"}
                  </button>
                </div>
              </div>
            )}

            {isRegister && (
               <div className="ml-field">
               <label className="ml-label" style={{margin:0}}>Password</label>
               <div className="ml-input-wrap">
                 <span className="ml-iicon">
                   <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                     <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                     <path d="M7 11V7a5 5 0 0110 0v4"/>
                   </svg>
                 </span>
                 <input className="ml-input" type="password"
                   placeholder="password" value={password}
                   onChange={e => setPassword(e.target.value)} required />
               </div>
             </div>
            )}

            {error   && <div className="ml-alert ml-alert-err">{error}</div>}
            {success && <div className="ml-alert ml-alert-ok">{success}</div>}

            <button type="submit" className="ml-submit" disabled={isLoading}>
              {isLoading ? "Processing..." : (isRegister ? "Create Account" : "Sign In")}
              <span className="ml-arrow-box">
                <svg width="13" height="13" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                  <path d="M5 12h14M12 5l7 7-7 7"/>
                </svg>
              </span>
            </button>
          </form>

          <div className="ml-divider">
            <div className="ml-divider-line"/>
            <span>OR</span>
            <div className="ml-divider-line"/>
          </div>

          <div className="ml-toggle-row">
            {isRegister ? "Already have an account? " : "Don't have an account? "}
            <button type="button" className="ml-toggle-btn"
              onClick={() => { setIsRegister(!isRegister); resetMessages(); }}>
              {isRegister ? "Sign In" : "Create one"}
            </button>
          </div>

          <div className="ml-footer">
            Laboratoire de Recherche en Reseaux<br/>
            et Systemes de Telecommunications - SUP'COM
          </div>
        </div>
      </div>
    </div>
  );
}
