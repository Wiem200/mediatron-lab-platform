import { useState } from 'react';
import { Mail } from 'lucide-react';
import { useLanguage } from '../context/LanguageContext';
import { subscriberService } from '../api/subscriberService';

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function SubscribeSection() {
  const { language } = useLanguage();
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [feedback, setFeedback] = useState(null);

  const copy = {
    en: {
      eyebrow: 'Newsletter',
      title: 'Stay updated with MEDIATRON research.',
      description:
        'Subscribe to receive laboratory news, public events, and research activity updates directly in your inbox.',
      placeholder: 'Enter your professional email',
      button: 'Subscribe',
      loading: 'Subscribing...',
      invalid: 'Invalid email format.',
      duplicate: 'This email is already registered.',
        success: 'Subscription successful! Check your email for a welcome message.',
      server: 'Server error, please try again later.',
    },
    fr: {
      eyebrow: 'Newsletter',
      title: 'Restez informe des recherches de MEDIATRON.',
      description:
        'Abonnez-vous pour recevoir les actualites du laboratoire, les evenements publics et les mises a jour de recherche directement par email.',
      placeholder: 'Entrez votre adresse email',
      button: "S'abonner",
      loading: 'Abonnement...',
      invalid: 'Invalid email format.',
      duplicate: 'This email is already registered.',
        success: 'Subscription successful! Check your email for a welcome message.',
      server: 'Server error, please try again later.',
    },
  }[language];

  const handleSubmit = async (event) => {
    event.preventDefault();
    const normalizedEmail = email.trim().toLowerCase();

    if (!emailRegex.test(normalizedEmail)) {
      setFeedback({ type: 'error', message: copy.invalid });
      return;
    }

    setLoading(true);
    setFeedback(null);

    try {
      const response = await subscriberService.subscribe({ email: normalizedEmail });
      setFeedback({
        type: 'success',
        message: response.data?.message || copy.success,
      });
      setEmail('');
    } catch (err) {
      const status = err?.response?.status;
      const message =
        status === 409
          ? copy.duplicate
          : status === 400
            ? copy.invalid
            : copy.server;
      setFeedback({ type: 'error', message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="mx-auto max-w-7xl px-6 py-6 lg:px-10" aria-labelledby="subscribe-section-title">
      <div className="relative overflow-hidden rounded-[2.4rem] border border-[#e5dbc9] bg-[linear-gradient(135deg,_#ffffff_0%,_#f6f1e7_100%)] px-8 py-10 shadow-[0_18px_45px_rgba(27,45,82,0.08)] lg:px-10 lg:py-12">
        <div className="absolute right-0 top-0 h-40 w-40 rounded-full bg-[#c8a864]/10 blur-3xl" />
        <div className="relative grid gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.34em] text-[#b08a37]">{copy.eyebrow}</p>
            <h2 id="subscribe-section-title" className="mt-4 max-w-3xl font-serif text-3xl leading-tight text-[#1b2d52] lg:text-4xl">
              {copy.title}
            </h2>
            <p className="mt-4 max-w-2xl text-sm leading-8 text-[#61718c] sm:text-base">
              {copy.description}
            </p>
          </div>

          <div className="rounded-[2rem] border border-[#ece2d3] bg-white p-5 shadow-sm">
            <form onSubmit={handleSubmit} className="space-y-4">
              <label className="block">
                <span className="mb-3 block text-[11px] font-black uppercase tracking-[0.28em] text-[#9aa6bf]">
                  Email
                </span>
                <div className="flex items-center gap-3 rounded-[1.5rem] border border-[#e6ebf2] bg-[#fbfcfe] px-4 py-3 focus-within:border-[#c8a864]">
                  <Mail className="h-5 w-5 text-[#8da0bd]" />
                  <input
                    type="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    placeholder={copy.placeholder}
                    className="w-full bg-transparent text-sm text-[#1b2d52] outline-none placeholder:text-[#9aa6bf]"
                    disabled={loading}
                  />
                </div>
              </label>

              <button
                type="submit"
                disabled={loading}
                className="inline-flex items-center justify-center rounded-2xl bg-[#1b2d52] px-6 py-3 text-sm font-black uppercase tracking-[0.18em] text-white transition hover:bg-[#2d4677] disabled:cursor-not-allowed disabled:opacity-60"
              >
                {loading ? copy.loading : copy.button}
              </button>
            </form>

            {feedback && (
              <div
                className={`mt-4 rounded-[1.4rem] border px-4 py-4 text-sm leading-6 ${
                  feedback.type === 'success'
                    ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                    : 'border-rose-200 bg-rose-50 text-rose-700'
                }`}
              >
                {feedback.message}
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
