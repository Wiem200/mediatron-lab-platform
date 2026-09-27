import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { authService } from "../api/authService";

const css = `
  @import url('https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=DM+Sans:opsz,wght@9..40,300;9..40,400;9..40,500;9..40,600&display=swap');

  .reset-shell {
    min-height: 100vh;
    padding: 32px 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    background:
      radial-gradient(circle at top right, rgba(27,45,82,0.12), transparent 30%),
      linear-gradient(145deg, #f5f2ec 0%, #f0e7d9 100%);
    font-family: 'DM Sans', sans-serif;
  }

  .reset-card {
    width: 100%;
    max-width: 460px;
    background: rgba(255,255,255,0.94);
    border: 1px solid rgba(27,45,82,0.08);
    border-radius: 24px;
    box-shadow: 0 24px 80px rgba(27,45,82,0.12);
    padding: 34px 30px;
    backdrop-filter: blur(10px);
  }

  .reset-eyebrow {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 6px 12px;
    border-radius: 999px;
    background: #edf0f6;
    color: #1b2d52;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 1.3px;
    text-transform: uppercase;
    margin-bottom: 16px;
  }

  .reset-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #c8a864;
  }

  .reset-title {
    margin: 0 0 10px;
    color: #1b2d52;
    font-family: 'DM Serif Display', serif;
    font-size: 34px;
    line-height: 1.1;
  }

  .reset-copy {
    margin: 0 0 28px;
    color: #667085;
    line-height: 1.7;
    font-size: 14px;
  }

  .reset-field {
    margin-bottom: 18px;
  }

  .reset-label {
    display: block;
    margin-bottom: 8px;
    color: #6b7485;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 1.4px;
    text-transform: uppercase;
  }

  .reset-input {
    width: 100%;
    border: 1.5px solid #e8e3d8;
    border-radius: 12px;
    padding: 14px 15px;
    color: #1b2d52;
    background: #fff;
    font-size: 14px;
    outline: none;
    transition: border-color 0.2s, box-shadow 0.2s;
  }

  .reset-input:focus {
    border-color: #1b2d52;
    box-shadow: 0 0 0 3px rgba(27,45,82,0.08);
  }

  .reset-alert {
    margin-bottom: 16px;
    border-radius: 12px;
    padding: 12px 14px;
    font-size: 13px;
  }

  .reset-alert.error {
    background: #fef3f2;
    border: 1px solid #fecaca;
    color: #b42318;
  }

  .reset-alert.success {
    background: #f0fdf4;
    border: 1px solid #bbf7d0;
    color: #166534;
  }

  .reset-submit {
    width: 100%;
    border: none;
    border-radius: 12px;
    padding: 15px;
    background: #1b2d52;
    color: #fff;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
    transition: background 0.2s, transform 0.1s;
  }

  .reset-submit:hover {
    background: #243868;
  }

  .reset-submit:active {
    transform: scale(0.99);
  }

  .reset-submit:disabled {
    opacity: 0.7;
    cursor: wait;
  }

  .reset-link-row {
    margin-top: 18px;
    text-align: center;
    font-size: 13px;
    color: #667085;
  }

  .reset-link {
    color: #1b2d52;
    font-weight: 700;
    text-decoration: underline;
    text-underline-offset: 3px;
  }
`;

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get("token") || "";
  const [newPassword, setNewPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (!token) {
      setError("This reset link is invalid. Please request a new one.");
      return;
    }

    if (newPassword.length < 6) {
      setError("Password must contain at least 6 characters.");
      return;
    }

    setIsLoading(true);

    try {
      const response = await authService.resetPassword(token, newPassword);
      setSuccess(response.message || "Password updated successfully.");
      setTimeout(() => navigate("/login"), 1800);
    } catch (err) {
      setError(err.response?.data?.message || "Unable to reset your password right now.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      <style>{css}</style>
      <div className="reset-shell">
        <div className="reset-card">
          <div className="reset-eyebrow">
            <span className="reset-dot" />
            Secure Reset
          </div>
          <h1 className="reset-title">Choose a new password</h1>
          <p className="reset-copy">
            This reset link is single-use and expires after one hour. Set a new password to regain access to MEDIATRON.
          </p>

          <form onSubmit={handleSubmit}>
            <div className="reset-field">
              <label className="reset-label">New Password</label>
              <input
                className="reset-input"
                type="password"
                placeholder="At least 6 characters"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                required
              />
            </div>

            {error && <div className="reset-alert error">{error}</div>}
            {success && <div className="reset-alert success">{success}</div>}

            <button type="submit" className="reset-submit" disabled={isLoading}>
              {isLoading ? "Updating password..." : "Update password"}
            </button>
          </form>

          <div className="reset-link-row">
            <Link to="/login" className="reset-link">Back to sign in</Link>
          </div>
        </div>
      </div>
    </>
  );
}
